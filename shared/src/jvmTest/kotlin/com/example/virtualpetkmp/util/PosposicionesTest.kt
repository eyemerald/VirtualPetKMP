package com.example.virtualpetkmp.util

import java.io.File
import java.nio.file.Files
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * La posposición de un aviso tiene que **sobrevivir al reinicio del móvil**.
 *
 * El fallo real: aviso de las 13:34 pospuesto 30 minutos, teléfono reiniciado, y no volvió a sonar
 * —el receptor de arranque programó el aviso *diario* siguiente, o sea, el día siguiente—. La causa
 * era que el momento pospuesto solo vivía en `AlarmManager`, que Android vacía al apagar.
 */
class PosposicionesTest {

    private lateinit var carpeta: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-posposiciones").toFile()
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    @Test
    fun `una posposicion guardada se lee mientras no haya pasado`() {
        Posposiciones.guardar(tratamientoId = 7L, avisoMillis = 2_000L, carpeta = carpeta.absolutePath)

        assertEquals(2_000L, Posposiciones.leer(7L, ahora = 1_000L, carpeta = carpeta.absolutePath))
    }

    @Test
    fun `una posposicion caducada no se devuelve y se limpia`() {
        Posposiciones.guardar(7L, 2_000L, carpeta.absolutePath)

        assertNull(
            Posposiciones.leer(7L, ahora = 2_500L, carpeta = carpeta.absolutePath),
            "una posposición pasada no puede volver a avisar"
        )
        assertEquals(0, Posposiciones.leer(7L, ahora = 1_000L, carpeta = carpeta.absolutePath) ?: 0L)
    }

    @Test
    fun `aceptar borra la posposicion`() {
        Posposiciones.guardar(7L, 2_000L, carpeta.absolutePath)
        Posposiciones.borrar(7L, carpeta.absolutePath)

        assertNull(Posposiciones.leer(7L, ahora = 1_000L, carpeta = carpeta.absolutePath))
    }

    @Test
    fun `cada tratamiento tiene la suya y no se pisan`() {
        Posposiciones.guardar(7L, 2_000L, carpeta.absolutePath)
        Posposiciones.guardar(9L, 3_000L, carpeta.absolutePath)

        assertEquals(2_000L, Posposiciones.leer(7L, ahora = 1_000L, carpeta = carpeta.absolutePath))
        assertEquals(3_000L, Posposiciones.leer(9L, ahora = 1_000L, carpeta = carpeta.absolutePath))
    }

    /**
     * El caso que se rompió: se pospone el aviso de hoy y el móvil se reinicia.
     *
     * Al volver, el receptor de arranque reprograma los tratamientos. Si no se mira la posposición,
     * programa la hora diaria (mañana). Con ella, programa la hora pospuesta.
     */
    @Test
    fun `al reprogramar se usa la hora pospuesta y no la del dia siguiente`() = kotlinx.coroutines.runBlocking {
        val zona = TimeZone.currentSystemDefault()
        val ahora = LocalDateTime(2026, 10, 22, 13, 40)
        val tratamiento = com.example.virtualpetkmp.Tratamiento(
            id = 7L,
            mascotaId = 1L,
            nombreMedicamento = "Oticoear",
            fechaInicio = LocalDate(2026, 10, 20),
            fechaFin = LocalDate(2026, 10, 30),
            hora = "13:34",
            notificar = true
        )
        // Se pospuso 30 minutos desde las 13:34, así que toca a las 14:04.
        val pospuesto = LocalDateTime(2026, 10, 22, 14, 4).toInstant(zona).toEpochMilliseconds()

        val programador = ProgramadorQueApunta()
        ReprogramadorNotificaciones.programarTratamiento(
            programador = programador,
            tratamiento = tratamiento,
            nombreMascota = "Luna",
            ahora = ahora,
            posposicion = { id -> if (id == 7L) pospuesto else null }
        )

        assertEquals(
            pospuesto,
            programador.millis,
            "el aviso pospuesto tiene que sobrevivir a que la app reprograme"
        )
    }

    @Test
    fun `sin posposicion se usa la hora diaria normal`() = kotlinx.coroutines.runBlocking {
        val zona = TimeZone.currentSystemDefault()
        val ahora = LocalDateTime(2026, 10, 22, 13, 40)
        val tratamiento = com.example.virtualpetkmp.Tratamiento(
            id = 7L,
            mascotaId = 1L,
            nombreMedicamento = "Oticoear",
            fechaInicio = LocalDate(2026, 10, 20),
            fechaFin = LocalDate(2026, 10, 30),
            hora = "13:34",
            notificar = true
        )

        val programador = ProgramadorQueApunta()
        ReprogramadorNotificaciones.programarTratamiento(
            programador = programador,
            tratamiento = tratamiento,
            nombreMascota = "Luna",
            ahora = ahora,
            posposicion = { null }
        )

        val manana = LocalDateTime(2026, 10, 23, 13, 34).toInstant(zona).toEpochMilliseconds()
        assertEquals(manana, programador.millis, "sin posposición toca el día siguiente a su hora")
    }

    /** Programador mínimo: solo interesa a qué hora se programó. */
    private class ProgramadorQueApunta : ProgramadorNotificaciones {
        var millis: Long = -1

        override fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long) {
            millis = fechaDisparoMillis
        }

        override fun programarTratamiento(aviso: AvisoTratamiento) {
            millis = aviso.fechaDisparoMillis
        }

        override fun cancelar(id: Long) = Unit
    }
}
