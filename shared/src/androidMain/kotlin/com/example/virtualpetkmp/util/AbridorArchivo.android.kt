package com.example.virtualpetkmp.util

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberAbridorArchivo(): (String) -> Boolean {
    val context = LocalContext.current

    return { rutaArchivo ->
    try {
        val archivo = File(rutaArchivo)
        if (!archivo.exists()) {
            false
        } else {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }
    } catch (e: Exception) {
        false
    }
    }
}
