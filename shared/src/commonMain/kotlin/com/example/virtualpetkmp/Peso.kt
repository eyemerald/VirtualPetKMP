package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Un pesaje de una mascota.
 *
 * Es el único dato de la app que se mide en el tiempo y se representa en un gráfico: la
 * evolución del peso y su comparación con el peso ideal.
 *
 * @param id identificador en la base de datos. `null` mientras no se ha guardado.
 * @param mascotaId mascota a la que pertenece.
 * @param fecha día del pesaje. Nunca es nula: un peso sin fecha no se puede situar en el
 *   gráfico.
 * @param peso kilos. En la base de datos es un `REAL`, así que admite decimales.
 * @param notas apunte libre del usuario (por ejemplo "pesada en casa").
 */
data class Peso(
    val id: Long? = null,
    val mascotaId: Long,
    val fecha: LocalDate,
    val peso: Double,
    val notas: String? = null
)
