package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Revision(
    val id: Long? = null,
    val mascotaId: Long,
    val fecha: LocalDate,
    val motivo: String,
    val diagnostico: String? = null,
    val notas: String? = null,
    val veterinario: String? = null
)
