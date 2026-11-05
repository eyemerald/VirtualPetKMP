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
 * @param frecuencia indicaciones libres del veterinario ("Cada 12 horas", "en ayunas"). Hasta la
 *   1.13 era "la frecuencia" a secas; con varias tomas al día el horario ya no se escribe aquí.
 * @param fechaFin `null` = tratamiento crónico, sin fecha de fin.
 * @param hora hora del día a la que avisar, en formato **"HH:mm"** ("08:30"). `null` = sin hora,
 *   y entonces no se puede avisar aunque [notificar] esté activo. Es la **primera** de [horas]
 *   cuando el tratamiento tiene varias tomas, y se sigue guardando para que un lector antiguo (o
 *   un `.vpet` de antes de la 1.14) no se quede sin hora.
 * @param vecesAlDia cuántas tomas tiene al día (1..6). `null` en los tratamientos de antes de la
 *   1.14: esos se comportan como una sola toma a la hora de [hora].
 * @param horas las horas de cada toma, en "HH:mm" separadas por comas ("08:00,14:00,22:00").
 *   `null` si no se definieron.
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
    val notificar: Boolean = false,
    val vecesAlDia: Int? = null,
    val horas: String? = null,
    /**
     * Identificador universal de la fila en `tratamientos`.
     *
     * No es decorativo: es lo que permite que un aviso ya programado se reconozca al sonar aunque
     * los ids locales hayan cambiado (al restaurar una copia, la fila se vuelve a insertar con otro
     * `id`). Ver `VerificacionAviso` y `RegistroAvisos`.
     */
    val uuid: String? = null
)
