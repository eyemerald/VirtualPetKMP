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
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val nombreOriginal = obtenerNombreArchivo(context, uri) ?: "ficha-mascota.pdf"
            // Return the content URI (the document the system created). The actual write will use ContentResolver.
            onArchivoSeleccionado(nombreOriginal, uri.toString())
        }
    }

    return {
        try {
            launcher.launch("ficha-mascota.pdf")
        } catch (_: Exception) {
            val carpetaDestino = File(context.filesDir, "exportaciones")
            carpetaDestino.mkdirs()
            val rutaFallback = File(carpetaDestino, "ficha-mascota-${System.currentTimeMillis()}.pdf")
            onArchivoSeleccionado(rutaFallback.name, rutaFallback.absolutePath)
        }
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String? {
    var nombre: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && indiceNombre >= 0) {
            nombre = cursor.getString(indiceNombre)
        }
    }
    return nombre
}
