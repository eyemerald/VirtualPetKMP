package com.example.virtualpetkmp.util

import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.error_id_requerido
import virtualpetkmp.shared.generated.resources.error_nombre_duplicado

/**
 * Motivos de error que la **capa de datos** puede devolver y que la interfaz sabe traducir.
 *
 * Los repositorios no pueden montar el texto final: no tienen contexto composable y no deben
 * saber en qué idioma está el usuario. Así que devuelven el motivo, y el ViewModel (que sí
 * corre dentro de una corrutina) lo convierte a texto con [textoErrorApp].
 *
 * Es un enum y no un texto para que traducir un mensaje no obligue a tocar la lógica de la
 * base de datos, y para que un mensaje nuevo no se olvide a medias.
 */
enum class ErrorApp {
    /** Ya hay otra mascota con ese nombre (no distingue mayúsculas). */
    NOMBRE_DUPLICADO,

    /** Se intentó actualizar un registro sin id. */
    ID_REQUERIDO
}

/**
 * Error de dominio traducible.
 *
 * Se hereda de `Exception` a propósito: los repositorios devuelven `Result.failure`, y así
 * este motivo viaja por el mismo cauce que cualquier otro fallo sin cambiar las firmas.
 */
class ErrorAppException(val error: ErrorApp) : Exception(error.name)

/**
 * Texto del error en el idioma del sistema.
 *
 * Es `suspend` porque lee un recurso de Compose fuera de la composición; se llama desde el
 * ViewModel, que ya está dentro de una corrutina.
 */
suspend fun textoErrorApp(error: ErrorApp): String = when (error) {
    ErrorApp.NOMBRE_DUPLICADO -> getString(Res.string.error_nombre_duplicado)
    ErrorApp.ID_REQUERIDO -> getString(Res.string.error_id_requerido)
}
