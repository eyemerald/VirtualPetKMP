package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La regla del recordatorio de tratamientos.
 *
 * Es lo único que decide **cuándo** suena el aviso, y lo usan los dos extremos de la alarma: el
 * que la programa (`ReprogramadorNotificaciones`) y el receptor que, al saltar, programa la del
 * día siguiente. Si esta regla se equivoca, o no avisa o avisa de más —y no hay ninguna pantalla
 * donde eso se vea—, así que se prueba aquí, sin móvil.
 */
class RecordatoriosTratamientoTest {

    private val hoy = LocalDate.parse("2026-05-20")

    private fun momento(dia: Int, hora: Int, minuto: Int = 0) =
        LocalDateTime(2026, 5, dia, hora, minuto)

    private fun tratamiento(
        hora: String? = "08:30",
        inicio: LocalDate = hoy,
        fin: LocalDate? = hoy,
        notificar: Boolean = true
    ) = Tratamiento(
        id = 7L,
        mascotaId = 1L,
        nombreMedicamento = "Oticoear",
        fechaInicio = inicio,
        fechaFin = fin,
        hora = hora,
        notificar = notificar
    )

    // --- Leer y escribir la hora ----------------------------------------------

    @Test
    fun `lee una hora con dos cifras`() {
        assertEquals(8 to 30, RecordatoriosTratamiento.leerHora("08:30"))
    }

    @Test
    fun `acepta la hora con una sola cifra`() {
        assertEquals(8 to 30, RecordatoriosTratamiento.leerHora("8:30"))
    }

    @Test
    fun `rechaza lo que no es una hora`() {
        assertNull(RecordatoriosTratamiento.leerHora("24:00"), "no existe la hora 24")
        assertNull(RecordatoriosTratamiento.leerHora("08:60"), "no existe el minuto 60")
        assertNull(RecordatoriosTratamiento.leerHora("8"), "falta el minuto")
        assertNull(RecordatoriosTratamiento.leerHora("ocho y media"))
        assertNull(RecordatoriosTratamiento.leerHora(""))
        assertNull(RecordatoriosTratamiento.leerHora(null))
    }

    @Test
    fun `escribe la hora siempre con dos cifras`() {
        assertEquals("08:05", RecordatoriosTratamiento.formatearHora(8, 5))
        assertEquals("00:00", RecordatoriosTratamiento.formatearHora(0, 0))
        assertEquals("23:59", RecordatoriosTratamiento.formatearHora(23, 59))
    }

    // --- Cuándo toca avisar ----------------------------------------------------

    @Test
    fun `si la hora de hoy no ha pasado, avisa hoy`() {
        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = hoy,
            fin = hoy,
            desde = momento(20, 7)
        )

