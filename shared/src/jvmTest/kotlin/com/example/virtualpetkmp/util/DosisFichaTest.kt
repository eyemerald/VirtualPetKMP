package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Las cuentas de la ficha ("1 vencida", "2 próximas") con la **misma regla que el filtro**: solo
 * cuenta el último registro de cada vacuna o preventivo.
 *
 * Antes se contaba cada fila, así que una vacuna puesta tres veces con la primera dosis vencida
 * salía como vencida para siempre aunque la última estuviera al día. Las ventanas de la ficha no
 * cambian: 15 días en vacunas y 5 en preventivos.
 */
class DosisFichaTest {

    private val hoy = LocalDate(2026, 6, 15)

    @Test
    fun `una dosis vieja no sale vencida si hay un registro posterior`() {
        val filas = listOf(
            fila("Rabia", aplicada = LocalDate(2024, 1, 1), proxima = LocalDate(2025, 1, 1)),
            fila("Rabia", aplicada = LocalDate(2026, 5, 1), proxima = LocalDate(2027, 5, 1))
        )

        assertEquals(0 to 0, Dosis.contarParaFicha(filas, hoy, 15))
    }

    @Test
    fun `la ultima dosis vencida si cuenta`() {
        val filas = listOf(
            fila("Rabia", aplicada = LocalDate(2024, 1, 1), proxima = LocalDate(2025, 1, 1)),
            fila("Rabia", aplicada = LocalDate(2026, 5, 1), proxima = LocalDate(2026, 6, 1))
        )

        assertEquals(1 to 0, Dosis.contarParaFicha(filas, hoy, 15))
    }

    @Test
    fun `las ventanas de la ficha siguen siendo 15 y 5 dias`() {
        val enDiez = listOf(fila("Rabia", LocalDate(2025, 1, 1), LocalDate.fromEpochDays(hoy.toEpochDays() + 10)))
        val enVeinte = listOf(fila("Rabia", LocalDate(2025, 1, 1), LocalDate.fromEpochDays(hoy.toEpochDays() + 20)))

        // Con 15 dias, la de 10 esta "proxima" y la de 20 no.
        assertEquals(0 to 1, Dosis.contarParaFicha(enDiez, hoy, 15))
        assertEquals(0 to 0, Dosis.contarParaFicha(enVeinte, hoy, 15))
        // Con 5 dias (preventivos), la de 10 ya no esta proxima.
        assertEquals(0 to 0, Dosis.contarParaFicha(enDiez, hoy, 5))
    }

    @Test
    fun `el nombre escrito distinto es la misma vacuna`() {
        val filas = listOf(
            fila("Rabia", aplicada = LocalDate(2024, 1, 1), proxima = LocalDate(2025, 1, 1)),
            fila(" RÁBIA ", aplicada = LocalDate(2026, 5, 1), proxima = LocalDate(2027, 5, 1))
        )

        assertEquals(0 to 0, Dosis.contarParaFicha(filas, hoy, 15))
    }

    private fun fila(nombre: String, aplicada: LocalDate, proxima: LocalDate) =
        FilaDosis(mascotaId = 1L, nombre = nombre, fechaAplicacion = aplicada, fechaProximaDosis = proxima)
}
