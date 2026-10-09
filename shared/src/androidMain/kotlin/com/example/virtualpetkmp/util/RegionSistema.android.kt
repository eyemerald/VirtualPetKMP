package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Android: la configuración regional vive en los recursos, así que se lee de la configuración
 * actual. Cambia sola si el usuario cambia el idioma del sistema con la app abierta, porque
 * Android recrea la actividad.
 */
@Composable
actual fun etiquetaRegionalDelSistema(): String? {
    val configuracion = LocalConfiguration.current
    val locales = configuracion.locales
    val locale = if (locales.isEmpty) configuracion.locale else locales[0]
    return locale?.toString()
}
