package com.example.virtualpetkmp

/**
 * Una clínica veterinaria de la agenda.
 *
 * No está vinculada a las mascotas: es una agenda de contactos. Lo que sí se usa desde otros
 * sitios son los datos sueltos (el nombre del veterinario se escribe a mano en vacunas y
 * revisiones, no se elige de aquí).
 *
 * @param esUrgencias si atiende 24 horas. Los de urgencias se agrupan aparte y llevan un
 *   botón de llamada destacado, porque es el caso en el que se busca rapidez.
 */
data class Veterinario(
    val id: Long? = null,
    val nombreClinica: String,
    val nombreVeterinario: String? = null,
    val telefono: String,
    val direccion: String,
    val esUrgencias: Boolean = false
)
