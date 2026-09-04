package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Mascota(
    val id: Long? = null,
    val nombre: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: LocalDate,
    val sexo: String,
    val color: String,
    val microchip: String?
)