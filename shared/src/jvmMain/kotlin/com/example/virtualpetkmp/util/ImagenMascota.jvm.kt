package com.example.virtualpetkmp.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File

actual fun cargarImagenDesdeRuta(ruta: String?): ImageBitmap? {
    if (ruta.isNullOrBlank()) return null
    return try {
        val archivo = File(ruta)
        if (!archivo.exists() || !archivo.isFile) return null

        val bytes = archivo.readBytes()
        if (bytes.isEmpty()) return null

        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        println("No se pudo cargar la imagen '$ruta': ${e.message}")
        null
    }
}
