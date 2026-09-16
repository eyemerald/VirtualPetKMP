package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Peso(
    val id: Long? = null,
    val mascotaId: Long,
    val fecha: LocalDate,
    val peso: Double,
    val notas: String? = null
)
