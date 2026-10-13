package com.example.virtualpetkmp.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reglas del eje vertical de los gráficos de peso.
 *
 * Es la parte que provocaba el fallo que reportó el usuario: la línea del peso ideal se veía
 * pegada a un borde (o directamente fuera de la escala) cuando el ideal quedaba por encima del
 * pesaje más alto o por debajo del más bajo. Aquí se fija que el rango **incluye el ideal** y
 * que los extremos llevan margen, así que ningún trazo puede tocar el borde.
 */
class EscalaPesoTest {

    @Test
    fun `el rango incluye el peso ideal aunque quede dentro de los datos`() {
        // El caso que puso el usuario: 1,50 a 1,80 con ideal 1,60. El ideal tiene que caer
        // dentro del eje y por encima del mínimo.
        val escala = escalaDePeso(
            pesosKilos = listOf(1.50, 1.80, 1.65),
            pesoIdealKilos = 1.60,
            unidad = UnidadPeso.KILOS
        )

        // El extremo bajo es el del dato (1,50) menos el margen, no el del ideal.
        assertEquals(1.50 - MARGEN_ESCALA_KILOS, escala.min, 0.000001)
        assertEquals(1.80 + MARGEN_ESCALA_KILOS, escala.max, 0.000001)

        val posicionIdeal = escala.normalizarKilos(1.60)
        assertTrue(posicionIdeal > 0f && posicionIdeal < 1f, "el ideal quedó en un borde: $posicionIdeal")
        // 1,60 está a un tercio del recorrido entre 1,50 y 1,80.
        assertTrue(posicionIdeal < 0.5f, "el ideal debería quedar por debajo de la mitad: $posicionIdeal")
    }

    @Test
    fun `un ideal por encima de todos los pesajes entra en el eje`() {
        // Antes esto dejaba la línea del ideal fuera del gráfico: el eje solo miraba los datos.
        val escala = escalaDePeso(
            pesosKilos = listOf(25.0, 26.0),
            pesoIdealKilos = 30.0,
            unidad = UnidadPeso.KILOS
        )

        assertEquals(30.0 + MARGEN_ESCALA_KILOS, escala.max, 0.000001)
        assertTrue(escala.normalizarKilos(30.0) < 1f, "el ideal tocó el borde de arriba")
        assertTrue(
            escala.normalizarKilos(30.0) > escala.normalizarKilos(26.0),
            "el ideal debe quedar por encima de los datos"
        )
    }

    @Test
    fun `un ideal por debajo de todos los pesajes entra en el eje`() {
        val escala = escalaDePeso(
            pesosKilos = listOf(25.0, 26.0),
            pesoIdealKilos = 18.0,
            unidad = UnidadPeso.KILOS
        )

        assertEquals(18.0 - MARGEN_ESCALA_KILOS, escala.min, 0.000001)
        assertTrue(escala.normalizarKilos(18.0) > 0f, "el ideal tocó el borde de abajo")
        assertTrue(
            escala.normalizarKilos(18.0) < escala.normalizarKilos(25.0),
            "el ideal debe quedar por debajo de los datos"
        )
    }

    @Test
    fun `ningun valor de los datos queda nunca en un borde`() {
        val pesos = listOf(12.0, 15.5, 19.25, 22.0)

        listOf(null, 5.0, 15.0, 40.0).forEach { ideal ->
            val escala = escalaDePeso(pesos, ideal, UnidadPeso.KILOS)
            (pesos + listOfNotNull(ideal)).forEach { valor ->
                val posicion = escala.normalizarKilos(valor)
                assertTrue(
                    posicion > 0f && posicion < 1f,
                    "con ideal=$ideal el valor $valor quedó en el borde ($posicion)"
                )
            }
        }
    }

    @Test
    fun `el margen es medio kilo tambien cuando se muestran libras`() {
        // El margen se define en kilos (la unidad en la que se guarda) y se convierte, así que
        // en libras es algo más de una libra. Lo importante: sigue habiendo aire.
        val escala = escalaDePeso(
            pesosKilos = listOf(10.0),
            pesoIdealKilos = null,
            unidad = UnidadPeso.LIBRAS
        )

        val margenEsperado = UnidadPeso.desdeKilos(MARGEN_ESCALA_KILOS, UnidadPeso.LIBRAS)
        val diezKilosEnLibras = UnidadPeso.desdeKilos(10.0, UnidadPeso.LIBRAS)
        assertEquals(diezKilosEnLibras - margenEsperado, escala.min, 0.000001)
        assertEquals(diezKilosEnLibras + margenEsperado, escala.max, 0.000001)

        // Y `normalizarKilos` recibe kilos aunque el eje esté en libras: la conversión es suya.
        assertEquals(0.5f, escala.normalizarKilos(10.0), 0.0001f)
    }

    @Test
    fun `con un unico valor el punto queda centrado`() {
        // Sin datos de sobra, el margen es lo único que da rango: el punto no puede quedar
        // pegado ni arriba ni abajo.
        val escala = escalaDePeso(listOf(7.0), 7.0, UnidadPeso.KILOS)

        assertEquals(1.0, escala.rango, 0.000001)
        assertEquals(0.5f, escala.normalizarKilos(7.0), 0.0001f)
    }

    @Test
    fun `sin datos ni ideal no hay rango y no se divide entre cero`() {
        val escala = escalaDePeso(emptyList(), null, UnidadPeso.KILOS)

        assertEquals(0.0, escala.rango, 0.000001)
        assertEquals(0.5f, escala.normalizarKilos(12.0))
    }

    @Test
    fun `el peso ideal participa del rango aunque no haya pesajes`() {
        // Se puede fijar el ideal antes de pesarlas: el gráfico no se dibuja, pero la escala
        // no debe quedar degenerada por si algún día se dibuja un punto único.
        val escala = escalaDePeso(emptyList(), 9.0, UnidadPeso.KILOS)

        assertEquals(9.0 - MARGEN_ESCALA_KILOS, escala.min, 0.000001)
        assertEquals(9.0 + MARGEN_ESCALA_KILOS, escala.max, 0.000001)
    }
}
