package com.example.virtualpetkmp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Lado máximo (en píxeles) de la imagen que se mantiene en memoria. */
private const val LADO_MAXIMO = 1024

actual fun cargarImagenDesdeRuta(ruta: String?): ImageBitmap? {
    if (ruta.isNullOrBlank()) return null
    return try {
        val archivo = File(ruta)
        if (!archivo.exists() || !archivo.isFile) return null

        val bitmap = decodificarReducido(archivo) ?: return null
        val orientado = aplicarOrientacion(bitmap, archivo)
        orientado.asImageBitmap()
    } catch (e: Exception) {
        println("No se pudo cargar la imagen '$ruta': ${e.message}")
        null
    } catch (e: OutOfMemoryError) {
        println("Sin memoria al cargar la imagen '$ruta'")
        null
    }
}

/** Decodifica la imagen reduciéndola a [LADO_MAXIMO] para no agotar la memoria. */
private fun decodificarReducido(archivo: File): Bitmap? {
    val soloLimites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.absolutePath, soloLimites)
    if (soloLimites.outWidth <= 0 || soloLimites.outHeight <= 0) return null

    var escala = 1
    var ancho = soloLimites.outWidth
    var alto = soloLimites.outHeight
    while (ancho / 2 >= LADO_MAXIMO && alto / 2 >= LADO_MAXIMO) {
        ancho /= 2
        alto /= 2
        escala *= 2
    }

    val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
    return BitmapFactory.decodeFile(archivo.absolutePath, opciones)
}

/**
 * Las fotos hechas con la cámara suelen guardar la rotación en el EXIF en lugar de
 * rotar los píxeles. Sin esto, la foto se vería girada en la ficha.
 */
private fun aplicarOrientacion(bitmap: Bitmap, archivo: File): Bitmap {
    val orientacion = try {
        @Suppress("DEPRECATION")
        ExifInterface(archivo.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matriz = Matrix()
    when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matriz.postRotate(90f)
            matriz.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matriz.postRotate(270f)
            matriz.postScale(-1f, 1f)
        }
        else -> return bitmap
    }

    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    } catch (e: Exception) {
        bitmap
    }
}
