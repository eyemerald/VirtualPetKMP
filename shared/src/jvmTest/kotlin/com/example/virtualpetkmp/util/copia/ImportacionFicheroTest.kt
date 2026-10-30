package com.example.virtualpetkmp.util.copia

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.data.abrirBaseDeDatos
import com.example.virtualpetkmp.data.crearDriverJdbc
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.escribirBytes
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Restaurar y fusionar **con la base en un fichero**, no en memoria.
 *
 * Por qué existe: los tests de traspaso usaban `IN_MEMORY`, donde sqldelight reutiliza una sola
 * conexión. Sobre fichero cada sentencia va por una conexión distinta (ver `CascadaEscritorioTest`),
 * y eso destapó dos fallos que la memoria escondía: el `id` de un alta volvía 0
 * (`IdInsertadoTest`) y el `ON DELETE CASCADE` no se aplicaba. Este test comprueba lo que de verdad
 * importa al usuario: que **cada fila importada queda colgando de su mascota** y que no hay
 * huérfanas.
 */
class ImportacionFicheroTest {

    private lateinit var raiz: File
    private lateinit var carpetas: CarpetasApp
    private val drivers = mutableListOf<SqlDriver>()

    private val uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"

    @BeforeTest
    fun setup() {
        raiz = Files.createTempDirectory("virtualpet-importacion-fichero").toFile()
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

    /** Abre una base **en fichero**, como la de la app. */
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

    /** Una mascota con una fila en cada una de las 7 tablas que cuelgan de ella. */
    private suspend fun sembrarMascotaCompleta(db: VirtualPetDatabase): Long {
        val mascotaId = MascotaRepository(db).insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Labrador Retriever",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = null
            ),
            uuid = uuidMascota,
            actualizadoEn = 1_000L
        ).getOrThrow()

