package com.example.virtualpetkmp.util

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El tamaño al que se guarda la foto de una mascota.
 *
 * Lo que se busca: **una sola regla, en una función pura**, para que la cuenta no viva dentro del
 * código de Android y de escritorio por separado (serían dos oportunidades de equivocarse).
 *
 * El lado máximo no es arbitrario: la foto se pinta como mucho a 84 dp y la densidad más alta que
 * usa Android en la práctica es 4,0 → 84 × 4,0 = 336 px.
 */
class RedimensionFotoTest {

    private val ladoMaximo = LADO_MAXIMO_FOTO_MASCOTA

    @Test
    fun `el lado maximo sale de 84 dp por densidad 4`() {
        assertEquals(336, ladoMaximo, "el lado máximo tiene que derivarse de la pantalla, no elegirse a ojo")
        assertEquals(84 * 4, ladoMaximo)
    }

    @Test
    fun `una foto horizontal se reduce manteniendo la proporcion`() {
        // 4000x3000 es una foto de cámara de 12 MP.
        val destino = calcularDimensionesFoto(4000, 3000)

        assertEquals(ladoMaximo, destino.ancho, "el lado mayor tiene que quedar en el máximo")
        assertEquals(252, destino.alto, "3000 * (336/4000) = 252")
        assertEquals(
            4000.0 / 3000.0,
            destino.ancho.toDouble() / destino.alto,
            absoluteTolerance = 0.01,
            message = "la proporción no puede cambiar"
        )
    }

    @Test
    fun `una foto vertical se reduce por el lado mayor`() {
        val destino = calcularDimensionesFoto(3000, 4000)

        assertEquals(252, destino.ancho)
        assertEquals(ladoMaximo, destino.alto, "en una vertical el máximo es el alto")
    }

    @Test
    fun `una foto cuadrada se reduce por el lado mayor`() {
        val destino = calcularDimensionesFoto(2000, 2000)

        assertEquals(ladoMaximo, destino.ancho)
        assertEquals(ladoMaximo, destino.alto)
    }

    @Test
    fun `una foto que ya es pequena no se amplia`() {
        val destino = calcularDimensionesFoto(200, 150)

        assertEquals(200, destino.ancho)
        assertEquals(150, destino.alto)
    }

    @Test
    fun `una foto justo en el limite no se toca`() {
        val destino = calcularDimensionesFoto(ladoMaximo, ladoMaximo - 100)

        assertEquals(ladoMaximo, destino.ancho)
        assertEquals(ladoMaximo - 100, destino.alto)
    }

    @Test
    fun `un tamano invalido no genera imagen`() {
        // Un fichero que no se pudo decodificar llega como 0x0, y a veces con solo un lado a cero.
        assertEquals(DimensionesFoto(0, 0), calcularDimensionesFoto(0, 0))
        assertEquals(DimensionesFoto(0, 0), calcularDimensionesFoto(0, 3000))
        assertEquals(DimensionesFoto(0, 0), calcularDimensionesFoto(3000, 0))
        assertEquals(DimensionesFoto(0, 0), calcularDimensionesFoto(-5, 100))
        assertEquals(DimensionesFoto(0, 0), calcularDimensionesFoto(1000, 1000, ladoMaximo = 0))
    }

    @Test
    fun `el redondeo nunca deja un lado a cero`() {
        // 5000x1 recortado a 336 dejaría el alto en 0,07 px: tiene que quedar al menos 1.
        val destino = calcularDimensionesFoto(5000, 1)

        assertEquals(ladoMaximo, destino.ancho)
        assertEquals(1, destino.alto)
    }

    // --- El camino real de escritorio: reducir un fichero de verdad ---------------------------

    private lateinit var carpeta: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-foto").toFile()
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    /** Crea una foto JPEG grande con ruido, para que el fichero tenga un tamaño realista. */
    private fun fotoDePrueba(ancho: Int, alto: Int, nombre: String = "foto.jpg"): File {
        val imagen = BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB)
        val azar = java.util.Random(42)
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                imagen.setRGB(x, y, azar.nextInt(0xFFFFFF))
            }
        }
        val archivo = File(carpeta, nombre)
        ImageIO.write(imagen, "jpg", archivo)
        return archivo
    }

    @Test
    fun `reducir una foto grande de verdad baja el tamano y las dimensiones`() {
        val archivo = fotoDePrueba(2000, 1500)
        val antes = archivo.length()

        assertTrue(reducirFotoDeMascota(archivo.absolutePath), "la reducción tiene que funcionar")

        val despues = archivo.length()
        val imagen = ImageIO.read(archivo)
        assertEquals(ladoMaximo, imagen.width)
        assertEquals(252, imagen.height)
        assertTrue(despues < antes, "el fichero tiene que pesar menos ($antes -> $despues)")
    }

    @Test
    fun `reducir una foto pequena no la toca`() {
        val archivo = fotoDePrueba(200, 150, "pequena.jpg")
        val antes = archivo.length()

        reducirFotoDeMascota(archivo.absolutePath)

        assertTrue(archivo.exists(), "el fichero no puede desaparecer")
        assertEquals(antes, archivo.length(), "no se recodifica si ya cabe")
    }

    @Test
    fun `un png con transparencia sigue siendo png y no se rellena de negro`() {
        val imagen = BufferedImage(2000, 1500, BufferedImage.TYPE_INT_ARGB)
        val archivo = File(carpeta, "transparente.png")
        ImageIO.write(imagen, "png", archivo)

        assertTrue(reducirFotoDeMascota(archivo.absolutePath))

        val reducida = ImageIO.read(archivo)
        assertEquals(ladoMaximo, reducida.width)
        assertTrue(reducida.colorModel.hasAlpha(), "se perdió la transparencia")
        // Esquina transparente: si se hubiera convertido a JPEG estaría opaca.
        assertEquals(0, reducida.getRGB(0, 0) ushr 24, "la zona transparente se rellenó de color")
    }

    @Test
    fun `un fichero que no es una imagen no rompe nada`() {
        val archivo = File(carpeta, "no-es-imagen.jpg")
        archivo.writeText("esto no es un jpg")

        assertEquals(false, reducirFotoDeMascota(archivo.absolutePath), "tiene que devolver false, no lanzar")
        assertTrue(archivo.exists())
    }
}
