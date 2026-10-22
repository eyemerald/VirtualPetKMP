package com.example.virtualpetkmp.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
actual fun rememberSelectorArchivo(
    onArchivoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val nombreOriginal = obtenerNombreArchivo(context, uri) ?: "archivo_${System.currentTimeMillis()}"
                val rutaLocal = copiarArchivoALocal(context, uri, nombreOriginal)
                if (rutaLocal != null) {
                    onArchivoSeleccionado(nombreOriginal, rutaLocal)
                }
            } catch (e: Exception) {
                println("Error copiando archivo: ${e.message}")
            }
        }
    }

    return {
        try {
            // Aceptar cualquier tipo de archivo (*/*)
            launcher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            println("Error abriendo selector: ${e.message}")
        }
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String? {
    var nombre: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && indiceNombre >= 0) {
                nombre = cursor.getString(indiceNombre)
            }
        }
    } catch (_: Exception) { }
    return nombre
}

private fun copiarArchivoALocal(context: Context, uri: Uri, nombreOriginal: String): String? {
    return try {
        val carpetaInformes = File(context.filesDir, "informes")
        if (!carpetaInformes.exists()) {
            carpetaInformes.mkdirs()
        }

        // Prefijo con timestamp para evitar colisiones
        val nombreSeguro = nombreOriginal.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val archivoDestino = File(carpetaInformes, "${System.currentTimeMillis()}_$nombreSeguro")

        context.contentResolver.openInputStream(uri)?.use { input ->
            archivoDestino.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        archivoDestino.absolutePath
    } catch (e: Exception) {
        println("Error copiando archivo a local: ${e.message}")
        null
    }
}

@Composable
actual fun rememberSelectorDestino(
    nombreSugerido: String,
    onDestinoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            val nombreFinal = nombreSugerido.ifBlank { "ficha-mascota.pdf" }
            onDestinoSeleccionado(nombreFinal, uri.toString())
        }
    }

    return {
        try {
            launcher.launch(nombreSugerido.ifBlank { "ficha-mascota.pdf" })
        } catch (e: Exception) {
            println("Error abriendo selector destino: ${e.message}")
        }
    }
}

/**
 * Elige un `.vpet`. El sistema devuelve un `content://`, así que se copia a la caché de la app y se
 * trabaja con una ruta normal: los lectores de ZIP y de SQLite de este proyecto esperan ficheros.
 *
 * La copia va a `cacheDir/pendientes` y no a `filesDir`: es un fichero temporal que se puede
 * regenerar en cualquier momento y Android puede borrarlo si se queda sin espacio.
 */
@Composable
actual fun rememberSelectorCopia(
    onElegida: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        // Genérico a propósito: un `.vpet` no está registrado como tipo conocido en el sistema.
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val nombre = obtenerNombreArchivo(context, uri) ?: "copia-${System.currentTimeMillis()}.vpet"
                val carpeta = java.io.File(context.cacheDir, "pendientes").apply { mkdirs() }
                val destino = java.io.File(carpeta, "${System.currentTimeMillis()}_${nombre.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
                if (copiarArchivoA(destino.absolutePath, uri.toString())) {
                    onElegida(nombre, destino.absolutePath)
                } else {
                    println("[SelectorCopia] no se pudo copiar el paquete elegido")
                }
            } catch (e: Exception) {
                println("[SelectorCopia] error: ${e.message}")
            }
        }
    }

    return { launcher.launch(arrayOf("*/*")) }
}

@Composable
actual fun rememberSelectorDestinoCopia(
    nombreSugerido: String,
    onElegido: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        // `application/octet-stream` y no `application/zip`: es un ZIP por dentro, pero el sistema
        // añade la extensión de su tipo al nombre (**.vpet.zip**), y entonces el fichero deja de
        // reconocerse por su nombre. Con el tipo genérico se guarda como `.vpet`.
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            onElegido(nombreSugerido.ifBlank { "VirtualPet-copia.vpet" }, uri.toString())
        }
    }

    return {
        try {
            launcher.launch(nombreSugerido.ifBlank { "VirtualPet-copia.vpet" })
        } catch (e: Exception) {
            println("[SelectorDestinoCopia] error: ${e.message}")
        }
    }
}
