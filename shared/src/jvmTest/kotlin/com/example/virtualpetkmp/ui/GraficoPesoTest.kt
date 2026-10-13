package com.example.virtualpetkmp.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reparto horizontal de los puntos del gráfico de peso.
 *
 * Existe por un fallo real: el sparkline de la ficha separaba los puntos a un sexto del ancho y
 * después "recortaba" por la izquierda lo que sobraba. Con siete pesajes no se notaba, pero a
 * partir de ocho el recorte se comía los primeros puntos y la línea arrancaba fuera de la
 * pantalla. Por eso lo que se comprueba aquí no es un detalle de estilo: es que ningún punto
 * caiga fuera del área de dibujo, con cualquier número de pesajes.
 */
class GraficoPesoTest {

    private val inicio = 16f
    private val ancho = 250f

    @Test
    fun `el primer punto cae en el margen izquierdo y el ultimo en el borde del area`() {
        val x = posicionesX(cantidad = 7, inicio = inicio, ancho = ancho)

        assertEquals(inicio, x.first(), 0.001f)
        assertEquals(inicio + ancho, x.last(), 0.001f)
    }

    @Test
    fun `ningun punto se sale del area de dibujo, sea cual sea el numero de pesajes`() {
        // El rango incluye justo los tamaños que rompían el reparto anterior (8 o más).
        (2..20).forEach { cantidad ->
            val x = posicionesX(cantidad, inicio, ancho)

            assertEquals(cantidad, x.size)
            x.forEachIndexed { indice, posicion ->
                assertTrue(
                    posicion >= inicio && posicion <= inicio + ancho,
                    "con $cantidad puntos, el punto $indice se sale: $posicion"
                )
            }
        }
    }

    @Test
    fun `los puntos van en orden y repartidos por igual`() {
        val x = posicionesX(cantidad = 5, inicio = inicio, ancho = ancho)
        val paso = ancho / 4f

        x.zipWithNext { anterior, siguiente ->
            assertEquals(paso, siguiente - anterior, 0.001f)
            assertTrue(siguiente > anterior, "los puntos deben ir de izquierda a derecha")
        }
    }

    @Test
    fun `con un solo punto se centra y sin puntos no hay nada`() {
        assertEquals(listOf(inicio + ancho / 2f), posicionesX(1, inicio, ancho))
        assertTrue(posicionesX(0, inicio, ancho).isEmpty())
    }
}
