package com.example.virtualpetkmp.util.copia

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.existeArchivo
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * "Deshacer" una fusión cuando la copia previa es la de una base **vacía**.
 *
 * Es el caso de un dispositivo recién instalado: se fusiona un paquete sobre una base sin datos, así
 * que la copia automática que se guarda antes de fusionar no tiene ni una mascota. Al pulsar
 * Deshacer, el lector de paquetes la rechaza (`DATOS_INCOMPLETOS`: un paquete sin mascotas ni
 * veterinarios no se acepta como copia) y **la fusión se queda como estaba**.
 *
 * Lo que se comprueba aquí es lo que de verdad importa: que ese fallo no deja nada a medias y que
 * los datos fusionados siguen intactos, porque el usuario acaba de leer que su copia es "incompleta
 * o dañada" y tiene que poder seguir usando la app.
 */
class DeshacerSobreBaseVaciaTest {

    private lateinit var raiz: File
    private lateinit var carpetas: CarpetasApp
    private lateinit var driverOrigen: SqlDriver
    private lateinit var dbOrigen: VirtualPetDatabase
    private lateinit var driverLocal: SqlDriver
    private lateinit var dbLocal: VirtualPetDatabase

    @AfterTest
    fun tearDown() {
        if (::driverOrigen.isInitialized) driverOrigen.close()
        if (::driverLocal.isInitialized) driverLocal.close()
        if (::raiz.isInitialized) raiz.deleteRecursively()
    }

    private fun preparar() {
        raiz = Files.createTempDirectory("virtualpet-deshacer").toFile()
        carpetas = CarpetasApp(
            fotos = File(raiz, "fotos").apply { mkdirs() }.absolutePath,
            informes = File(raiz, "informes").apply { mkdirs() }.absolutePath,
            copias = File(raiz, "copias").apply { mkdirs() }.absolutePath
        )
        driverOrigen = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { VirtualPetDatabase.Schema.create(it) }
        dbOrigen = VirtualPetDatabase(driverOrigen)
        driverLocal = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { VirtualPetDatabase.Schema.create(it) }
        dbLocal = VirtualPetDatabase(driverLocal)
    }

    private fun exportador(db: VirtualPetDatabase) =
        ExportadorCopia(db, carpetas, versionApp = "prueba", plataforma = "test")

    /** El paquete que trae el otro dispositivo: una mascota con un peso y una nota. */
    private fun sembrarOrigen(): String = runBlocking {
        val mascotaId = MascotaRepository(dbOrigen).insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Mestizo",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = null
            ),
            uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
            actualizadoEn = 1_000L
        ).getOrThrow()
        PesoRepository(dbOrigen).insertPeso(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 4),
            peso = 26.3,
            notas = null,
            uuid = "peso-1",
            actualizadoEn = 1_000L
        ).getOrThrow()
        NotaRepository(dbOrigen).insertNota(
            mascotaId = mascotaId,
            texto = "Come bien",
            uuid = "nota-1",
            actualizadoEn = 1_000L
        ).getOrThrow()

        val destino = File(raiz, "de-otro-movil.vpet").absolutePath
        exportador(dbOrigen).exportar(destino).getOrThrow()
        destino
    }

    private fun filas(db: VirtualPetDatabase): List<Int> = listOf(
        db.mascotasQueries.selectAll().executeAsList().size,
        db.pesosQueries.selectAll().executeAsList().size,
        db.notasQueries.selectAll().executeAsList().size
    )

    @Test
    fun `deshacer sobre una base vacia falla sin tocar los datos fusionados`() = runBlocking {
        preparar()
        val paquete = sembrarOrigen()

        // 1. Se fusiona en el dispositivo vacío. Antes se guarda la copia previa, que aquí está vacía.
        val fusion = ImportadorFusion(dbLocal, carpetas, exportador(dbLocal))
            .aplicar(paquete)
            .getOrThrow()
        assertEquals(listOf(1, 1, 1), filas(dbLocal), "la fusión tiene que traer las tres filas")
        val copiaPrevia = fusion.copiaPrevia
        assertNotNull(copiaPrevia, "fusionar sobre una base vacía debe dejar copia previa")
        assertTrue(existeArchivo(copiaPrevia), "la copia previa tiene que estar en disco")

        // 2. Deshacer: se restaura esa copia previa. El lector la rechaza porque no tiene ni una
        //    mascota, que es lo que hace que "Deshacer" falle en este caso.
        val vuelta = ImportadorCopia(dbLocal, carpetas, exportador(dbLocal)).restaurar(copiaPrevia)

        assertTrue(vuelta.isFailure, "una copia sin datos no se puede restaurar")
        assertEquals(
            ErrorCopia.DATOS_INCOMPLETOS,
            assertIs<ErrorCopiaException>(vuelta.exceptionOrNull()).error,
            "el motivo del fallo tiene que ser el de la copia vacía, para poder decírselo al usuario"
        )

        // 3. Lo importante: no se ha tocado nada de lo fusionado.
        assertEquals(
            listOf(1, 1, 1),
            filas(dbLocal),
            "un deshacer fallido no puede borrar ni duplicar los datos de la fusión"
        )
        val mascota = dbLocal.mascotasQueries.selectAll().executeAsList().single()
        assertEquals("Luna", mascota.nombre)
        assertEquals(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
            mascota.uuid,
            "y la mascota tiene que seguir siendo la misma (mismo uuid)"
        )
        assertEquals(26.3, dbLocal.pesosQueries.selectAll().executeAsList().single().peso)
        assertEquals("Come bien", dbLocal.notasQueries.selectAll().executeAsList().single().texto)
        assertTrue(existeArchivo(copiaPrevia), "y la copia previa sigue ahí, para poder reintentar")
    }
}
