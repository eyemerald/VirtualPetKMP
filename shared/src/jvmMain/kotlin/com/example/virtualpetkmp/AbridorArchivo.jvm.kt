package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

@Composable
actual fun rememberAbridorArchivo(): (String) -> Boolean {
    return { rutaArchivo ->
        try {
            val archivo = File(rutaArchivo)
            if (archivo.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
