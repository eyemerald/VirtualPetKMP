package com.example.virtualpetkmp.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reparto horizontal de los puntos y ancho del lienzo del gráfico de peso.
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

    // --- Ancho del lienzo: cuándo hace falta arrastrar -------------------------------------

    @Test
    fun `con pocos puntos el grafico se ajusta a la pantalla y no hay nada que arrastrar`() {
        val pantalla = 350.dp

        // Siete pesajes: entran de sobra con 14 dp de separación.
        assertEquals(pantalla, anchoDelGrafico(pantalla, puntos = 7))
    }

    @Test
    fun `con muchos puntos el lienzo se ensancha para que no se apelotonen`() {
        val pantalla = 350.dp
        val separacion = 14.dp
        val margenes = MARGENES_GRAFICO_PESO

        // 40 pesajes no caben: el lienzo mide lo que piden ellos, no la pantalla.
        val ancho = anchoDelGrafico(pantalla, puntos = 40)

        assertEquals(separacion * 39 + margenes.izquierda + margenes.derecha, ancho)
        assertTrue(ancho > pantalla, "con 40 puntos debería haber desplazamiento: $ancho")
    }

    @Test
    fun `el lienzo nunca es mas estrecho que la pantalla`() {
        val pantalla = 350.dp

        // Ni con un punto, ni con ninguno: si midiera solo lo que piden los puntos, el gráfico
        // saldría diminuto en una esquina.
        listOf(0, 1, 2, 5).forEach { puntos ->
            assertEquals(
                pantalla,
                anchoDelGrafico(pantalla, puntos),
                "con $puntos puntos el lienzo debería ocupar la pantalla"
            )
        }
    }

    @Test
    fun `el cambio de ajustarse a arrastrarse ocurre justo cuando deja de caber`() {
        val pantalla = 350.dp
        val margenes = MARGENES_GRAFICO_PESO
        // Puntos que caben exactamente: (pantalla - margenes) / separacion + 1.
        val cabenJusto = ((pantalla - margenes.izquierda - margenes.derecha) / SEPARACION_MINIMA_PUNTO)
            .toInt() + 1

        assertEquals(pantalla, anchoDelGrafico(pantalla, cabenJusto))
        assertTrue(
            anchoDelGrafico(pantalla, cabenJusto + 1) > pantalla,
            "un punto más ya no cabe y debería aparecer el desplazamiento"
        )
    }
}
