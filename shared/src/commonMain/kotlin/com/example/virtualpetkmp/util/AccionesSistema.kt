package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que intenta abrir el marcador telefónico con el número dado.
 * Devuelve true si se pudo abrir, false si hubo error.
 */
@Composable
expect fun rememberLlamador(): (telefono: String) -> Boolean

/**
 * Devuelve una función que intenta abrir Google Maps con la búsqueda indicada.
 * El parámetro query puede ser una dirección o una búsqueda como "veterinarios cercanos".
 * Devuelve true si se pudo abrir, false si hubo error.
 */
@Composable
expect fun rememberAbridorMapa(): (query: String) -> Boolean

/**
 * Devuelve un programador de notificaciones que permite programar y cancelar
 * notificaciones locales para recordar fechas futuras.
 */
@Composable
expect fun rememberProgramadorNotificaciones(): ProgramadorNotificaciones

interface ProgramadorNotificaciones {
    /**
     * Programa una notificación que se mostrará en la fecha indicada.
     * @param id Identificador único (usar el id del registro de la BD).
     * @param titulo Título de la notificación.
     * @param mensaje Cuerpo de la notificación.
     * @param fechaDisparoMillis Timestamp en milisegundos desde epoch.
     */
    fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long)

    /**
     * Programa un recordatorio **de tratamiento**, que es un aviso que se repite cada día.
     *
     * Va aparte de [programar] porque Android necesita más datos para repetirlo solo: cuando la
     * alarma salta, la app puede estar cerrada, así que el aviso lleva dentro la hora y la fecha
     * de fin para saber si tiene que programarse otra vez para mañana.
     *
     * @param hora "HH:mm" del aviso, ya normalizada.
     * @param fechaFin último día del tratamiento (ISO), o `null` si es crónico.
     */
    fun programarTratamiento(
        id: Long,
        titulo: String,
        mensaje: String,
        fechaDisparoMillis: Long,
        hora: String,
        fechaFin: String?
    )

    /**
     * Cancela la notificación previamente programada con ese id.
     */
    fun cancelar(id: Long)
}

/**
 * Comprueba si el usuario ha concedido permiso para programar alarmas exactas.
 * En Android 12+ esto requiere el permiso SCHEDULE_EXACT_ALARM.
 * En versiones anteriores y en otras plataformas siempre devuelve true.
 */
@Composable
expect fun rememberComprobadorPermisoExacto(): ComprobadorPermisoExacto

/**
 * Devuelve una función que **pide el permiso de notificaciones** si aún no está concedido.
 *
 * Devuelve `true` si ya se puede notificar en ese mismo momento (permiso concedido, versión de
 * Android que no lo pide, o plataforma sin permisos como el escritorio) y `false` si acaba de
 * lanzar la petición al sistema, en cuyo caso la respuesta llega después.
 *
 * La llama el formulario de tratamientos al encender "Recordarme": si el usuario va a pedir
 * avisos, es el momento de pedir el permiso, no al abrir la app sin venir a cuento.
 */
@Composable
expect fun rememberPermisoNotificaciones(): () -> Boolean

interface ComprobadorPermisoExacto {
    /** True si podemos programar alarmas exactas en este dispositivo. */
    fun puedeProgramarExacto(): Boolean

    /** Abre los ajustes de Android para que el usuario conceda el permiso. */
    fun pedirPermiso()
}

/**
 * Devuelve una función que intenta añadir un evento al calendario del dispositivo.
 * El título y la descripción se pasan ya formateados.
 * La fecha es la del evento (todo el día).
 * Devuelve true si se pudo abrir el calendario, false si hubo error.
 */
@Composable
expect fun rememberAgregadorCalendario(): (titulo: String, descripcion: String, fechaMillis: Long) -> Boolean
