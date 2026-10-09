package com.example.virtualpetkmp.util

import kotlin.math.abs
import kotlin.math.round

/**
 * Formatea kilos con dos decimales y coma, como se escriben en español (1.7 -> "1,70").
 *
 * Se hace a mano porque `String.format` no existe en commonMain de Kotlin Multiplatform.
 * Está en `util` para que el formulario de mascota, el de pesaje y la hoja de peso usen
 * exactamente el mismo formato y no se desincronicen.
 */
fun formatearKilos(valor: Double): String {
    val centesimas = round(valor * 100).toLong()
    val signo = if (centesimas < 0) "-" else ""
    val absoluto = abs(centesimas)
    val entero = absoluto / 100
    val decimales = absoluto % 100
    return "$signo$entero,${decimales.toString().padStart(2, '0')}"
}

/**
 * Lee un peso escrito a mano admitiendo coma o punto como separador decimal ("12,4" o
 * "12.4"). Devuelve null si el texto está vacío o no es un número válido.
 */
fun parseKilos(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()
