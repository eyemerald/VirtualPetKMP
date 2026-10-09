package com.example.virtualpetkmp

/**
 * Un apunte libre sobre una mascota ("le asustan los petardos", "alergia al pollo").
 *
 * Es el único dato de la app sin fecha ni estado: sirve para recordar cosas que no encajan en
 * ninguna otra categoría, y por eso la tarjeta de la ficha lo resume como "información
 * importante de la mascota" en lugar de mostrar un recuento.
 */
data class Nota(
    val id: Long? = null,
    val mascotaId: Long,
    val texto: String
)
