package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate

/**
 * La regla de "esta vacuna o este preventivo está pendiente".
 *
 * **Pendiente = la próxima dosis ya pasó o cae dentro de [VENTANA_DIAS] días.** Es una sola regla
 * para los dos filtros de la lista, y vive aquí y no suelta en el SQL para que se pueda leer y
 * cambiar en un sitio: el SQL solo recibe la fecha límite ya calculada.
 *
 * La ventana es más ancha que los avisos a propósito (los avisos de vacuna saltan 15 días antes y
 * los de preventivo 1 día antes): el filtro sirve para *revisar*, no para avisar, así que incluye
 * lo que hay que mirar con tiempo.
 */
object Pendientes {

    /** Días de antelación con los que una dosis ya se considera "pendiente". */
    const val VENTANA_DIAS = 30

    /** Última fecha que cuenta como pendiente: hoy + [VENTANA_DIAS]. */
    fun limite(hoy: LocalDate): LocalDate =
        LocalDate.fromEpochDays(hoy.toEpochDays() + VENTANA_DIAS)

    /**
     * Si una próxima dosis está pendiente a día de hoy.
     *
     * Es la misma condición que aplica el SQL (`fechaProximaDosis <= límite`), escrita también en
     * Kotlin para poder probar la regla sin base de datos.
     */
    fun estaPendiente(fechaProximaDosis: LocalDate, hoy: LocalDate): Boolean =
        fechaProximaDosis <= limite(hoy)
}
