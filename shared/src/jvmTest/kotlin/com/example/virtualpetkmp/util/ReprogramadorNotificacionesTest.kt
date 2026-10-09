package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Vacuna
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprueba las dos reglas de aviso de la app: con cuánta antelación se avisa y con qué id se
 * registra la notificación.
 *
 * El id importa más de lo que parece: vacunas y preventivos son tablas distintas y **ambas
 * numeran sus filas desde 1**, así que si compartieran el id de notificación, el aviso de una
 * vacuna y el de un preventivo de la misma mascota se pisarían y uno de los dos se perdería
 * sin que nadie se enterara. Este test existe justo por eso.
 */
class ReprogramadorNotificacionesTest {

    /** Programador de mentira: apunta lo que le piden, sin tocar AlarmManager. */
    private class ProgramadorFalso : ProgramadorNotificaciones {
        val programadas = mutableMapOf<Long, Long>()
        val canceladas = mutableListOf<Long>()

        override fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long) {
            programadas[id] = fechaDisparoMillis
        }

        override fun cancelar(id: Long) {
            canceladas.add(id)
        }
    }

    private val hoy = LocalDate.parse("2026-10-09")

    private fun vacuna(id: Long, venceEnDias: Int) = Vacuna(
        id = id,
        mascotaId = 1L,
        nombre = "Rabia",
        fechaAplicacion = hoy,
        fechaProximaDosis = LocalDate.fromEpochDays(hoy.toEpochDays() + venceEnDias)
    )

    private fun preventivo(id: Long, venceEnDias: Int) = Preventivo(
        id = id,
        mascotaId = 1L,
        tipo = "Pipeta",
        nombre = "Frontline",
        fechaAplicacion = hoy,
        fechaProximaDosis = LocalDate.fromEpochDays(hoy.toEpochDays() + venceEnDias)
    )

    @Test
    fun `una vacuna y un preventivo con el mismo id de fila no comparten notificacion`() {
        val programador = ProgramadorFalso()

        // Mismo id de fila en las dos tablas: es el caso normal, ambas empiezan en 1.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = 60))
        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 1L, venceEnDias = 60))

        assertEquals(2, programador.programadas.size, "los dos avisos deben quedar registrados")
        assertNotEquals(
            programador.programadas.keys.first(),
            programador.programadas.keys.last()
        )
    }

    @Test
    fun `cancelar un preventivo usa el mismo id con el que se programo`() {
        val programador = ProgramadorFalso()

        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 7L, venceEnDias = 60))
        val idProgramado = programador.programadas.keys.single()

        ReprogramadorNotificaciones.cancelarPreventivo(programador, 7L)

        assertEquals(listOf(idProgramado), programador.canceladas)
    }

    @Test
    fun `la vacuna avisa 15 dias antes y el preventivo 1 dia antes`() {
        val programador = ProgramadorFalso()

        // Vencen dentro de 60 días, así que ambos avisos caen en el futuro y se programan.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = 60))
        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 2L, venceEnDias = 60))

        val diasHasta = { millis: Long ->
            val fechaAviso = Instant.fromEpochMilliseconds(millis)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            fechaAviso.toEpochDays() - hoy.toEpochDays()
        }

        val avisoVacuna = diasHasta(programador.programadas.values.first())
        val avisoPreventivo = diasHasta(programador.programadas.values.last())

        assertEquals(45, avisoVacuna, "60 días menos los 15 de antelación de la vacuna")
        assertEquals(59, avisoPreventivo, "60 días menos el día de antelación del preventivo")
    }

    @Test
    fun `una dosis cuyo aviso ya ha pasado no programa nada`() {
        val programador = ProgramadorFalso()

        // Venció hace 30 días: el aviso habría caído en el pasado.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = -30))

        assertTrue(programador.programadas.isEmpty())
    }
}
