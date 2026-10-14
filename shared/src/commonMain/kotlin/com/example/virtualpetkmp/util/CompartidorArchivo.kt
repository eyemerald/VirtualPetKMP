package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/** Qué pasó al intentar compartir un archivo recién generado. */
enum class ResultadoCompartir {
    /** Se abrió el selector del sistema para elegir por dónde enviarlo. */
    COMPARTIDO,

    /** El dispositivo no tiene ninguna aplicación con la que compartir un PDF. */
    SIN_APLICACION,

    /** No se pudo ni preparar el archivo. */
    ERROR
}

/**
 * Devuelve una función que entrega un archivo al **compartir del sistema**, que es lo que abre la
 * hoja con WhatsApp, el correo, Drive y demás.
 *
 * Se le pasan el nombre y el contenido en memoria, no una ruta: cada plataforma decide dónde
 * dejarlo. En Android tiene que ser una carpeta declarada en el `FileProvider`, porque el archivo
 * lo lee otra aplicación; en escritorio no existe esa hoja y se abre el documento con la
 * aplicación por defecto, que es desde donde se adjunta al correo.
 *
 * Devuelve un [ResultadoCompartir] y no un `Boolean` porque "no hay ninguna app con la que
 * compartir" merece un mensaje distinto de "algo falló".
 */
@Composable
expect fun rememberCompartidorArchivo(): (nombreArchivo: String, contenido: ByteArray) -> ResultadoCompartir
