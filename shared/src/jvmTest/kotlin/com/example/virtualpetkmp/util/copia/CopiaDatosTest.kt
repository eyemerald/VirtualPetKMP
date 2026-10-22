package com.example.virtualpetkmp.util.copia

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.data.VeterinarioRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.escribirBytes
import com.example.virtualpetkmp.util.escribirZip
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.leerBytes
import com.example.virtualpetkmp.util.listarArchivos
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ida y vuelta de la copia de seguridad con **archivos de verdad**, en directorios temporales.
 *
 * Lo que se comprueba aquí es lo que no se puede comprobar mirando la pantalla: que una
 * restauración deja la base de datos igual (incluidos los identificadores universales), que los
 * PDFs y las fotos viajan dentro del paquete y vuelven a abrirse en el otro lado, y que un paquete
 * roto **no toca nada**.
 */
class CopiaDatosTest {

    private lateinit var raiz: File
    private lateinit var carpetas: CarpetasApp
    private lateinit var driverA: SqlDriver
    private lateinit var dbA: VirtualPetDatabase

    @BeforeTest
    fun setup() {
        raiz = Files.createTempDirectory("virtualpet-copia").toFile()
        carpetas = CarpetasApp(
            fotos = File(raiz, "fotos").apply { mkdirs() }.absolutePath,
            informes = File(raiz, "informes").apply { mkdirs() }.absolutePath,
            copias = File(raiz, "copias").apply { mkdirs() }.absolutePath
        )
        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driverA)
        dbA = VirtualPetDatabase(driverA)
    }

    @AfterTest
    fun tearDown() {
        driverA.close()
        raiz.deleteRecursively()
    }

    private fun baseDeDatosVacia(): Pair<SqlDriver, VirtualPetDatabase> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        return driver to VirtualPetDatabase(driver)
    }

    private fun exportador(db: VirtualPetDatabase) =
        ExportadorCopia(db, carpetas, versionApp = "prueba", plataforma = "test")

    private fun importador(db: VirtualPetDatabase, copias: Int = 3) =
        ImportadorCopia(db, carpetas, exportador(db), copiasAConservar = copias)

    private val foto = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 4, 5)
    private val pdf = "%PDF-1.4 contenido de prueba".toByteArray()

    /** Deja una mascota con historial completo y sus dos archivos en disco. */
    private fun sembrar(db: VirtualPetDatabase): Long = runBlocking {
        val rutaFoto = "${carpetas.fotos}/foto-luna.jpg"
        escribirBytes(rutaFoto, foto)
        val rutaPdf = "${carpetas.informes}/analitica.pdf"
        escribirBytes(rutaPdf, pdf)

        val mascotaId = MascotaRepository(db).insertMascota(
            com.example.virtualpetkmp.Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Labrador Retriever",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = "941000012345678",
                foto = rutaFoto,
                pesoIdeal = 27.0
            )
        ).getOrThrow()

        VacunaRepository(db).insertVacuna(
            mascotaId = mascotaId,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2025, 1, 10),
            fechaProximaDosis = LocalDate(2026, 1, 10),
            veterinario = "Dra. Ruiz",
            lote = "L-1"
        ).getOrThrow()

        PreventivoRepository(db).insertPreventivo(
            mascotaId = mascotaId,
            tipo = "Pipeta",
            nombre = "Frontline",
            fechaAplicacion = LocalDate(2026, 2, 1),
            fechaProximaDosis = LocalDate(2026, 3, 1),
            veterinario = "Dra. Ruiz",
            lote = "P-2"
        ).getOrThrow()

        RevisionRepository(db).insertRevision(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 2),
            motivo = "Revisión anual",
            diagnostico = "Todo bien",
            notas = "Sin incidencias",
            veterinario = "Dra. Ruiz"
        ).getOrThrow()

        TratamientoRepository(db).insertTratamiento(
            mascotaId = mascotaId,
            nombreMedicamento = "Oticoear",
            dosis = "4 gotas",
            frecuencia = "Cada 12 horas",
            fechaInicio = LocalDate(2026, 2, 3),
            fechaFin = LocalDate(2026, 2, 20),
            hora = "09:00",
            notificar = true
        ).getOrThrow()

        PesoRepository(db).insertPeso(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 4),
            peso = 26.3,
            notas = null
        ).getOrThrow()

        InformeRepository(db).insertInforme(
            mascotaId = mascotaId,
            tipo = "Analítica",
            descripcion = "Resultados normales",
            fecha = LocalDate(2026, 2, 5),
            nombreArchivo = "analitica.pdf",
            rutaArchivo = rutaPdf
        ).getOrThrow()

        NotaRepository(db).insertNota(mascotaId, "Come bien").getOrThrow()

        VeterinarioRepository(db).insertVeterinario(
            Veterinario(
                nombreClinica = "Clínica del Barrio",
                nombreVeterinario = "Dr. López",
                telefono = "918765432",
                direccion = "Avenida del Parque 4",
                esUrgencias = false
            )
        ).getOrThrow()

        mascotaId
    }

    @Test
    fun `la copia guarda todos los datos y los dos archivos`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath

        val exportado = exportador(dbA).exportar(destino).getOrThrow()

        assertTrue(existeArchivo(destino), "no se escribió el paquete")
        assertEquals(9, exportado.filas, "el paquete debe llevar las nueve filas")
        assertEquals(2, exportado.archivos, "faltan la foto y el PDF")
        assertEquals(0, exportado.archivosQueFaltaban)
        assertTrue(exportado.bytes > 0)
    }

    @Test
    fun `restaurar en una base vacia deja los datos y los archivos igual`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        val (driverB, dbB) = baseDeDatosVacia()
        try {
            val resultado = importador(dbB).restaurar(destino).getOrThrow()
            assertEquals(1, resultado.mascotas)
            assertEquals(9, resultado.filas)
            assertEquals(2, resultado.archivosCopiados)
            assertEquals(0, resultado.filasSinMascota)

            // Los datos, fila a fila.
            val mascota = dbB.mascotasQueries.selectAll().executeAsList().single()
            assertEquals("Luna", mascota.nombre)
            assertEquals("941000012345678", mascota.microchip)
            assertEquals(27.0, mascota.peso_ideal)
            assertEquals(32, mascota.uuid.length, "el uuid debe conservarse al restaurar")

            assertEquals("Rabia", dbB.vacunasQueries.selectAll().executeAsList().single().nombre)
            assertEquals("Frontline", dbB.preventivosQueries.selectAll().executeAsList().single().nombre)
            assertEquals("Revisión anual", dbB.revisionesQueries.selectAll().executeAsList().single().motivo)
            assertEquals("Oticoear", dbB.tratamientosQueries.selectAll().executeAsList().single().nombreMedicamento)
            assertEquals(26.3, dbB.pesosQueries.selectAll().executeAsList().single().peso)
            assertEquals("Come bien", dbB.notasQueries.selectAll().executeAsList().single().texto)
            assertEquals("Clínica del Barrio", dbB.veterinariosQueries.selectAll().executeAsList().single().nombreClinica)

            // Y los archivos: la ruta es nueva (de este dispositivo), pero el contenido es el mismo.
            val fotoRestaurada = dbB.mascotasQueries.selectAll().executeAsList().single().foto
            assertNotNull(fotoRestaurada)
            assertTrue(existeArchivo(fotoRestaurada), "la foto restaurada no está en disco")
            assertTrue(foto.contentEquals(leerBytes(fotoRestaurada)), "la foto cambió de contenido")

            val informe = dbB.informesQueries.selectAll().executeAsList().single()
            assertTrue(existeArchivo(informe.rutaArchivo), "el PDF restaurado no está en disco")
            assertTrue(pdf.contentEquals(leerBytes(informe.rutaArchivo)), "el PDF cambió de contenido")
            assertEquals("analitica.pdf", informe.nombreArchivo, "el nombre visible no debe cambiar")

            // El tratamiento conserva si avisa y a qué hora (si no, se perderían los recordatorios).
            val tratamiento = dbB.tratamientosQueries.selectAll().executeAsList().single()
            assertEquals("09:00", tratamiento.hora)
            assertEquals(1L, tratamiento.notificar)
        } finally {
            driverB.close()
        }
    }

    @Test
    fun `restaurar dos veces seguidas no duplica nada`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        val importador = importador(dbA)
        importador.restaurar(destino).getOrThrow()
        importador.restaurar(destino).getOrThrow()

        assertEquals(1, dbA.mascotasQueries.selectAll().executeAsList().size)
        assertEquals(1, dbA.vacunasQueries.selectAll().executeAsList().size)
        assertEquals(1, dbA.notasQueries.selectAll().executeAsList().size)
        assertEquals(1, dbA.informesQueries.selectAll().executeAsList().size)
    }

    @Test
    fun `restaurar reemplaza lo que habia antes`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        // Ahora la base tiene una mascota más, que no está en el paquete.
        MascotaRepository(dbA).insertMascota(
            com.example.virtualpetkmp.Mascota(
                nombre = "Nilo",
                especie = "Gato",
                raza = "Común Europeo",
                fechaNacimiento = LocalDate(2023, 5, 1),
                sexo = "Macho",
                color = "Atigrado",
                microchip = null
            )
        ).getOrThrow()

        importador(dbA).restaurar(destino).getOrThrow()

        val nombres = dbA.mascotasQueries.selectAll().executeAsList().map { it.nombre }
        assertEquals(listOf("Luna"), nombres, "restaurar tiene que dejar exactamente lo del paquete")
    }

    @Test
    fun `un paquete roto no toca la base de datos`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        // Se corta el fichero a la mitad: es lo que pasa con una copia interrumpida.
        val bytes = leerBytes(destino)!!
        escribirBytes(destino, bytes.copyOf(bytes.size / 2))

        val antes = dbA.mascotasQueries.selectAll().executeAsList().size
        val resultado = importador(dbA).restaurar(destino)

        assertTrue(resultado.isFailure, "un paquete cortado no puede restaurarse")
        val error = (resultado.exceptionOrNull() as? ErrorCopiaException)?.error
        assertTrue(
            error == ErrorCopia.PAQUETE_ILEGIBLE || error == ErrorCopia.DATOS_INCOMPLETOS,
            "error inesperado: $error"
        )
        assertEquals(antes, dbA.mascotasQueries.selectAll().executeAsList().size, "se perdió la mascota")
        assertEquals("Luna", dbA.mascotasQueries.selectAll().executeAsList().single().nombre)
    }

    @Test
    fun `un zip que no es de la app se rechaza`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "ajeno.vpet").absolutePath
        escribirZip(destino, mapOf("datos.json" to """{"formato":"otra-cosa"}"""), emptyMap())

        val resultado = importador(dbA).restaurar(destino)

        assertTrue(resultado.isFailure)
        val error = (resultado.exceptionOrNull() as? ErrorCopiaException)?.error
        assertTrue(
            error == ErrorCopia.NO_ES_DE_VIRTUALPET || error == ErrorCopia.DATOS_INCOMPLETOS,
            "error inesperado: $error"
        )
        assertEquals(1, dbA.mascotasQueries.selectAll().executeAsList().size)
    }

    @Test
    fun `se conservan solo las tres ultimas copias automaticas`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        val importador = importador(dbA, copias = 3)
        repeat(5) { importador.restaurar(destino).getOrThrow() }

        val copias = listarArchivos(carpetas.copias).filter { it.endsWith(".vpet") }
        assertEquals(3, copias.size, "deberían quedar 3 copias, hay ${copias.size}")
    }

    @Test
    fun `la vista previa cuenta lo que trae el paquete sin tocar la base`() = runBlocking {
        sembrar(dbA)
        val destino = File(raiz, "copia.vpet").absolutePath
        exportador(dbA).exportar(destino).getOrThrow()

        val (driverB, dbB) = baseDeDatosVacia()
        try {
            val vista = importador(dbB).vistaPrevia(destino).getOrThrow()

            assertEquals(listOf("Luna"), vista.nombresMascotas)
            assertEquals(9, vista.totalFilas)
            assertEquals(2, vista.archivos)
            assertTrue(vista.bytesArchivos > 0, "los adjuntos deben contar bytes")
            assertTrue(vista.creadoEn.isNotBlank())

            // Ver la vista previa no puede haber escrito nada.
            assertEquals(0, dbB.mascotasQueries.selectAll().executeAsList().size)
        } finally {
            driverB.close()
        }
    }

    @Test
    fun `los adjuntos que no estan en el dispositivo no rompen la exportacion`() = runBlocking {
        sembrar(dbA)
        // Se borra el PDF del disco pero la fila sigue en la base de datos.
        val informe = dbA.informesQueries.selectAll().executeAsList().single()
        File(informe.rutaArchivo).delete()
        assertFalse(existeArchivo(informe.rutaArchivo))

        val destino = File(raiz, "copia.vpet").absolutePath
        val exportado = exportador(dbA).exportar(destino).getOrThrow()

        assertEquals(1, exportado.archivos, "solo debería entrar la foto")
        assertEquals(1, exportado.archivosQueFaltaban, "debería contar el PDF que faltaba")
    }
}
