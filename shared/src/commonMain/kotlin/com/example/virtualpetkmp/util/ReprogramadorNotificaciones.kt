package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Vacuna
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

object ReprogramadorNotificaciones {

    /** Días antes de la próxima dosis para avisar de una vacuna. */
    private const val DIAS_AVISO_VACUNA = 15

    /** Días antes de la próxima dosis para avisar de un preventivo. */
    private const val DIAS_AVISO_PREVENTIVO = 1

    /** Hora del día a la que se lanza la notificación. */
    private const val HORA_AVISO = 9

    fun programarVacuna(programador: ProgramadorNotificaciones, vacuna: Vacuna) {
        programar(
            programador = programador,
            id = vacuna.id ?: return,
            nombre = vacuna.nombre,
            fechaProximaDosis = vacuna.fechaProximaDosis,
            diasAviso = DIAS_AVISO_VACUNA,
            titulo = "Vacuna pendiente",
            prefijoMensaje = "A tu mascota le toca la vacuna"
        )
    }

    fun programarPreventivo(programador: ProgramadorNotificaciones, preventivo: Preventivo) {
        programar(
            programador = programador,
            id = preventivo.id ?: return,
            nombre = preventivo.nombre,
            fechaProximaDosis = preventivo.fechaProximaDosis,
            diasAviso = DIAS_AVISO_PREVENTIVO,
            titulo = "Preventivo pendiente",
            prefijoMensaje = "A tu mascota le toca el preventivo"
        )
    }

    fun cancelar(programador: ProgramadorNotificaciones, id: Long) {
        programador.cancelar(id)
    }

    private fun programar(
        programador: ProgramadorNotificaciones,
        id: Long,
        nombre: String,
        fechaProximaDosis: LocalDate,
        diasAviso: Int,
        titulo: String,
        prefijoMensaje: String
    ) {
        try {
            val fechaAviso = fechaProximaDosis.minus(DatePeriod(days = diasAviso))
            val fechaHora = LocalDateTime(
                fechaAviso.year,
                fechaAviso.monthNumber,
                fechaAviso.dayOfMonth,
                HORA_AVISO,
                0
            )
            val millis = fechaHora.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            if (millis > Clock.System.now().toEpochMilliseconds()) {
                programador.programar(
                    id = id,
                    titulo = titulo,
                    mensaje = "$prefijoMensaje \"$nombre\" el ${fechaProximaDosis.dayOfMonth}/${fechaProximaDosis.monthNumber}/${fechaProximaDosis.year}",
                    fechaDisparoMillis = millis
                )
            }
        } catch (e: Exception) {
            println("Error programando notificación: ${e.message}")
        }
    }
}