        assertEquals(momento(20, 8, 30), proximo)
    }

    @Test
    fun `si la hora de hoy ya paso, avisa manana`() {
        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = hoy,
            fin = LocalDate.parse("2026-05-25"),
            desde = momento(20, 9)
        )

        assertEquals(momento(21, 8, 30), proximo)
    }

    @Test
    fun `justo a la hora ya cuenta como pasada, para no avisar dos veces`() {
        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = hoy,
            fin = LocalDate.parse("2026-05-25"),
            desde = momento(20, 8, 30)
        )

        assertEquals(momento(21, 8, 30), proximo)
    }

    @Test
    fun `no avisa antes de empezar el tratamiento`() {
        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = LocalDate.parse("2026-05-25"),
            fin = LocalDate.parse("2026-05-30"),
            desde = momento(20, 9)
        )

        assertEquals(LocalDateTime(2026, 5, 25, 8, 30), proximo)
    }

    @Test
    fun `no avisa despues del ultimo dia`() {
        // El 20 a las 7 todavía toca hoy; a las 9 el siguiente sería el 21, ya fuera del
        // tratamiento, así que no hay aviso.
        val hoyTodavia = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = hoy,
            fin = hoy,
            desde = momento(20, 7)
        )
        val yaNo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = hoy,
            fin = hoy,
            desde = momento(20, 9)
        )

        assertEquals(momento(20, 8, 30), hoyTodavia)
        assertNull(yaNo)
    }

    @Test
    fun `un tratamiento cronico avisa todos los dias`() {
        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = "08:30",
            inicio = LocalDate.parse("2026-01-01"),
            fin = null,
            desde = momento(20, 9)
        )

        assertEquals(momento(21, 8, 30), proximo)
    }

    @Test
    fun `sin hora no hay aviso`() {
        assertNull(
            RecordatoriosTratamiento.proximoAviso(
                hora = null,
                inicio = hoy,
                fin = null,
                desde = momento(20, 7)
            )
        )
    }

    @Test
    fun `con una hora invalida no hay aviso`() {
        assertNull(
            RecordatoriosTratamiento.proximoAviso(
                hora = "25:99",
                inicio = hoy,
                fin = null,
                desde = momento(20, 7)
            )
        )
    }

    @Test
    fun `con el aviso apagado no hay nada programado`() {
        assertNull(RecordatoriosTratamiento.proximoAviso(tratamiento(notificar = false), momento(20, 7)))
    }

    @Test
    fun `el atajo con el tratamiento usa su hora y su fecha de fin`() {
        val conAviso = tratamiento(fin = LocalDate.parse("2026-05-21"))

        assertEquals(
            momento(21, 8, 30),
            RecordatoriosTratamiento.proximoAviso(conAviso, momento(20, 9))
        )
    }

    // --- Posponer ------------------------------------------------------------

    /**
     * Posponer cuenta desde **el momento de pulsar el botón**, no desde la hora a la que sonó.
     *
     * Si contara desde la hora del aviso, quien pospone diez minutos tarde vería el siguiente
     * aviso veinte minutos después en lugar de treinta, y el texto del botón estaría mintiendo.
     */
    @Test
    fun `posponer cuenta los 30 minutos desde que se pulsa`() {
        val aviso = AvisoTratamiento(
            notificacionId = RecordatoriosTratamiento.idNotificacion(1L),
            tratamientoId = 1L,
            mascotaId = 1L,
            titulo = "Hora de la medicina",
            mensaje = "Para Luna: Oticoear",
            fechaDisparoMillis = 1_000_000L,
            hora = "08:30"
        )

        // Se pulsa once minutos después de sonar.
        val nuevo = aviso.pospuesto(desdeMillis = 1_000_000L + 11 * 60_000L)

        assertEquals(
            1_000_000L + (11 + 30) * 60_000L,
            nuevo.fechaDisparoMillis,
            "el siguiente aviso debe caer 30 minutos después de pulsar, no de sonar"
        )
        assertEquals("08:30", nuevo.hora, "la hora del tratamiento no cambia al posponer")
    }

    @Test
    fun `posponer respeta el ultimo dia del tratamiento`() {
        val fin = LocalDate.parse("2026-05-20")

        // El último día sí: el tratamiento dura hasta esa fecha inclusive.
        assertTrue(RecordatoriosTratamiento.dentroDelTratamiento(fin, fin))
        // Al día siguiente ya no toca nada.
        assertFalse(RecordatoriosTratamiento.dentroDelTratamiento(LocalDate.parse("2026-05-21"), fin))
    }

    @Test
    fun `un tratamiento cronico admite posponer cualquier dia`() {
        assertTrue(
            RecordatoriosTratamiento.dentroDelTratamiento(LocalDate.parse("2030-01-01"), null),
            "sin fecha de fin no hay límite"
        )
    }

    // --- Ids de notificación ---------------------------------------------------

    @Test
    fun `el id de la notificacion no choca con el de vacunas ni preventivos`() {
        val idTratamiento = RecordatoriosTratamiento.idNotificacion(1L)

        assertNotEquals(1L, idTratamiento, "el 1 lo usan las vacunas")
        assertNotEquals(1L + 1_000_000_000L, idTratamiento, "el 1.000.000.001 lo usan los preventivos")
        assertTrue(idTratamiento > 0, "un id negativo no valdría para PendingIntent")
    }
}
