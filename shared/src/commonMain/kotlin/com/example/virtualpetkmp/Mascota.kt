package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Una mascota: la entidad raíz de la app. Todo lo demás (vacunas, pesos, notas, visitas...)
 * cuelga de ella mediante `mascotaId` y se borra en cascada al eliminarla.
 *
 * @param id identificador en la base de datos. `null` mientras no se ha guardado, que es lo
 *   que distingue "alta" de "edición" al llamar a `saveMascota`.
 * @param color color del pelaje. Obligatorio en el formulario, aunque parezca un detalle.
 * @param microchip número de microchip, opcional.
 * @param foto ruta absoluta del archivo de imagen. `null` = sin foto, y entonces el avatar
 *   dibuja la inicial del nombre.
 * @param pesoIdeal kilos que el usuario considera ideales. `null` = sin configurar, y en ese
 *   caso no se dibuja la referencia en el gráfico ni el indicador en la lista.
 */
data class Mascota(
    val id: Long? = null,
    val nombre: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: LocalDate,
    val sexo: String,
    val color: String,
    val microchip: String?,
    val foto: String? = null,
    val pesoIdeal: Double? = null
)