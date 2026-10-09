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

/**
 * Traduce las dosis pendientes de vacunas y preventivos a notificaciones locales.
 *
 * Aquí viven las dos únicas reglas de aviso de la app: una vacuna avisa 15 días antes de la
 * próxima dosis y un preventivo 1 día antes, siempre a las 9 de la mañana. Si la fecha de
 * aviso ya ha pasado, no se programa nada (no tiene sentido avisar de algo que ya venció).
 *
 * Lo llama sobre todo `FichaMascotaViewModel.cargarResumen()`: al abrir la ficha de una
 * mascota se cancelan y se vuelven a programar sus avisos, y ese es el mecanismo que mantiene
 * las notificaciones al día sin un servicio en segundo plano.
 */
object ReprogramadorNotificaciones {

    /** Días antes de la próxima dosis para avisar de una vacuna. */
    private const val DIAS_AVISO_VACUNA = 15

    /** Días antes de la próxima dosis para avisar de un preventivo. */
    private const val DIAS_AVISO_PREVENTIVO = 1

    /** Hora del día a la que se lanza la notificación. */
    private const val HORA_AVISO = 9

    /**
     * Desplazamiento que se suma al id de un preventivo para obtener su id de notificación.
     *
     * Vacunas y preventivos son tablas distintas y **ambas numeran sus filas desde 1**, así que
     * usar el id tal cual hacía que una vacuna y un preventivo de la misma mascota pidieran la
     * misma alarma: la segunda sustituía a la primera y uno de los dos avisos se perdía en
     * silencio. Con dos rangos separados no pueden chocar.
     */
    private const val DESPLAZAMIENTO_PREVENTIVO = 1_000_000_000L

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
        val id = preventivo.id ?: return
        programar(
            programador = programador,
            id = id + DESPLAZAMIENTO_PREVENTIVO,
            nombre = preventivo.nombre,
            fechaProximaDosis = preventivo.fechaProximaDosis,
            diasAviso = DIAS_AVISO_PREVENTIVO,
            titulo = "Preventivo pendiente",
            prefijoMensaje = "A tu mascota le toca el preventivo"
        )
    }

    /** Cancela el aviso de un preventivo, usando el mismo id desplazado que al programarlo. */
    fun cancelarPreventivo(programador: ProgramadorNotificaciones, id: Long) {
        programador.cancelar(id + DESPLAZAMIENTO_PREVENTIVO)
    }

    /** Cancela el aviso de una vacuna. */
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
