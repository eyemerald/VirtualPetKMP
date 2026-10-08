package com.example.virtualpetkmp.util

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Comprueba que el cargador de fotos de la ficha no se rompe con archivos
 * que no son imágenes (fotos borradas o corruptas) y que sí carga una imagen real.
 */
class ImagenMascotaTest {

    private fun crearJpegDePrueba(ancho: Int, alto: Int): File {
        val info = ImageInfo(ancho, alto, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
        val bitmap = Bitmap()
        bitmap.allocPixels(info)
        val bytes = requireNotNull(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.JPEG, 90))
            .bytes
        val archivo = File.createTempFile("foto_mascota_", ".jpg")
        archivo.deleteOnExit()
        archivo.writeBytes(bytes)
        return archivo
    }

    @Test
    fun `carga una foto grande sin agotar la memoria`() {
        // Una foto de móvil típica: si no se redujera, ocuparía ~72 MB en memoria.
        val archivo = crearJpegDePrueba(4000, 3000)

        val imagen = cargarImagenDesdeRuta(archivo.absolutePath)
        assertNotNull(imagen)
        assertEquals(4000, imagen.width)
        assertEquals(3000, imagen.height)

        archivo.delete()
    }

    @Test
    fun `devuelve null con ruta nula, inexistente o archivo que no es imagen`() {
        assertNull(cargarImagenDesdeRuta(null))
        assertNull(cargarImagenDesdeRuta(""))
        assertNull(cargarImagenDesdeRuta("/ruta/que/no/existe/foto.jpg"))

        val noEsImagen = File.createTempFile("informe_", ".pdf")
        noEsImagen.deleteOnExit()
        noEsImagen.writeText("esto no es una imagen")
        assertNull(cargarImagenDesdeRuta(noEsImagen.absolutePath))
        noEsImagen.delete()
    }
}
