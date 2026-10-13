package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Construcción de la serie que dibuja el gráfico de peso.
 *
 * El caso que motivó estos tests: con el filtro "Mes actual", todos los pesajes caen dentro del
 * mismo mes natural. Agrupando por mes, la serie quedaba con **un solo punto** y el gráfico se
 * veía como un punto suelto sin línea, con aspecto de estar roto. La regla es: si todo cae en el
 * mismo mes, un punto por pesaje.
 */
class SeriesPesoTest {

    private fun peso(fecha: String, kilos: Double) = Peso(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse(fecha),
        peso = kilos,
        notas = null
    )

    @Test
    fun `todos los pesajes en el mismo mes se dibujan uno a uno`() {
        val delMes = listOf(
            peso("2026-10-02", 24.8),
            peso("2026-10-13", 25.1),
            peso("2026-10-27", 25.0)
        )

        assertEquals(GranularidadPeso.POR_PESAJES, granularidadDe(delMes))

        val serie = serieDePeso(delMes)
        assertEquals(3, serie.size)
        assertEquals(listOf(24.8, 25.1, 25.0), serie.map { it.media })
        assertEquals(
            listOf("2026-10-02", "2026-10-13", "2026-10-27"),
            serie.map { it.fecha.toString() }
        )
    }

    @Test
    fun `un solo pesaje no permite agrupar por mes y se dibuja el punto`() {
        // Es el caso de una mascota recién estrenada, o del filtro de mes con un único pesaje.
        val serie = serieDePeso(listOf(peso("2026-10-13", 25.0)))

        assertEquals(GranularidadPeso.POR_PESAJES, granularidadDe(listOf(peso("2026-10-13", 25.0))))
        assertEquals(1, serie.size)
        assertEquals(25.0, serie.first().media)
    }

    @Test
    fun `si el rango cruza de mes se agrupa por meses`() {
        val variosMeses = listOf(
            peso("2026-08-15", 24.0),
            peso("2026-10-02", 24.8),
            peso("2026-10-13", 25.1)
        )

        assertEquals(GranularidadPeso.MENSUAL, granularidadDe(variosMeses))

        val serie = serieDePeso(variosMeses)
        // Agosto, septiembre (sin datos) y octubre: el hueco también ocupa su sitio.
        assertEquals(3, serie.size)
        assertEquals(listOf<Double?>(24.0, null, (24.8 + 25.1) / 2), serie.map { it.media })
        assertEquals(
            listOf("2026-08-01", "2026-09-01", "2026-10-01"),
            serie.map { it.fecha.toString() }
        )
    }

    @Test
    fun `el mismo mes de otro ano NO cuenta como el mismo mes`() {
        // Octubre de 2025 y octubre de 2026 son dos puntos distintos, no una media conjunta.
        val dosOctubres = listOf(
            peso("2025-10-05", 20.0),
            peso("2026-10-13", 25.1)
        )

        assertEquals(GranularidadPeso.MENSUAL, granularidadDe(dosOctubres))

        val serie = serieDePeso(dosOctubres)
        assertEquals(13, serie.size, "de octubre de 2025 a octubre de 2026 hay 13 meses")
        assertEquals(20.0, serie.first().media)
        assertEquals(25.1, serie.last().media)
        assertNull(serie[1].media, "los meses intermedios van como huecos")
    }

    @Test
    fun `la serie mensual rellena los meses sin pesajes`() {
        val serie = serieDePeso(
            listOf(
                peso("2026-01-10", 30.0),
                peso("2026-04-20", 32.0)
            )
        )

        assertEquals(4, serie.size)
        assertEquals(listOf(30.0, null, null, 32.0), serie.map { it.media })
        assertTrue(serie.all { it.fecha.dayOfMonth == 1 }, "los puntos mensuales van fechados a día 1")
    }

    @Test
    fun `la serie sale ordenada cronologicamente aunque la entrada venga desordenada`() {
        val desordenado = listOf(
            peso("2026-10-27", 25.0),
            peso("2025-12-01", 19.0),
            peso("2026-03-05", 22.0)
        )

        val serie = serieDePeso(desordenado)

        assertEquals(serie.map { it.fecha }.sorted(), serie.map { it.fecha })
    }

    @Test
    fun `sin pesajes no hay serie`() {
        assertEquals(GranularidadPeso.MENSUAL, granularidadDe(emptyList()))
        assertTrue(serieDePeso(emptyList()).isEmpty())
    }
}
