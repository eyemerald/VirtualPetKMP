package com.example.virtualpetkmp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

/**
 * Reduce la foto en Android: `inSampleSize` para no cargar 12 MP, rotación EXIF a los píxeles y
 * recompresión.
 *
 * **Por qué `inSampleSize` y no decodificar y escalar.** Una foto de cámara son 12 MP (4000×3000):
 * decodificarla entera son ~48 MB de mapa de bits en memoria y en un móvil modesto eso es un
 * `OutOfMemoryError` a la vuelta de la esquina. Con `inSampleSize` el decodificador lee ya reducido
 * (potencias de 2) y luego se ajusta al tamaño exacto.
 *
 * **Por qué la rotación EXIF se aplica a los píxeles.** Las cámaras guardan la foto "tumbada" y
 * anotan en el EXIF cómo hay que girarla. Si se recodifica sin aplicar esa rotación, la foto se
 * guarda torcida **para siempre** (la orientación del EXIF se pierde al recomprimir). Aquí se gira
 * el bitmap antes de comprimir.
 *
 * Nunca amplía: `calcularDimensionesFoto` devuelve el tamaño original si la foto ya es pequeña.
 */
actual fun reducirFotoDeMascota(ruta: String): Boolean {
    val archivo = File(ruta)
    if (!archivo.exists() || archivo.length() == 0L) return false

    return try {
        // 1. Tamaño real, sin cargar los píxeles.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, limites)
        val destino = calcularDimensionesFoto(limites.outWidth, limites.outHeight)
        if (destino.ancho == 0 || destino.alto == 0) return false

        // Si ya cabe, no se toca el fichero (no tiene sentido recodificar y perder calidad).
        if (destino.ancho == limites.outWidth && destino.alto == limites.outHeight) return true

        // 2. Decodificar con muestra: se busca la mayor potencia de 2 que no baje del tamaño final.
        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calculoMuestra(limites.outWidth, limites.outHeight, destino)
        }
        val original = BitmapFactory.decodeFile(ruta, opciones) ?: return false

        // 3. Rotación EXIF, aplicada a los píxeles antes de recomprimir.
        val grados = rotacionExif(ruta)
        val girado = if (grados == 0) {
            original
        } else {
            val matriz = Matrix().apply { postRotate(grados.toFloat()) }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
        }

        // 4. Escalado al tamaño final, manteniendo la proporción.
        val escalado = if (girado.width != destino.ancho || girado.height != destino.alto) {
            Bitmap.createScaledBitmap(girado, destino.ancho, destino.alto, true)
        } else {
            girado
        }

        // 5. Recomprimir. PNG conserva la transparencia; JPEG es lo normal en fotos.
        val conTransparencia = archivo.extension.lowercase() == "png" || escalado.hasAlpha()
        val formato = if (conTransparencia) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        val temporal = File(archivo.parentFile, "${archivo.name}.reducida")
        temporal.outputStream().use { salida ->
            escalado.compress(formato, CALIDAD_JPEG, salida)
        }

        // Se cierra todo antes de mover el fichero (en Android, un descriptor abierto lo impide).
        original.recycle()
        if (girado !== original) girado.recycle()
        if (escalado !== girado) escalado.recycle()

        if (temporal.length() == 0L) {
            temporal.delete()
            return false
        }
        archivo.delete()
        temporal.renameTo(archivo)
    } catch (e: Exception) {
        println("[VirtualPet] no se pudo reducir la foto ($ruta): ${e.message}")
        false
    }
}

/** Calidad de recompresión: 82 es el punto en el que no se nota el JPEG y baja mucho el tamaño. */
private const val CALIDAD_JPEG = 82

/**
 * `inSampleSize`: la mayor potencia de 2 que aún no deja la imagen por debajo del tamaño final.
 * Con 4000×3000 y destino 336, sale 8 (se decodifica a 500×375 y luego se ajusta a 336×252).
 */
private fun calculoMuestra(ancho: Int, alto: Int, destino: DimensionesFoto): Int {
    var muestra = 1
    while (ancho / (muestra * 2) >= destino.ancho && alto / (muestra * 2) >= destino.alto) {
        muestra *= 2
    }
    return muestra
}

/** Grados de rotación que pide el EXIF (0, 90, 180 o 270). */
private fun rotacionExif(ruta: String): Int = try {
    when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
} catch (e: Exception) {
    0
}
