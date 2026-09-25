package com.example.virtualpetkmp

data class Veterinario(
    val id: Long? = null,
    val nombreClinica: String,
    val nombreVeterinario: String? = null,
    val telefono: String,
    val direccion: String,
    val esUrgencias: Boolean = false
)
