package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberLlamador(): (String) -> Boolean {
    return { telefono ->
        try {
            if (Desktop.isDesktopSupported()) {
                // En desktop no hay teléfono, pero mostramos el número con una llamada simulada
                // usando el navegador por defecto con un "tel:" URI.
                Desktop.getDesktop().browse(URI("tel:${telefono.filter { it.isDigit() || it == '+' }}"))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
actual fun rememberAbridorMapa(): (String) -> Boolean {
    return { query ->
        try {
            if (Desktop.isDesktopSupported()) {
                val url = "https://www.google.com/maps/search/${java.net.URLEncoder.encode(query, "UTF-8")}"
                Desktop.getDesktop().browse(URI(url))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
