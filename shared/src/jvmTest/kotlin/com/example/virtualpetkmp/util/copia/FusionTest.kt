package com.example.virtualpetkmp.util.copia

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.escribirBytes
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.leerBytes
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * La fusión entre dispositivos: el caso "llevo la misma mascota en el móvil y en el ordenador".
 *
 * Aquí está el riesgo de verdad del traspaso: fusionar mal **duplica** el historial (dos veces la
 * misma vacuna) o, peor, **pisa** datos nuevos con viejos. Cada prueba de este fichero es uno de los
 * escenarios acordados en `docs/TRASPASO_DE_DATOS.md`.
 */
class FusionTest {

    private lateinit var raiz: File
    private lateinit var carpetas: CarpetasApp
    private lateinit var driverA: SqlDriver
    private lateinit var dbA: VirtualPetDatabase
    private lateinit var driverB: SqlDriver
    private lateinit var dbB: VirtualPetDatabase

    @AfterTest
    fun tearDown() {
        if (::driverA.isInitialized) driverA.close()
        if (::driverB.isInitialized) driverB.close()
        if (::raiz.isInitialized) raiz.deleteRecursively()
    }

    private fun preparar() {
        raiz = Files.createTempDirectory("virtualpet-fusion").toFile()
        carpetas = CarpetasApp(
            fotos = File(raiz, "fotos").apply { mkdirs() }.absolutePath,
            informes = File(raiz, "informes").apply { mkdirs() }.absolutePath,
            copias = File(raiz, "copias").apply { mkdirs() }.absolutePath
        )
        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { VirtualPetDatabase.Schema.create(it) }
        dbA = VirtualPetDatabase(driverA)
        driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { VirtualPetDatabase.Schema.create(it) }
        dbB = VirtualPetDatabase(driverB)
    }

    private fun exportador(db: VirtualPetDatabase) =
        ExportadorCopia(db, carpetas, versionApp = "prueba", plataforma = "test")

    private fun fusionador(db: VirtualPetDatabase) =
        ImportadorFusion(db, carpetas, exportador(db))

    private val pdf = "%PDF-1.4 documento de prueba".toByteArray()

    /** Una mascota con un poco de historial. `uuidMascota` fijo para poder comparar entre bases. */
    private fun sembrar(
        db: VirtualPetDatabase,
        nombre: String,
        uuidMascota: String,
        color: String = "Negro",
        uuidPeso: String = "peso-${uuidMascota}",
        uuidInforme: String = "informe-${uuidMascota}",
        momento: Long = 1_000L
    ): Long = runBlocking {
        val mascotaId = MascotaRepository(db).insertMascota(
            Mascota(
                nombre = nombre,
                especie = "Perro",
                raza = "Mestizo",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = color,
                microchip = null
            ),
            uuid = uuidMascota,
            actualizadoEn = momento
        ).getOrThrow()

        PesoRepository(db).insertPeso(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 4),
            peso = 26.3,
            notas = null,
            uuid = uuidPeso,
            actualizadoEn = momento
        ).getOrThrow()

        val rutaPdf = "${carpetas.informes}/documento-$uuidInforme.pdf"
        escribirBytes(rutaPdf, pdf)
        InformeRepository(db).insertInforme(
            mascotaId = mascotaId,
            tipo = "Analítica",
            descripcion = "Resultados",
            fecha = LocalDate(2026, 2, 5),
            nombreArchivo = "analitica.pdf",
            rutaArchivo = rutaPdf,
            uuid = uuidInforme,
            actualizadoEn = momento
        ).getOrThrow()

        NotaRepository(db).insertNota(
            mascotaId = mascotaId,
            texto = "Nota de $nombre",
            uuid = "nota-$uuidMascota",
            actualizadoEn = momento
        ).getOrThrow()

