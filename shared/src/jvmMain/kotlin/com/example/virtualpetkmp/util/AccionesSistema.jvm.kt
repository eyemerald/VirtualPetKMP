package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
actual fun rememberProgramadorNotificaciones(): ProgramadorNotificaciones {
    return remember {
        object : ProgramadorNotificaciones {
            override fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long) {
                // En JVM/Desktop no hay notificaciones nativas programadas.
                // Simplemente dejamos constancia en consola para debug.
                println("[Notificación programada] id=$id | $titulo | $mensaje | disparo a las $fechaDisparoMillis")
            }

            override fun cancelar(id: Long) {
                println("[Notificación cancelada] id=$id")
            }
        }
    }
}

@Composable
actual fun rememberComprobadorPermisoExacto(): ComprobadorPermisoExacto {
    return remember {
        object : ComprobadorPermisoExacto {
            override fun puedeProgramarExacto(): Boolean = true
            override fun pedirPermiso() { /* no-op en JVM */ }
        }
    }
}

@Composable
actual fun rememberAgregadorCalendario(): (String, String, Long) -> Boolean {
    return { titulo, descripcion, fechaMillis ->
        try {
            if (Desktop.isDesktopSupported()) {
                // Convertir millis a formato YYYYMMDD para el enlace de Google Calendar
                val fecha = java.time.Instant.ofEpochMilli(fechaMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val fechaStr = fecha.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                val fechaFin = fecha.plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))

                val tituloEncoded = java.net.URLEncoder.encode(titulo, "UTF-8")
                val descEncoded = java.net.URLEncoder.encode(descripcion, "UTF-8")

                val url = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                        "&text=$tituloEncoded" +
                        "&dates=$fechaStr/$fechaFin" +
                        "&details=$descEncoded" +
                        "&reminders=1day"

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
