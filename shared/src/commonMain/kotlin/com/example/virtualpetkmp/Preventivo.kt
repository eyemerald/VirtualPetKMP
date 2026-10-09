package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Un tratamiento preventivo (pipeta, desparasitación interna, etc.).
 *
 * Se modela igual que una vacuna porque el caso de uso es el mismo: se aplica y toca repetir
 * cada cierto tiempo. La diferencia está en el aviso (los preventivos avisan 5 días antes
 * frente a los 15 de las vacunas) y en que aquí se distingue el [tipo].
 *
 * @param tipo categoría del preventivo; se guarda como texto libre para no cerrar la lista.
 * @param fechaProximaDosis fecha del siguiente recordatorio.
 */
data class Preventivo(
    val id: Long? = null,
    val mascotaId: Long,
    val tipo: String,
    val nombre: String,
    val fechaAplicacion: LocalDate,
    val fechaProximaDosis: LocalDate,
    val veterinario: String? = null,
    val lote: String? = null
)