        mascotaId
    }

    private fun exportar(db: VirtualPetDatabase, nombre: String = "paquete.vpet"): String =
        runBlocking {
            val destino = File(raiz, nombre).absolutePath
            exportador(db).exportar(destino).getOrThrow()
            destino
        }

    private fun filas(db: VirtualPetDatabase): Map<String, Int> = mapOf(
        "mascotas" to db.mascotasQueries.selectAll().executeAsList().size,
        "pesos" to db.pesosQueries.selectAll().executeAsList().size,
        "informes" to db.informesQueries.selectAll().executeAsList().size,
        "notas" to db.notasQueries.selectAll().executeAsList().size
    )

    // --- Escenario 1: paquete nuevo en una base vacía ----------------------------------------

    @Test
    fun `fusionar en una base vacia deja lo mismo que el origen`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        val paquete = exportar(dbA)

        val plan = fusionador(dbB).analizar(paquete).getOrThrow()
        assertEquals(1, plan.mascotasNuevas)
        assertEquals(0, plan.mascotasAModificar)
        assertTrue(plan.conflictos.isEmpty())

        val resultado = fusionador(dbB).aplicar(paquete).getOrThrow()
        assertEquals(1, resultado.mascotasNuevas)
        assertEquals(4, filas(dbB).values.sum(), "deberían entrar mascota, peso, informe y nota")

        val mascota = dbB.mascotasQueries.selectAll().executeAsList().single()
        assertEquals("Luna", mascota.nombre)
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", mascota.uuid)
        assertEquals(1_000L, mascota.actualizadoEn)
        assertTrue(
            dbB.informesQueries.selectAll().executeAsList().single().rutaArchivo.isNotBlank(),
            "el informe tiene que quedar apuntando a un archivo"
        )
    }

    // --- Escenario 2: reimportar no duplica ---------------------------------------------------

    @Test
    fun `reimportar el mismo paquete no duplica nada`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        val paquete = exportar(dbA)

        fusionador(dbB).aplicar(paquete).getOrThrow()
        val despuesDeUna = filas(dbB)

        val segunda = fusionador(dbB).aplicar(paquete).getOrThrow()

        assertEquals(despuesDeUna, filas(dbB), "la segunda importación duplicó filas")
        assertEquals(0, segunda.filasNuevas)
        assertEquals(0, segunda.filasActualizadas)
    }

    // --- Escenario 3: gana la más nueva -------------------------------------------------------

    @Test
    fun `una fila mas nueva en el paquete actualiza la local sin duplicarla`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", momento = 1_000L)
        fusionador(dbB).aplicar(exportar(dbA)).getOrThrow()
        assertEquals(1, dbB.pesosQueries.selectAll().executeAsList().size)

        // En A se corrige el peso (más nuevo) y se exporta otra vez.
        PesoRepository(dbA).updatePeso(
            id = dbA.pesosQueries.selectAll().executeAsList().single().id,
            fecha = LocalDate(2026, 2, 4),
            peso = 27.5,
            notas = "corregido",
            actualizadoEn = 5_000L
        ).getOrThrow()

        val resultado = fusionador(dbB).aplicar(exportar(dbA, "paquete2.vpet")).getOrThrow()

        assertEquals(0, resultado.filasNuevas)
        assertEquals(1, resultado.filasActualizadas)

        val peso = dbB.pesosQueries.selectAll().executeAsList().single()
        assertEquals(27.5, peso.peso)
        assertEquals("corregido", peso.notas)
        assertEquals(5_000L, peso.actualizadoEn)
    }

    @Test
    fun `un paquete mas viejo no revierte lo que hay`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", momento = 1_000L)
        fusionador(dbB).aplicar(exportar(dbA)).getOrThrow()

        // Ahora B (este dispositivo) tiene el peso corregido y más nuevo.
        PesoRepository(dbB).updatePeso(
            id = dbB.pesosQueries.selectAll().executeAsList().single().id,
            fecha = LocalDate(2026, 2, 4),
            peso = 28.0,
            notas = "de B",
            actualizadoEn = 9_000L
        ).getOrThrow()

        val resultado = fusionador(dbB).aplicar(exportar(dbA, "viejo.vpet")).getOrThrow()

        assertEquals(0, resultado.filasActualizadas, "no se puede pisar con algo más viejo")
        val peso = dbB.pesosQueries.selectAll().executeAsList().single()
        assertEquals(28.0, peso.peso)
        assertEquals("de B", peso.notas)
        assertEquals(9_000L, peso.actualizadoEn)
    }

    // --- Escenario 6: mismo nombre, identidad distinta ---------------------------------------

    @Test
    fun `un nombre repetido se avisa en el plan y se puede renombrar`() = runBlocking {
        preparar()
        // Dos "Luna" creadas por separado: mismo nombre, uuid distinto.
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        sembrar(dbB, "Luna", uuidMascota = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2")

        val paquete = exportar(dbA)
        val plan = fusionador(dbB).analizar(paquete).getOrThrow()

        assertEquals(1, plan.conflictos.size, "tendría que avisar del nombre repetido")
        assertEquals("Luna", plan.conflictos.single().nombrePaquete)
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2", plan.conflictos.single().uuidLocal)

        val resultado = fusionador(dbB).aplicar(
            paquete,
            mapOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1" to DecisionConflicto.RENOMBRAR)
        ).getOrThrow()

        assertEquals(1, resultado.mascotasNuevas)
        val nombres = dbB.mascotasQueries.selectAll().executeAsList().map { it.nombre }.sorted()
        assertEquals(listOf("Luna", "Luna (2)"), nombres)
    }

    @Test
    fun `se puede sobrescribir la mascota local con la del paquete`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", color = "Blanco")
        sembrar(dbB, "Luna", uuidMascota = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2", color = "Negro")

        val paquete = exportar(dbA)
        val resultado = fusionador(dbB).aplicar(
            paquete,
            mapOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1" to DecisionConflicto.SOBRESCRIBIR)
        ).getOrThrow()

        assertEquals(0, resultado.mascotasNuevas, "sobrescribir no crea otra mascota")
        assertEquals(1, dbB.mascotasQueries.selectAll().executeAsList().size)
        assertEquals("Blanco", dbB.mascotasQueries.selectAll().executeAsList().single().color)

        // El historial no se pisa: se unen los dos. El peso local sigue y se añade el del paquete,
        // porque son dos registros distintos (cada dispositivo apuntó uno).
        val pesos = dbB.pesosQueries.selectAll().executeAsList().map { it.uuid }.sorted()
        assertEquals(
            listOf("peso-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", "peso-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2"),
            pesos,
            "sobrescribir la mascota no puede borrar el historial local"
        )
        assertEquals(2, dbB.notasQueries.selectAll().executeAsList().size)
    }

    @Test
    fun `se puede omitir la mascota conflictiva`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        sembrar(dbB, "Luna", uuidMascota = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2")

        val resultado = fusionador(dbB).aplicar(
            exportar(dbA),
            mapOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1" to DecisionConflicto.OMITIR)
        ).getOrThrow()

        assertEquals(1, resultado.conflictosOmitidos)
        assertEquals(1, dbB.mascotasQueries.selectAll().executeAsList().size)
        assertEquals(1, dbB.pesosQueries.selectAll().executeAsList().size, "no debería traer su historial")
    }

    // --- Escenario 7: paquete roto -------------------------------------------------------------

    @Test
    fun `un paquete roto no toca la base de datos`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        val paquete = exportar(dbA)

        val bytes = leerBytes(paquete)!!
        escribirBytes(paquete, bytes.copyOf(bytes.size / 2))

        val antes = filas(dbB)
        val resultado = fusionador(dbB).aplicar(paquete)

        assertTrue(resultado.isFailure)
        assertEquals(antes, filas(dbB))
    }

    // --- Nunca borra, y recupera adjuntos que faltaban ---------------------------------------

    @Test
    fun `la fusion nunca borra lo que solo existe en este dispositivo`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        sembrar(dbB, "Nilo", uuidMascota = "ccccccccccccccccccccccccccccccc3")

        fusionador(dbB).aplicar(exportar(dbA)).getOrThrow()

        val nombres = dbB.mascotasQueries.selectAll().executeAsList().map { it.nombre }.sorted()
        assertEquals(listOf("Luna", "Nilo"), nombres, "importar no puede borrar lo local")
    }

    @Test
    fun `si el documento no estaba en el dispositivo se recupera del paquete`() = runBlocking {
        preparar()
        sembrar(dbA, "Luna", uuidMascota = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1")
        val paquete = exportar(dbA)
        fusionador(dbB).aplicar(paquete).getOrThrow()

        // El PDF se pierde en este dispositivo, pero la fila sigue.
        val informe = dbB.informesQueries.selectAll().executeAsList().single()
        File(informe.rutaArchivo).delete()
        assertTrue(!existeArchivo(informe.rutaArchivo))

        // Un paquete más nuevo (con el archivo dentro) lo recupera.
        InformeRepository(dbA).updateInforme(
            id = dbA.informesQueries.selectAll().executeAsList().single().id,
            tipo = "Analítica",
            descripcion = "Actualizada",
            fecha = LocalDate(2026, 2, 5),
            actualizadoEn = 7_000L
        ).getOrThrow()

        val resultado = fusionador(dbB).aplicar(exportar(dbA, "con-archivo.vpet")).getOrThrow()
        assertEquals(1, resultado.archivosCopiados)

        val recuperado = dbB.informesQueries.selectAll().executeAsList().single()
        assertTrue(existeArchivo(recuperado.rutaArchivo), "el PDF debería haber vuelto")
        assertTrue(pdf.contentEquals(leerBytes(recuperado.rutaArchivo)))
    }
}
