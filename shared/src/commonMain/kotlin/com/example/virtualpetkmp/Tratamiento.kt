package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Un medicamento que la mascota está tomando o ha tomado.
 *
 * Un tratamiento está **activo** cuando hoy cae entre [fechaInicio] y [fechaFin], o cuando no
 * tiene fecha de fin (crónico). Ese cálculo es el que alimenta el aviso "1 activo" de la
 * tarjeta de la ficha y la separación entre activos e historial.
 *
 * @param dosis texto tal cual lo escribió el usuario ("1/2 pastilla", "1,5 ml").
 * @param frecuencia cada cuánto se administra ("Cada 12 horas").
 * @param fechaFin `null` = tratamiento crónico, sin fecha de fin.
 * @param hora hora del día a la que avisar, en formato **"HH:mm"** ("08:30"). `null` = sin hora,
 *   y entonces no se puede avisar aunque [notificar] esté activo.
 * @param notificar el usuario quiere un recordatorio diario a esa hora mientras dure el
 *   tratamiento. Es una preferencia por tratamiento: dos medicamentos pueden avisar a horas
 *   distintas, y uno puede avisar y el otro no.
 */
data class Tratamiento(
    val id: Long? = null,
    val mascotaId: Long,
    val nombreMedicamento: String,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate? = null,
    val hora: String? = null,
    val notificar: Boolean = false
)
