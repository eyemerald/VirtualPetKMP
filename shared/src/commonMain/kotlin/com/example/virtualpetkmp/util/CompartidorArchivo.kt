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

/**
 * Devuelve una función que entrega al compartir del sistema un archivo **que ya está en disco**.
 *
 * Existe aparte de [rememberCompartidorArchivo] por dos motivos: una copia de seguridad puede
 * pesar decenas de megas (no se mete en un `ByteArray`) y su tipo no es PDF, sino un archivo
 * genérico, para que WhatsApp, Telegram, el correo o Drive lo acepten como documento.
 *
 * Es lo que hace posible pasar los datos entre el móvil y el ordenador (o entre dos móviles) sin
 * cables: se comparte el `.vpet` por donde se quiera y en el otro lado se abre con la app.
 */
@Composable
expect fun rememberCompartidorPaquete(): (nombreArchivo: String, rutaLocal: String) -> ResultadoCompartir
