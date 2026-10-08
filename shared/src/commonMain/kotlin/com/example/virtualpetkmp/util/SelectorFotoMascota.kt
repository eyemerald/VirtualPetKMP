package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Resultado de elegir o hacer una foto para una mascota.
 */
data class FotoSeleccionada(
    /** Ruta absoluta del archivo de imagen en el almacenamiento propio de la app. */
    val ruta: String,
    /** Nombre visible del archivo original (o generado), solo informativo. */
    val nombre: String
)

/**
 * Devuelve las acciones disponibles para obtener la foto de una mascota.
 *
 * - [elegirDeArchivos]: abre el selector de imágenes del sistema (galería / explorador).
 * - [hacerFoto]: abre la cámara. Es `null` en plataformas sin integración de cámara
 *   (por ejemplo escritorio), para poder ocultar el botón correspondiente.
 *
 * Ambas acciones devuelven la ruta de una copia local de la imagen, de modo que la
 * app no dependa de que el archivo original siga existiendo.
 */
class AccionesFotoMascota(
    val elegirDeArchivos: () -> Unit,
    val hacerFoto: (() -> Unit)?,
    val soportaCamara: Boolean = hacerFoto != null
)

@Composable
expect fun rememberFotoMascota(
    onFotoSeleccionada: (FotoSeleccionada) -> Unit,
    onError: (String) -> Unit = {}
): AccionesFotoMascota
