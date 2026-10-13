package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Filtros del gráfico de peso: todo, año, **mes actual** y rango de fechas.
 *
 * El de "mes actual" es el que más fácil se rompe sin que nadie se entere: si se comparara solo
 * el mes sin el año, octubre de este año arrastraría también el octubre del anterior, y el
 * gráfico enseñaría pesajes de hace un año como si fueran de este mes.
 */
class FiltrosPesoTest {

    /** Hoy, fijo: así el test del mes actual no depende de cuándo se ejecute. */
    private val hoy = LocalDate.parse("2026-10-13")

    private fun peso(fecha: String, kilos: Double) = Peso(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse(fecha),
        peso = kilos,
        notas = null
    )

    private val historial = listOf(
        peso("2025-10-05", 20.0),
        peso("2026-01-20", 21.5),
        peso("2026-09-30", 24.0),
        peso("2026-10-02", 24.8),
        peso("2026-10-13", 25.1),
        peso("2026-11-01", 25.4)
    )

    private fun fechas(resultado: List<Peso>) = resultado.map { it.fecha.toString() }

    @Test
    fun `todo devuelve el historial entero, de mas antiguo a mas reciente`() {
        val resultado = filtrarPesos(historial, FiltroPeso.TODO, hoy)

        assertEquals(
            listOf("2025-10-05", "2026-01-20", "2026-09-30", "2026-10-02", "2026-10-13", "2026-11-01"),
            fechas(resultado)
        )
    }

    @Test
    fun `el mes actual trae solo los pesajes de este mes`() {
        val resultado = filtrarPesos(historial, FiltroPeso.MES, hoy)

        assertEquals(listOf("2026-10-02", "2026-10-13"), fechas(resultado))
    }

    @Test
    fun `el mes actual no mezcla el mismo mes de otro ano`() {
        // El historial tiene un 5 de octubre de 2025: no puede colarse.
        val resultado = filtrarPesos(historial, FiltroPeso.MES, hoy)

        assertTrue(
            resultado.none { it.fecha.year == 2025 },
            "el filtro de mes actual se trajo un pesaje de otro año: ${fechas(resultado)}"
        )
    }

    @Test
    fun `el mes actual respeta el cambio de mes`() {
        // El mismo historial, mirado desde noviembre: el mes en curso es otro.
        val desdeNoviembre = filtrarPesos(historial, FiltroPeso.MES, LocalDate.parse("2026-11-01"))

        assertEquals(listOf("2026-11-01"), fechas(desdeNoviembre))
    }

    @Test
    fun `un mes sin pesajes deja la lista vacia, no el historial entero`() {
        val enDiciembre = filtrarPesos(historial, FiltroPeso.MES, LocalDate.parse("2026-12-24"))

        assertTrue(enDiciembre.isEmpty(), "esperaba lista vacía y salió ${fechas(enDiciembre)}")
    }

    @Test
    fun `el ano elegido trae solo ese ano`() {
        val resultado = filtrarPesos(historial, FiltroPeso.ANIO, hoy, anio = 2026)

        assertEquals(
            listOf("2026-01-20", "2026-09-30", "2026-10-02", "2026-10-13", "2026-11-01"),
            fechas(resultado)
        )
    }

    @Test
    fun `un ano sin datos deja la lista vacia`() {
        assertTrue(filtrarPesos(historial, FiltroPeso.ANIO, hoy, anio = 2020).isEmpty())
    }

    @Test
    fun `el rango de fechas recorta por los dos lados`() {
        val resultado = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-09-01"),
            hasta = LocalDate.parse("2026-10-05")
        )

        assertEquals(listOf("2026-09-30", "2026-10-02"), fechas(resultado))
    }

    @Test
    fun `un rango abierto por un lado solo recorta por el otro`() {
        val soloDesde = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-10-01")
        )
        val soloHasta = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            hasta = LocalDate.parse("2026-01-31")
        )

        assertEquals(listOf("2026-10-02", "2026-10-13", "2026-11-01"), fechas(soloDesde))
        assertEquals(listOf("2025-10-05", "2026-01-20"), fechas(soloHasta))
    }

    @Test
    fun `sin fechas el rango no recorta nada`() {
        assertEquals(historial.size, filtrarPesos(historial, FiltroPeso.FECHAS, hoy).size)
    }

    @Test
    fun `los limites del rango entran en el resultado`() {
        // El día exacto que se escribe como "desde" o "hasta" cuenta: si no, el usuario
        // escribiría la fecha de un pesaje y no lo vería.
        val resultado = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-10-02"),
            hasta = LocalDate.parse("2026-10-13")
        )

        assertEquals(listOf("2026-10-02", "2026-10-13"), fechas(resultado))
    }

    @Test
    fun `todos los filtros devuelven la lista ordenada`() {
        val desordenado = historial.reversed()

        listOf(
            filtrarPesos(desordenado, FiltroPeso.TODO, hoy),
            filtrarPesos(desordenado, FiltroPeso.ANIO, hoy, anio = 2026),
            filtrarPesos(desordenado, FiltroPeso.MES, hoy),
            filtrarPesos(desordenado, FiltroPeso.FECHAS, hoy)
        ).forEach { resultado ->
            assertEquals(fechas(resultado).sorted(), fechas(resultado), "el gráfico espera orden cronológico")
        }
    }
}
