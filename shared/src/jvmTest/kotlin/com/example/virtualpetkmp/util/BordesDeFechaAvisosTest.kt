package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los bordes de fecha de los avisos de un tratamiento: el último día y las horas del día extremas.
 *
 * Son los dos sitios donde una regla de fechas se rompe sin que nadie lo note: un tratamiento que
 * acaba **hoy** tiene que avisar hoy (y no reprogramar nada para mañana), y las tomas de las 00:00 y
 * de las 23:59 tienen que caer en el día que toca y no en el de al lado. Es lógica pura, sin base de
 * datos ni móvil.
 */
class BordesDeFechaAvisosTest {

    private val hoy = LocalDate(2026, 5, 20)
    private val manana = hoy.plus(DatePeriod(days = 1))

    private fun tratamiento(
        hora: String?,
        horas: String? = null,
        vecesAlDia: Int? = null,
        fin: LocalDate? = hoy
    ) = Tratamiento(
        id = 7L,
        mascotaId = 1L,
        nombreMedicamento = "Oticoear",
        fechaInicio = hoy.minus(DatePeriod(days = 5)),
        fechaFin = fin,
        hora = hora,
        notificar = true,
        uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
        vecesAlDia = vecesAlDia,
        horas = horas
    )

    // --- El último día del tratamiento -------------------------------------------------------

    @Test
    fun `el tratamiento que acaba hoy sigue avisando a su hora de hoy`() {
        val t = tratamiento("08:00")

        assertEquals(
            LocalDateTime(2026, 5, 20, 8, 0),
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 20, 7, 0)),
            "si el último día es hoy, la toma de hoy tiene que sonar"
        )
    }

    @Test
    fun `el tratamiento que acaba hoy no se reprograma para manana`() {
        val t = tratamiento("08:00")

        assertNull(
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 20, 8, 0)),
            "al sonar justo a su hora, el siguiente sería mañana: ya no toca"
        )
        assertNull(
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 20, 9, 30)),
            "pasada la hora del último día no hay nada que programar"
        )
        assertTrue(
            RecordatoriosTratamiento.proximosAvisos(t, desde = LocalDateTime(2026, 5, 21, 0, 0)).isEmpty(),
            "mañana el tratamiento está terminado: ninguna toma"
        )
    }

    @Test
    fun `un tratamiento cronico sigue avisando manana`() {
        val t = tratamiento("08:00", fin = null)

        assertEquals(
            LocalDateTime(2026, 5, 21, 8, 0),
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 20, 8, 0)),
            "sin fecha de fin no hay último día"
        )
    }

    // --- Las horas extremas del día -----------------------------------------------------------

    @Test
    fun `las tomas de las 00 y de las 2359 caen en el dia que toca`() {
        val t = tratamiento(hora = "00:00", horas = "00:00,23:59", vecesAlDia = 2, fin = null)

        // A las 00:30 la toma de medianoche ya pasó; la de las 23:59 es hoy mismo.
        assertEquals(
            listOf(LocalDateTime(2026, 5, 21, 0, 0), LocalDateTime(2026, 5, 20, 23, 59)),
            RecordatoriosTratamiento.proximosAvisos(t, desde = LocalDateTime(2026, 5, 20, 0, 30)),
            "medianoche ya pasó (mañana) y 23:59 es hoy"
        )
    }

    @Test
    fun `justo a medianoche la toma de las 00 00 ya cuenta como pasada`() {
        val t = tratamiento(hora = "00:00", horas = "00:00,23:59", vecesAlDia = 2, fin = null)

        assertEquals(
            listOf(LocalDateTime(2026, 5, 21, 0, 0), LocalDateTime(2026, 5, 20, 23, 59)),
            RecordatoriosTratamiento.proximosAvisos(t, desde = LocalDateTime(2026, 5, 20, 0, 0)),
            "a la hora exacta se considera que ya ha pasado, como el resto de tomas"
        )
    }

    @Test
    fun `la toma de las 2359 sigue siendo del ultimo dia`() {
        val t = tratamiento(hora = "23:59", fin = hoy)

        assertEquals(
            LocalDateTime(2026, 5, 20, 23, 59),
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 20, 12, 0)),
            "las 23:59 del último día todavía son suyas"
        )
        assertNull(
            RecordatoriosTratamiento.proximoAviso(t, toma = 0, desde = LocalDateTime(2026, 5, 21, 0, 0)),
            "y al día siguiente ya no"
        )
    }

    /**
     * El caso que junta los dos bordes: posponer 30 minutos la última toma de un tratamiento que
     * acaba hoy a las 23:59 la llevaría a mañana, fuera del tratamiento.
     */
    @Test
    fun `posponer la toma de las 2359 del ultimo dia no alarga el tratamiento`() {
        assertTrue(
            RecordatoriosTratamiento.dentroDelTratamiento(LocalDate(2026, 5, 20), hoy),
            "el mismo día del fin sí vale"
        )
        assertTrue(
            !RecordatoriosTratamiento.dentroDelTratamiento(manana, hoy),
            "el día siguiente al fin no: posponer no puede alargar la medicación"
        )
    }
}
