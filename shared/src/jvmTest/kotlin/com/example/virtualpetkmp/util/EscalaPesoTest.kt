package com.example.virtualpetkmp.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reglas del eje vertical de los gráficos de peso.
 *
 * Fija lo que pedía el usuario: el eje **no** se calcula con el mínimo y el máximo pelados,
 * sino que deja un **10 % de margen** por arriba y por abajo para que la línea no choque con
 * los bordes. El margen es relativo a propósito: con una cantidad fija, una mascota pequeña
 * (1,60 a 1,70 kg) quedaba dibujada como una raya aplastada en mitad del gráfico.
 *
 * También se comprueba que el peso ideal entra en el rango: si el ideal queda fuera de los
 * datos, el eje se estira para que la referencia se vea a su altura exacta.
 */
class EscalaPesoTest {

    @Test
    fun `el eje deja un diez por ciento por encima y por debajo`() {
        // El ejemplo exacto del usuario: 1,60 y 1,70 dan 1,59 y 1,71.
        val escala = escalaDePeso(
            pesosKilos = listOf(1.60, 1.70),
            pesoIdealKilos = null,
            unidad = UnidadPeso.KILOS
        )

        assertEquals(1.59, escala.min, 0.000001)
        assertEquals(1.71, escala.max, 0.000001)
    }

    @Test
    fun `ni el peso mas alto ni el mas bajo caen en un borde`() {
        val escala = escalaDePeso(listOf(1.60, 1.70), null, UnidadPeso.KILOS)

        // Con el 10 % de margen a cada lado, el eje total es 1,2 veces el rango de los datos:
        // el pesaje más bajo se queda en el 8,3 % del alto y el más alto en el 91,7 %.
        assertEquals(0.0833f, escala.normalizarKilos(1.60), 0.001f)
        assertEquals(0.9167f, escala.normalizarKilos(1.70), 0.001f)
    }

    @Test
    fun `el margen es proporcional, no una cantidad fija`() {
        // Un perro de 26 kg tiene que ocupar el mismo alto del gráfico que un conejo de 1,6 kg:
        // eso es justo lo que no pasaba con el margen fijo de medio kilo.
        val pequeno = escalaDePeso(listOf(1.60, 1.70), null, UnidadPeso.KILOS)
        val grande = escalaDePeso(listOf(25.0, 26.0), null, UnidadPeso.KILOS)

        // Los dos ejes miden 1,2 veces el recorrido de sus datos.
        assertEquals(1.20, pequeno.rango / 0.10, 0.000001)
        assertEquals(1.20, grande.rango / 1.00, 0.000001)

        // Y el aire de arriba es una décima parte del recorrido, en cada caso el suyo.
        assertEquals(0.01, pequeno.max - 1.70, 0.000001)
        assertEquals(0.10, grande.max - 26.0, 0.000001)

        // La línea ocupa el mismo 83,3 % del alto en las dos.
        val ocupacionPequeno = pequeno.normalizarKilos(1.70) - pequeno.normalizarKilos(1.60)
        val ocupacionGrande = grande.normalizarKilos(26.0) - grande.normalizarKilos(25.0)
        assertEquals(0.8333f, ocupacionPequeno, 0.001f)
        assertEquals(0.8333f, ocupacionGrande, 0.001f)
    }

    @Test
    fun `el rango incluye el peso ideal aunque quede dentro de los datos`() {
        // El caso que puso el usuario en su día: 1,50 a 1,80 con ideal 1,60. El ideal tiene
        // que caer dentro del eje y por encima del mínimo.
        val escala = escalaDePeso(
            pesosKilos = listOf(1.50, 1.80, 1.65),
            pesoIdealKilos = 1.60,
            unidad = UnidadPeso.KILOS
        )

        val posicionIdeal = escala.normalizarKilos(1.60)
        assertTrue(posicionIdeal > 0f && posicionIdeal < 1f, "el ideal quedó en un borde: $posicionIdeal")
        assertTrue(posicionIdeal < 0.5f, "el ideal debería quedar por debajo de la mitad: $posicionIdeal")
    }

    @Test
    fun `un ideal por encima de todos los pesajes entra en el eje`() {
        val escala = escalaDePeso(listOf(25.0, 26.0), 30.0, UnidadPeso.KILOS)

        // El rango son los datos MÁS el ideal (25 a 30), y el 10 % se calcula sobre ese conjunto.
        assertEquals(24.5, escala.min, 0.000001)
        assertEquals(30.5, escala.max, 0.000001)
        assertTrue(escala.normalizarKilos(30.0) < 1f, "el ideal tocó el borde de arriba")
        assertTrue(
            escala.normalizarKilos(30.0) > escala.normalizarKilos(26.0),
            "el ideal debe quedar por encima de los datos"
        )
    }

    @Test
    fun `un ideal por debajo de todos los pesajes entra en el eje`() {
        val escala = escalaDePeso(listOf(25.0, 26.0), 18.0, UnidadPeso.KILOS)

        // De 18 a 26, con el 10 % (0,8) a cada lado.
        assertEquals(17.2, escala.min, 0.000001)
        assertEquals(26.8, escala.max, 0.000001)
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
    fun `el margen se calcula en la unidad que se muestra`() {
        // En libras el margen también es el 10 % del rango (2 kg son 4,41 lb y el 10 % son
        // 0,44 lb), así que la proporción del gráfico es la misma que en kilos.
        val librasPorKilo = UnidadPeso.LIBRAS_POR_KILO
        val enKilos = escalaDePeso(listOf(10.0, 12.0), null, UnidadPeso.KILOS)
        val enLibras = escalaDePeso(listOf(10.0, 12.0), null, UnidadPeso.LIBRAS)

        assertEquals(2.4, enKilos.rango, 0.000001)
        assertEquals(2.4 * librasPorKilo, enLibras.rango, 0.000001)

        // Y `normalizarKilos` recibe kilos aunque el eje esté en libras: la conversión es suya.
        assertEquals(enKilos.normalizarKilos(11.0), enLibras.normalizarKilos(11.0), 0.0001f)
    }

    @Test
    fun `con todos los pesos iguales la linea queda centrada`() {
        // El 10 % de cero no es nada, así que aquí sí hace falta una cantidad fija: si no, el
        // rango sería cero y la línea se pegaría a un borde (o se dividiría entre cero).
        val escala = escalaDePeso(listOf(7.0, 7.0, 7.0), null, UnidadPeso.KILOS)

        assertEquals(2 * MARGEN_ESCALA_PLANO_KILOS, escala.rango, 0.000001)
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

        assertEquals(9.0 - MARGEN_ESCALA_PLANO_KILOS, escala.min, 0.000001)
        assertEquals(9.0 + MARGEN_ESCALA_PLANO_KILOS, escala.max, 0.000001)
    }
}
