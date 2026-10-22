package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File

/**
 * Escritorio: las carpetas de `~/.virtualpet`, junto a la base de datos, para que todo lo de la
 * app esté en un único sitio y sea fácil de copiar a mano.
 */
@Composable
actual fun rememberCarpetasApp(): CarpetasApp {
    return remember {
        val base = File(System.getProperty("user.home"), ".virtualpet")
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
