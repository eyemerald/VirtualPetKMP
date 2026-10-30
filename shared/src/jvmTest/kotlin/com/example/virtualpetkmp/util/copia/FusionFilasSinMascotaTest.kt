package com.example.virtualpetkmp.util.copia

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.abrirBaseDeDatos
import com.example.virtualpetkmp.data.crearDriverJdbc
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.escribirZip
import com.example.virtualpetkmp.util.leerTextoDeZip
import com.example.virtualpetkmp.util.sha256
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Filas hijas que no se pueden colgar de ninguna mascota al fusionar.
 *
 * Un paquete manipulado (o una mascota omitida a mano al resolver un conflicto) puede traer una
 * vacuna o una nota cuyo `mascotaUuid` no existe ni en el paquete ni en el dispositivo. Antes esas
 * filas **se descartaban en silencio**; ahora se cuentan en `ResultadoFusion.filasSinMascota` para
 * poder decírselo al usuario.
 *
 * El paquete se fabrica a mano aquí (ZIP con `datos.json` y `manifiesto.json`) porque es la única
 * forma de crear ese caso: el exportador de la app nunca escribe una fila huérfana.
 */
class FusionFilasSinMascotaTest {

    private lateinit var raiz: File
    private lateinit var carpetas: CarpetasApp
    private val drivers = mutableListOf<SqlDriver>()

    private val uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"

    @BeforeTest
    fun setup() {
        raiz = Files.createTempDirectory("virtualpet-fusion-huerfanas").toFile()
        carpetas = CarpetasApp(
            fotos = File(raiz, "fotos").apply { mkdirs() }.absolutePath,
            informes = File(raiz, "informes").apply { mkdirs() }.absolutePath,
            copias = File(raiz, "copias").apply { mkdirs() }.absolutePath
        )
    }

    @AfterTest
    fun tearDown() {
        drivers.forEach { runCatching { it.close() } }
        raiz.deleteRecursively()
    }

    private fun baseEnFichero(nombre: String): VirtualPetDatabase {
        val archivo = File(raiz, nombre)
        val driver = crearDriverJdbc(archivo)
        drivers.add(driver)
        return abrirBaseDeDatos(driver, archivo = archivo)
    }

    private fun SqlDriver.numero(sql: String): Long =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: -1L)
            },
            parameters = 0
        ).value

    /** Base con una mascota y una nota válida. */
    private suspend fun sembrar(db: VirtualPetDatabase) {
        val mascotaId = MascotaRepository(db).insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Mestizo",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = null
            ),
            uuid = uuidMascota,
            actualizadoEn = 1_000L
        ).getOrThrow()

        NotaRepository(db).insertNota(
            mascotaId = mascotaId,
            texto = "Nota válida",
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
            actualizadoEn = 1_000L
        ).getOrThrow()
    }

    private suspend fun exportar(db: VirtualPetDatabase, nombre: String): String {
        val destino = File(raiz, nombre).absolutePath
        ExportadorCopia(db, carpetas, versionApp = "prueba", plataforma = "test")
            .exportar(destino).getOrThrow()
        return destino
    }

    /**
     * Reescribe el paquete añadiendo una nota cuya mascota no existe, y **recalcula el hash** del
     * manifiesto (el lector comprueba el SHA-256 de `datos.json`).
     */
    private fun anadirNotaHuerfana(paqueteOriginal: String, paqueteNuevo: String) {
        val datosTexto = leerTextoDeZip(paqueteOriginal, PaqueteDatos.ENTRADA_DATOS)!!
        val datos = PaqueteDatos.json.decodeFromString(PaqueteDatos.serializer(), datosTexto)
        val modificado = datos.copy(
            notas = datos.notas + FilaNota(
                uuid = "ccccccccccccccccccccccccccccccc1",
                actualizadoEn = 2_000L,
                mascotaUuid = "fffffffffffffffffffffffffffffff9",
                texto = "Nota de una mascota que no existe"
            )
        )

        val nuevoDatos = PaqueteDatos.json.encodeToString(PaqueteDatos.serializer(), modificado)
        val manifiestoTexto = leerTextoDeZip(paqueteOriginal, PaqueteDatos.ENTRADA_MANIFIESTO)!!
        val manifiesto = PaqueteDatos.json.decodeFromString(ManifiestoPaquete.serializer(), manifiestoTexto)
        val nuevoManifiesto = PaqueteDatos.json.encodeToString(
            ManifiestoPaquete.serializer(),
            manifiesto.copy(hashDatos = sha256(nuevoDatos.toByteArray(Charsets.UTF_8)))
        )

        escribirZip(
            paqueteNuevo,
            mapOf(
                PaqueteDatos.ENTRADA_DATOS to nuevoDatos,
                PaqueteDatos.ENTRADA_MANIFIESTO to nuevoManifiesto
            ),
            emptyMap()
        )
    }

    @Test
    fun `una fila sin mascota se descarta y se cuenta`(): Unit = runBlocking {
        val origen = baseEnFichero("origen.db")
        sembrar(origen)

        val paquete = exportar(origen, "paquete.vpet")
        val paqueteRoto = File(raiz, "paquete-con-huerfana.vpet").absolutePath
        anadirNotaHuerfana(paquete, paqueteRoto)

        val destino = baseEnFichero("destino.db")
        val driverDestino = drivers.last()
        val resultado = ImportadorFusion(
            destino, carpetas, ExportadorCopia(destino, carpetas, "prueba", "test")
        ).aplicar(paqueteRoto).getOrThrow()

        assertEquals(
            1,
            resultado.filasSinMascota,
            "la nota huérfana tiene que aparecer en filasSinMascota, no desaparecer en silencio"
        )
        assertEquals(2, resultado.filasNuevas, "deberían entrar la mascota y su nota válida")
        assertEquals(1, driverDestino.numero("SELECT count(*) FROM mascotas;"))
        assertEquals(
            1,
            driverDestino.numero("SELECT count(*) FROM notas;"),
            "la nota huérfana no se puede insertar (quedaría colgando de nada)"
        )
        assertEquals(
            0,
            driverDestino.numero("SELECT count(*) FROM pragma_foreign_key_check;"),
            "quedaron filas huérfanas en la base"
        )
    }

    @Test
    fun `un paquete normal no cuenta filas sin mascota`(): Unit = runBlocking {
        val origen = baseEnFichero("origen-limpio.db")
        sembrar(origen)
        val paquete = exportar(origen, "paquete-limpio.vpet")

        val destino = baseEnFichero("destino-limpio.db")
        val resultado = ImportadorFusion(
            destino, carpetas, ExportadorCopia(destino, carpetas, "prueba", "test")
        ).aplicar(paquete).getOrThrow()

        assertEquals(0, resultado.filasSinMascota, "no había ninguna fila huérfana")
        assertEquals(2, resultado.filasNuevas)
    }
}
