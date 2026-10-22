package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
 * Android: carpetas privadas de la app (`filesDir`), las mismas que ya usan la foto de la mascota
 * y los informes adjuntos. `copias` es nueva y solo guarda las copias de seguridad automáticas.
 */
@Composable
actual fun rememberCarpetasApp(): CarpetasApp {
    val context = LocalContext.current
    return remember(context) {
        val base = context.filesDir
        val fotos = File(base, "fotos")
        val informes = File(base, "informes")
        val copias = File(base, "copias")
        listOf(fotos, informes, copias).forEach { it.mkdirs() }
        CarpetasApp(
            fotos = fotos.absolutePath,
            informes = informes.absolutePath,
            copias = copias.absolutePath
        )
    }
}
