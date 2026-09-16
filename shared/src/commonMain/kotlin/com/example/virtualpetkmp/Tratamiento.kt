package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Tratamiento(
    val id: Long? = null,
    val mascotaId: Long,
    val nombreMedicamento: String,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate? = null
)
