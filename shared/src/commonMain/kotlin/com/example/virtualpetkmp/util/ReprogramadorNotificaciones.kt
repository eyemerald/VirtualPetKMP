package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.aviso_preventivo_mensaje
import virtualpetkmp.shared.generated.resources.aviso_preventivo_titulo
import virtualpetkmp.shared.generated.resources.aviso_tratamiento_mensaje
import virtualpetkmp.shared.generated.resources.aviso_tratamiento_titulo
import virtualpetkmp.shared.generated.resources.aviso_vacuna_mensaje
import virtualpetkmp.shared.generated.resources.aviso_vacuna_titulo

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
 *
 * **El texto de la notificación está traducido**: es lo primero que ve el usuario y sale fuera
 * de la app, así que se lee con `getString` (la variante `suspend`, porque aquí no hay contexto
 * composable) y la fecha se formatea con la configuración regional que se le pasa.
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

    /**
     * Programa el aviso de la próxima dosis de una vacuna.
     *
     * @param localizacion configuración regional activa, para escribir la fecha como la espera
     *   el usuario. Por defecto, español de España, que es lo que usan los tests.
     */
    suspend fun programarVacuna(
        programador: ProgramadorNotificaciones,
        vacuna: Vacuna,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES")
    ) {
        programar(
            programador = programador,
            id = vacuna.id ?: return,
            fechaProximaDosis = vacuna.fechaProximaDosis,
            diasAviso = DIAS_AVISO_VACUNA,
            titulo = getString(Res.string.aviso_vacuna_titulo),
            mensaje = getString(
                Res.string.aviso_vacuna_mensaje,
                vacuna.nombre,
                localizacion.formatearFecha(vacuna.fechaProximaDosis)
            )
        )
    }

    /** Programa el aviso de la próxima dosis de un preventivo. */
    suspend fun programarPreventivo(
        programador: ProgramadorNotificaciones,
        preventivo: Preventivo,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES")
    ) {
        val id = preventivo.id ?: return
        programar(
            programador = programador,
            id = id + DESPLAZAMIENTO_PREVENTIVO,
            fechaProximaDosis = preventivo.fechaProximaDosis,
            diasAviso = DIAS_AVISO_PREVENTIVO,
            titulo = getString(Res.string.aviso_preventivo_titulo),
            mensaje = getString(
                Res.string.aviso_preventivo_mensaje,
                preventivo.nombre,
                localizacion.formatearFecha(preventivo.fechaProximaDosis)
            )
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

    /**
     * Programa el recordatorio diario de un tratamiento.
     *
     * A diferencia de las dosis, que avisan **una vez** con días de antelación, un tratamiento se
     * recuerda **cada día a su hora** mientras dure: es una medicina, y el aviso sirve para el día
     * en que toca, no para prepararse. Por eso la alarma se vuelve a programar sola al saltar
     * (ver `NotificacionReceiver`), y aquí solo se calcula la primera.
     *
     * No programa nada si el tratamiento no pide aviso, si no tiene hora o si ya ha terminado:
     * la comprobación está en `RecordatoriosTratamiento`, que tiene sus propios tests.
     */
    suspend fun programarTratamiento(
        programador: ProgramadorNotificaciones,
        tratamiento: Tratamiento,
        nombreMascota: String,
        ahora: LocalDateTime = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
    ) {
        val id = tratamiento.id ?: return
        val (hora, minuto) = RecordatoriosTratamiento.leerHora(tratamiento.hora) ?: return
        val proximo = RecordatoriosTratamiento.proximoAviso(tratamiento, ahora) ?: return
        val millis = proximo.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        // El medicamento y su dosis en una sola línea: "Oticoear · 4 gotas".
        val medicamento = listOfNotNull(
            tratamiento.nombreMedicamento,
            tratamiento.dosis?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        try {
            programador.programarTratamiento(
                id = RecordatoriosTratamiento.idNotificacion(id),
                titulo = getString(Res.string.aviso_tratamiento_titulo),
                mensaje = getString(Res.string.aviso_tratamiento_mensaje, nombreMascota, medicamento),
                fechaDisparoMillis = millis,
                hora = RecordatoriosTratamiento.formatearHora(hora, minuto),
                fechaFin = tratamiento.fechaFin?.toString()
            )
        } catch (e: Exception) {
            println("Error programando el recordatorio del tratamiento: ${e.message}")
        }
    }

    /** Cancela el recordatorio de un tratamiento. */
    fun cancelarTratamiento(programador: ProgramadorNotificaciones, id: Long) {
        programador.cancelar(RecordatoriosTratamiento.idNotificacion(id))
    }

    private fun programar(
        programador: ProgramadorNotificaciones,
        id: Long,
        fechaProximaDosis: LocalDate,
        diasAviso: Int,
        titulo: String,
        mensaje: String
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
                    mensaje = mensaje,
                    fechaDisparoMillis = millis
                )
            }
        } catch (e: Exception) {
            println("Error programando notificación: ${e.message}")
        }
    }
}
