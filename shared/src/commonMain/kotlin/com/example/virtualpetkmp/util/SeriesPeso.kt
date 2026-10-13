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
 * El gráfico agrupa por meses porque un historial de años, con un punto por pesaje, sería un
 * amasijo. Pero cuando todo lo que se está mirando cae dentro del **mismo mes** —el caso del
 * filtro "Mes actual", o de una mascota recién pesada por primera vez— agrupar por mes deja un
 * único punto y el gráfico se queda sin línea: se ve un punto suelto y parece roto. En ese caso
 * se dibuja un punto por pesaje, que es lo que el usuario quiere ver: cómo evolucionó ese mes.
 */
enum class GranularidadPeso {
    /** Un punto por mes, con la media de los pesajes de ese mes. */
    MENSUAL,

    /** Un punto por pesaje, en orden de fecha. */
    POR_PESAJES
}

/**
 * Decide la granularidad de [pesos]: por pesajes si todos caen en el mismo mes natural, y
 * mensual en cualquier otro caso.
 */
fun granularidadDe(pesos: List<Peso>): GranularidadPeso {
    val primera = pesos.minByOrNull { it.fecha } ?: return GranularidadPeso.MENSUAL
    val ultima = pesos.maxByOrNull { it.fecha } ?: return GranularidadPeso.MENSUAL

    val mismoAnio = primera.fecha.year == ultima.fecha.year
    val mismoMes = primera.fecha.monthNumber == ultima.fecha.monthNumber
    return if (mismoAnio && mismoMes) GranularidadPeso.POR_PESAJES else GranularidadPeso.MENSUAL
}

/**
 * Construye la serie que dibuja el gráfico, **siempre en orden cronológico**.
 *
 * - [GranularidadPeso.POR_PESAJES]: un punto por pesaje.
 * - [GranularidadPeso.MENSUAL]: un punto por cada mes entre el primer y el último pesaje,
 *   incluidos los meses sin datos (con `media = null`), para que el eje sea una línea de tiempo
 *   continua y no un salto.
 */
fun serieDePeso(pesos: List<Peso>): List<PuntoPeso> = when (granularidadDe(pesos)) {
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