        VacunaRepository(db).insertVacuna(
            mascotaId = mascotaId,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2026, 1, 10),
            fechaProximaDosis = LocalDate(2027, 1, 10),
            veterinario = null,
            lote = null,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
            actualizadoEn = 1_000L
        ).getOrThrow()

        PreventivoRepository(db).insertPreventivo(
            mascotaId = mascotaId,
            tipo = "Pipeta",
            nombre = "Frontline",
            fechaAplicacion = LocalDate(2026, 2, 1),
            fechaProximaDosis = LocalDate(2026, 3, 1),
            veterinario = null,
            lote = null,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2",
            actualizadoEn = 1_000L
        ).getOrThrow()

        RevisionRepository(db).insertRevision(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 3, 1),
            motivo = "Revisión anual",
            diagnostico = null,
            notas = null,
            veterinario = null,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb3",
            actualizadoEn = 1_000L
        ).getOrThrow()

        TratamientoRepository(db).insertTratamiento(
            mascotaId = mascotaId,
            nombreMedicamento = "Antibiótico",
            dosis = null,
            frecuencia = null,
            fechaInicio = LocalDate(2026, 4, 1),
            fechaFin = null,
            hora = null,
            notificar = false,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb4",
            actualizadoEn = 1_000L
        )

        PesoRepository(db).insertPeso(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 5, 1),
            peso = 4.7,
            notas = null,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb5",
            actualizadoEn = 1_000L
        ).getOrThrow()

        val rutaPdf = "${carpetas.informes}/informe.pdf"
        escribirBytes(rutaPdf, "%PDF-1.4 prueba".toByteArray())
        InformeRepository(db).insertInforme(
            mascotaId = mascotaId,
            tipo = "Analítica",
            descripcion = null,
            fecha = LocalDate(2026, 6, 1),
            nombreArchivo = "informe.pdf",
            rutaArchivo = rutaPdf,
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb6",
            actualizadoEn = 1_000L
        ).getOrThrow()

        NotaRepository(db).insertNota(
            mascotaId = mascotaId,
            texto = "Alérgica a las pulgas",
            uuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb7",
            actualizadoEn = 1_000L
        ).getOrThrow()

        return mascotaId
    }

    private val tablasHijas = listOf(
        "vacunas", "preventivos", "revisiones", "tratamientos", "pesos", "informes", "notas"
    )

    /** Comprueba que la mascota importada tiene sus 7 filas y que no hay huérfanas. */
    private fun comprobarMascotaConSusHijos(db: VirtualPetDatabase, driver: SqlDriver) {
        val mascota = db.mascotasQueries.selectAll().executeAsList().single()
        assertEquals(uuidMascota, mascota.uuid, "la mascota importada no es la del paquete")
        val id = mascota.id!!

        tablasHijas.forEach { tabla ->
            assertEquals(
                1,
                driver.numero("SELECT count(*) FROM $tabla WHERE mascotaId = $id;"),
                "a la tabla $tabla no le llegó su fila enganchada a la mascota"
            )
            assertEquals(
                0,
                driver.numero("SELECT count(*) FROM $tabla WHERE mascotaId <> $id;"),
                "hay filas de $tabla colgando de otra mascota"
            )
        }

        assertEquals(
            0,
            driver.numero("SELECT count(*) FROM pragma_foreign_key_check;"),
            "quedaron filas huérfanas tras la importación"
        )
    }

    @Test
    fun `restaurar un paquete en una base de fichero engancha los hijos a su mascota`() = runBlocking {
        val origen = baseEnFichero("origen.db")
        sembrarMascotaCompleta(origen)

        val paquete = File(raiz, "paquete.vpet").absolutePath
        ExportadorCopia(origen, carpetas, versionApp = "prueba", plataforma = "test")
            .exportar(paquete).getOrThrow()

        val destino = baseEnFichero("destino-restaurar.db")
        val driverDestino = drivers.last()
        ImportadorCopia(destino, carpetas, ExportadorCopia(destino, carpetas, "prueba", "test"))
            .restaurar(paquete).getOrThrow()

        comprobarMascotaConSusHijos(destino, driverDestino)
    }

    @Test
    fun `fusionar un paquete en una base de fichero engancha los hijos a su mascota`() = runBlocking {
        val origen = baseEnFichero("origen-fusion.db")
        sembrarMascotaCompleta(origen)

        val paquete = File(raiz, "paquete-fusion.vpet").absolutePath
        ExportadorCopia(origen, carpetas, versionApp = "prueba", plataforma = "test")
            .exportar(paquete).getOrThrow()

        val destino = baseEnFichero("destino-fusion.db")
        val driverDestino = drivers.last()
        val fusionador = ImportadorFusion(destino, carpetas, ExportadorCopia(destino, carpetas, "prueba", "test"))

        fusionador.analizar(paquete).getOrThrow()
        fusionador.aplicar(paquete).getOrThrow()

        comprobarMascotaConSusHijos(destino, driverDestino)
    }

    @Test
    fun `fusionar dos veces no duplica los hijos`() = runBlocking {
        val origen = baseEnFichero("origen-doble.db")
        sembrarMascotaCompleta(origen)

        val paquete = File(raiz, "paquete-doble.vpet").absolutePath
        ExportadorCopia(origen, carpetas, versionApp = "prueba", plataforma = "test")
            .exportar(paquete).getOrThrow()

        val destino = baseEnFichero("destino-doble.db")
        val driverDestino = drivers.last()
        val fusionador = ImportadorFusion(destino, carpetas, ExportadorCopia(destino, carpetas, "prueba", "test"))

        fusionador.aplicar(paquete).getOrThrow()
        val segunda = fusionador.aplicar(paquete).getOrThrow()

        assertEquals(0, segunda.filasNuevas, "la segunda fusión no debería añadir filas")
        assertEquals(0, segunda.filasActualizadas, "la segunda fusión no debería actualizar filas")
        comprobarMascotaConSusHijos(destino, driverDestino)
        assertTrue(
            driverDestino.numero("SELECT count(*) FROM mascotas;") == 1L,
            "la mascota se duplicó"
        )
    }
}
