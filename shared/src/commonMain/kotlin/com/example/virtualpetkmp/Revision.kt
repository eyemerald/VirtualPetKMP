package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Una visita al veterinario: lo que pasó y qué se dijo.
 *
 * Es un registro histórico, no genera avisos ni estado en la tarjeta de la ficha.
 *
 * @param motivo por qué se llevó a la mascota ("Revisión anual", "Cojea de la pata derecha").
 * @param diagnostico conclusión del veterinario, si la hubo.
 */
data class Revision(
    val id: Long? = null,
    val mascotaId: Long,
    val fecha: LocalDate,
    val motivo: String,
    val diagnostico: String? = null,
    val notas: String? = null,
    val veterinario: String? = null
)
