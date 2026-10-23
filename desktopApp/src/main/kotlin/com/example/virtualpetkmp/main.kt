package com.example.virtualpetkmp

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.virtualpetkmp.data.DatabaseFactory
import javax.imageio.ImageIO

/**
 * Punto de entrada de la app de escritorio.
 *
 * El icono de la ventana (y el de la barra de tareas) se pone aquí: sin esto Windows enseña el
 * café de Java en lugar del logo. El del `.exe` y el del acceso directo los pone el instalador
 * (`icons/app.ico`), que es un fichero aparte porque Windows los quiere en otro formato.
 *
 * Se carga el PNG de `resources` en vez de usar los recursos de Compose del módulo compartido
 * porque esos son `internal` de `shared` y no se pueden ver desde aquí.
 *
 * El tamaño inicial imita la ventana de un móvil, que es para lo que está pensada la interfaz.
 */
fun main() = application {
    val databaseFactory = DatabaseFactory()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Virtual Pet",
        icon = cargarIcono()?.let { BitmapPainter(it) },
        state = rememberWindowState(size = DpSize(430.dp, 900.dp))
    ) {
        App(databaseFactory)
    }
}

/** Icono de la ventana, o `null` si el recurso no está (la app funciona igual, sin icono propio). */
private fun cargarIcono() = try {
    val flujo = object {}.javaClass.getResourceAsStream("/logo.png")
    flujo?.use { ImageIO.read(it) }?.toComposeImageBitmap()
} catch (e: Exception) {
    println("[main] no se pudo cargar el icono de la ventana: ${e.message}")
    null
}
