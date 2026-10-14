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
 * La granularidad ya **no** se deduce de los datos: la decide el filtro (ver `presentacionDe`, y
 * sus tests en `FiltrosPesoTest`). Aquí se comprueba que cada granularidad construye bien su
 * serie, que es lo que puede romperse en silencio: una media mal calculada o un hueco que no se
 * intercala dan un gráfico que miente sin dar ningún error.
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
    fun `un punto por pesaje, en orden de fecha`() {
        val delMes = listOf(
            peso("2026-10-27", 25.0),
            peso("2026-10-02", 24.8),
            peso("2026-10-13", 25.1)
        )

        val serie = serieDePeso(delMes, GranularidadPeso.POR_PESAJES)

        assertEquals(3, serie.size)
        assertEquals(listOf(24.8, 25.1, 25.0), serie.map { it.media })
        assertEquals(
            listOf("2026-10-02", "2026-10-13", "2026-10-27"),
            serie.map { it.fecha.toString() }
        )
    }

    @Test
    fun `un solo pesaje se dibuja como un punto`() {
        val serie = serieDePeso(listOf(peso("2026-10-13", 25.0)), GranularidadPeso.POR_PESAJES)

        assertEquals(1, serie.size)
        assertEquals(25.0, serie.first().media)
    }

    @Test
    fun `la serie mensual es la media de cada mes`() {
        val variosMeses = listOf(
            peso("2026-08-15", 24.0),
            peso("2026-10-02", 24.8),
            peso("2026-10-13", 25.1)
        )

        val serie = serieDePeso(variosMeses, GranularidadPeso.MENSUAL)

        // Agosto, septiembre (sin datos) y octubre: el hueco también ocupa su sitio.
        assertEquals(3, serie.size)
        assertEquals(listOf<Double?>(24.0, null, (24.8 + 25.1) / 2), serie.map { it.media })
        assertEquals(
            listOf("2026-08-01", "2026-09-01", "2026-10-01"),
            serie.map { it.fecha.toString() }
        )
    }

    @Test
    fun `el mismo mes de otro ano son dos puntos distintos`() {
        val dosOctubres = listOf(
            peso("2025-10-05", 20.0),
            peso("2026-10-13", 25.1)
        )

        val serie = serieDePeso(dosOctubres, GranularidadPeso.MENSUAL)

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
            ),
            GranularidadPeso.MENSUAL
        )

        assertEquals(4, serie.size)
        assertEquals(listOf(30.0, null, null, 32.0), serie.map { it.media })
        assertTrue(serie.all { it.fecha.dayOfMonth == 1 }, "los puntos mensuales van fechados a día 1")
    }

    @Test
    fun `las dos series salen ordenadas cronologicamente`() {
        val desordenado = listOf(
            peso("2026-10-27", 25.0),
            peso("2025-12-01", 19.0),
            peso("2026-03-05", 22.0)
        )

        GranularidadPeso.entries.forEach { granularidad ->
            val serie = serieDePeso(desordenado, granularidad)

            assertEquals(
                serie.map { it.fecha }.sorted(),
                serie.map { it.fecha },
                "la serie $granularidad debe ir de antiguo a reciente"
            )
        }
    }

    @Test
    fun `sin pesajes no hay serie`() {
        GranularidadPeso.entries.forEach { granularidad ->
            assertTrue(serieDePeso(emptyList(), granularidad).isEmpty())
        }
    }
}
