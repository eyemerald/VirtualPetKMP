package com.example.virtualpetkmp.util

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

/**
 * Carga una imagen desde disco y la convierte en [ImageBitmap] lista para pintar.
 *
 * Devuelve `null` si la ruta es nula, el archivo no existe o no se puede decodificar
 * (por ejemplo si el archivo se borró o el formato no está soportado). Nunca lanza.
 * Se ejecuta en el hilo que la llame: conviene invocarla desde un `LaunchedEffect`.
 */
expect fun cargarImagenDesdeRuta(ruta: String?): ImageBitmap?

/** `true` si la ruta apunta a un archivo existente y legible. */
fun existeFoto(ruta: String?): Boolean {
    if (ruta.isNullOrBlank()) return false
    return try {
        File(ruta).let { it.exists() && it.isFile && it.length() > 0L }
    } catch (_: Exception) {
        false
    }
}
