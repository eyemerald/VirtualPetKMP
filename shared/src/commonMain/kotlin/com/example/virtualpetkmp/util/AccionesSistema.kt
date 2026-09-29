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

interface ComprobadorPermisoExacto {
    /** True si podemos programar alarmas exactas en este dispositivo. */
    fun puedeProgramarExacto(): Boolean

    /** Abre los ajustes de Android para que el usuario conceda el permiso. */
    fun pedirPermiso()
}
