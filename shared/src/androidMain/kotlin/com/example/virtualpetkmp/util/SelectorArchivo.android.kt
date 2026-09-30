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
