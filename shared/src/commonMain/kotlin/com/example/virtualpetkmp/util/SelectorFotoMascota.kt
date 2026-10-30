package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.error_foto_abrir_camara
import virtualpetkmp.shared.generated.resources.error_foto_abrir_selector
import virtualpetkmp.shared.generated.resources.error_foto_camara_sin_imagen
import virtualpetkmp.shared.generated.resources.error_foto_copiar
import virtualpetkmp.shared.generated.resources.error_foto_guardar_camara
import virtualpetkmp.shared.generated.resources.error_foto_leer
import virtualpetkmp.shared.generated.resources.error_foto_sin_galeria

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
 * Lo que puede fallar al elegir o hacer la foto de una mascota.
 *
 * **Por qué es un enum y no un texto.** El selector es código de plataforma (`actual`), y ahí no se
 * pueden leer los recursos de idioma de Compose: antes devolvía frases en español escritas a mano,
 * que el usuario veía tal cual en cualquier idioma. Ahora devuelve el motivo, y la pantalla lo
 * convierte a texto con [claveDeErrorFoto]. El detalle técnico de la excepción (si lo hay) va al
 * registro con [registrarFallo].
 */
enum class ErrorFoto {
    /** No se pudo copiar la imagen elegida a la carpeta privada de la app. */
    COPIAR,

    /** La imagen no se pudo leer o decodificar. */
    LEER,

    /** La foto hecha con la cámara no se pudo guardar. */
    GUARDAR_CAMARA,

    /** La cámara terminó sin devolver ninguna imagen. */
    CAMARA_SIN_IMAGEN,

    /** No hay ninguna galería o aplicación de imágenes disponible. */
    SIN_GALERIA,

    /** No se pudo abrir la cámara. */
    ABRIR_CAMARA,

    /** No se pudo abrir el selector de archivos (escritorio). */
    ABRIR_SELECTOR
}

/**
 * Texto que se enseña para un [ErrorFoto], elegido de los recursos de la app.
 *
 * Es una función pura (enum → recurso) para poder probarla sin pintar nada: el test comprueba que
 * los siete motivos tienen texto y que ninguno repite el de otro.
 */
fun claveDeErrorFoto(error: ErrorFoto): StringResource = when (error) {
    ErrorFoto.COPIAR -> Res.string.error_foto_copiar
    ErrorFoto.LEER -> Res.string.error_foto_leer
    ErrorFoto.GUARDAR_CAMARA -> Res.string.error_foto_guardar_camara
    ErrorFoto.CAMARA_SIN_IMAGEN -> Res.string.error_foto_camara_sin_imagen
    ErrorFoto.SIN_GALERIA -> Res.string.error_foto_sin_galeria
    ErrorFoto.ABRIR_CAMARA -> Res.string.error_foto_abrir_camara
    ErrorFoto.ABRIR_SELECTOR -> Res.string.error_foto_abrir_selector
}

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
    onError: (ErrorFoto) -> Unit = {}
): AccionesFotoMascota
