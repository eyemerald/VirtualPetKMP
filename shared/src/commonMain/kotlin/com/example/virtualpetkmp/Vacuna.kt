package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Una vacuna aplicada a una mascota, con la fecha de la próxima dosis.
 *
 * La app no guarda una pauta de vacunación: guarda cada dosis aplicada y cuándo toca la
 * siguiente, que es lo que alimenta los avisos y el estado de la tarjeta ("vencida",
 * "vence pronto", "al día").
 *
 * @param fechaAplicacion día en que se puso.
 * @param fechaProximaDosis día en que toca la siguiente. Es la fecha que se usa para el
 *   estado y para programar la notificación local.
 * @param veterinario quién la aplicó, si el usuario lo anota.
 * @param lote número de lote del vial, si el usuario lo anota.
 */
data class Vacuna(
    val id: Long? = null,
    val mascotaId: Long,
    val nombre: String,
    val fechaAplicacion: LocalDate,
    val fechaProximaDosis: LocalDate,
    val veterinario: String? = null,
    val lote: String? = null
)
