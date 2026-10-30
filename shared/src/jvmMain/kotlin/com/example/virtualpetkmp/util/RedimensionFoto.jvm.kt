package com.example.virtualpetkmp.util

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Reduce la foto en escritorio con `ImageIO` y un escalado de calidad.
 *
 * Se usa `Graphics2D` con interpolación bilineal en vez de `getScaledInstance`: este último es
 * lento y deja bordes con dientes en fotos grandes. El escalado se hace en un paso (de 4000 px a
 * 336 px directamente), que para estas reducciones da mejor resultado que encadenar pasos.
 *
 * **Transparencia.** Si la imagen tiene canal alfa (un PNG con fondo transparente), se guarda como
 * **PNG** en vez de JPEG: convertirla a JPEG la dejaría con fondo negro, que es peor que ocupar unos
 * KB más. Las fotos normales (JPEG, sin alfa) se recomprimen a JPEG con calidad 82.
 *
 * Nunca amplía y no toca el fichero si ya cabe (`calcularDimensionesFoto`).
 */
actual fun reducirFotoDeMascota(ruta: String): Boolean {
    val archivo = File(ruta)
    if (!archivo.exists() || archivo.length() == 0L) return false

    return try {
        val original = ImageIO.read(archivo) ?: return false
        val destino = calcularDimensionesFoto(original.width, original.height)
        if (destino.ancho == 0 || destino.alto == 0) return false

        // Si ya cabe, no se recodifica: recomprimir sin necesidad solo quita calidad.
        if (destino.ancho == original.width && destino.alto == original.height) return true

        val conTransparencia = original.colorModel.hasAlpha()
        val tipo = if (conTransparencia) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val reducido = BufferedImage(destino.ancho, destino.alto, tipo)
        val grafico = reducido.createGraphics()
        try {
            grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            grafico.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            grafico.drawImage(original, 0, 0, destino.ancho, destino.alto, null)
        } finally {
            grafico.dispose()
        }

        val temporal = File(archivo.parentFile, "${archivo.name}.reducida")
        if (conTransparencia) {
            ImageIO.write(reducido, "png", temporal)
        } else {
            escribirJpeg(reducido, temporal)
        }

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
private const val CALIDAD_JPEG = 0.82f

/** Escribe JPEG con la calidad indicada (ImageIO por defecto usa 0.75 y sin control). */
private fun escribirJpeg(imagen: BufferedImage, destino: File) {
    val escritor = ImageIO.getImageWritersByFormatName("jpeg").next()
    ImageIO.createImageOutputStream(destino).use { salida ->
        escritor.output = salida
        val parametros = escritor.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = CALIDAD_JPEG
        }
        escritor.write(null, IIOImage(imagen, null, null), parametros)
    }
    escritor.dispose()
}
