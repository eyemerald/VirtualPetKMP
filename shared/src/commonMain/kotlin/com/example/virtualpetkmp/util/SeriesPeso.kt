package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.LocalDate

/**
 * Un punto del gráfico de peso: el valor que se dibuja y la fecha que representa.
 *
 * [media] es `null` en los meses sin ningún pesaje, que no se dibujan como punto pero que la
 * línea sí atraviesa para que la evolución se vea entera.
 */
data class PuntoPeso(val fecha: LocalDate, val media: Double?)

/**
 * Cada cuánto se dibuja un punto en el gráfico.
 *
 * Lo decide el filtro activo, no los datos: ver [presentacionDe]. Un historial de años con un
 * punto por pesaje sería un amasijo, así que el filtro de un año agrupa por meses; el resto de
 * filtros enseñan cada pesaje, que es el detalle que el usuario quiere ver.
 */
enum class GranularidadPeso {
    /** Un punto por mes, con la media de los pesajes de ese mes. */
    MENSUAL,

    /** Un punto por pesaje, en orden de fecha. */
    POR_PESAJES
}

/**
 * Construye la serie que dibuja el gráfico, **siempre en orden cronológico**.
 *
 * - [GranularidadPeso.POR_PESAJES]: un punto por pesaje.
 * - [GranularidadPeso.MENSUAL]: un punto por cada mes entre el primer y el último pesaje,
 *   incluidos los meses sin datos (con `media = null`), para que el eje sea una línea de tiempo
 *   continua y no un salto.
 */
fun serieDePeso(pesos: List<Peso>, granularidad: GranularidadPeso): List<PuntoPeso> =
    when (granularidad) {
        GranularidadPeso.POR_PESAJES -> pesos.sortedBy { it.fecha }
            .map { PuntoPeso(fecha = it.fecha, media = it.peso) }

        GranularidadPeso.MENSUAL -> serieMensual(pesos)
    }

/** Serie mensual: la media de cada mes, con los meses sin pesajes intercalados como huecos. */
private fun serieMensual(pesos: List<Peso>): List<PuntoPeso> {
    if (pesos.isEmpty()) return emptyList()

    val medias = pesos.groupBy { it.fecha.year to it.fecha.monthNumber }
        .mapValues { (_, delMes) -> delMes.sumOf { it.peso } / delMes.size }

    val primera = pesos.minBy { it.fecha }.fecha
    val ultima = pesos.maxBy { it.fecha }.fecha
    val totalMeses = (ultima.year - primera.year) * 12 + (ultima.monthNumber - primera.monthNumber)

    return (0..totalMeses).map { avance ->
        val bruto = primera.monthNumber - 1 + avance
        val anio = primera.year + bruto / 12
        val mes = bruto % 12 + 1
        PuntoPeso(
            fecha = LocalDate(anio, mes, 1),
            media = medias[anio to mes]
        )
    }
}
