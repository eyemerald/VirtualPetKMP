package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Vacuna(
    val id: Long? = null,
    val mascotaId: Long,
    val nombre: String,
    val fechaAplicacion: LocalDate,
    val fechaProximaDosis: LocalDate,
    val veterinario: String? = null,
    val lote: String? = null
)
