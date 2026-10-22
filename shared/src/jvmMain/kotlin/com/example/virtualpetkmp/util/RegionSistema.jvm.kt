package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.util.Locale

/**
 * Escritorio: la configuración regional es la del sistema operativo, y se lee una vez por
 * composición desde la locale por defecto de la JVM.
 */
@Composable
actual fun etiquetaRegionalDelSistema(): String? {
    val locale = Locale.getDefault()
    val idioma = locale.language?.takeIf { it.isNotBlank() } ?: return null
    val pais = locale.country?.takeIf { it.isNotBlank() }
    return if (pais == null) idioma else "$idioma-$pais"
}

/** En escritorio no hay diferencia entre dentro y fuera de la composición: es la locale de la JVM. */
actual fun etiquetaRegionalFueraDeComposicion(): String? =
    etiquetaRegionalDelSistemaSinComposicion()

private fun etiquetaRegionalDelSistemaSinComposicion(): String? {
    val locale = Locale.getDefault()
    val idioma = locale.language?.takeIf { it.isNotBlank() } ?: return null
    val pais = locale.country?.takeIf { it.isNotBlank() }
    return if (pais == null) idioma else "$idioma-$pais"
}
