package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

data class Informe(
    val id: Long? = null,
    val mascotaId: Long,
    val tipo: String,
    val descripcion: String? = null,
    val fecha: LocalDate,
    val nombreArchivo: String,
    val rutaArchivo: String
)
