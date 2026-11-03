package com.example.virtualpetkmp.util

import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.error_id_requerido
import virtualpetkmp.shared.generated.resources.error_inesperado
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

/**
 * Mensaje de un fallo cualquiera, en el idioma activo.
 *
 * Los motivos de dominio ([ErrorAppException]) se traducen con su texto propio. **Cualquier otra
 * excepción se enseña con un texto genérico traducido** y su detalle técnico va al registro
 * ([registrarFallo]): el mensaje de una excepción está escrito para quien programa (nombres de
 * columnas, rutas, códigos) y en un solo idioma, así que no puede ser lo que lea el usuario.
 */
suspend fun textoDeFallo(e: Throwable?): String = when (e) {
    is ErrorAppException -> textoErrorApp(e.error)
    else -> {
        registrarFallo(e)
        getString(Res.string.error_inesperado)
    }
}

/**
 * Deja un fallo en el registro, para poder diagnosticarlo.
 *
 * **Es el mecanismo que ya usa el proyecto**: `println`, que en Android acaba en `logcat` como
 * `System.out` y en escritorio en la consola. No hay librería de registro (ni Timber ni
 * `android.util.Log`) y no merecía la pena añadir una para esto. Se añade `printStackTrace()`
 * porque el detalle que de verdad sirve para arreglar un fallo es la traza, no solo el mensaje.
 */
internal fun registrarFallo(e: Throwable?) {
    if (e == null) return
    println("[VirtualPet] fallo inesperado: ${e::class.simpleName}: ${e.message}")
    e.printStackTrace()
}

/*
 * Aquí vivía `detalleDeError`, que pegaba el mensaje de la excepción detrás de avisos como
 * "No se pudo cargar la lista: <mensaje>". Se quitó en la 1.13: ese mensaje está escrito para
 * quien programa (nombres de columnas, rutas, códigos) y en un solo idioma. Ahora los avisos usan
 * su texto traducido, sin añadidos, y el detalle va al registro con [registrarFallo].
 */
