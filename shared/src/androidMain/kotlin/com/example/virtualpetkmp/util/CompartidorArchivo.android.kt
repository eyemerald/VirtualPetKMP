package com.example.virtualpetkmp.util

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android: deja el PDF en la caché y lanza el compartir del sistema.
 *
 * La carpeta tiene que ser una de las declaradas en `file_paths.xml`, o el `FileProvider` se
 * niega a entregar el archivo y la otra aplicación se encuentra con un permiso denegado.
 */
@Composable
actual fun rememberCompartidorArchivo(): (String, ByteArray) -> ResultadoCompartir {
    val context = LocalContext.current

    return { nombreArchivo, contenido ->
        try {
            val carpeta = File(context.cacheDir, "exportaciones").apply { mkdirs() }
            val archivo = File(carpeta, nombreArchivo)
            archivo.writeBytes(contenido)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
            val enviar = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, nombreArchivo)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val elegir = Intent.createChooser(enviar, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(elegir)
            ResultadoCompartir.COMPARTIDO
        } catch (e: ActivityNotFoundException) {
            // Ninguna app instalada sabe recibir un PDF.
            ResultadoCompartir.SIN_APLICACION
        } catch (e: Exception) {
            ResultadoCompartir.ERROR
        }
    }
}
