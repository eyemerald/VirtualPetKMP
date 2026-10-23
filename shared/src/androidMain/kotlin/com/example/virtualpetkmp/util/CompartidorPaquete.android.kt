package com.example.virtualpetkmp.util

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android: deja el paquete en la caché (copiándolo, no cargándolo en memoria) y lanza el compartir.
 *
 * El tipo es `application/octet-stream` a propósito: es la forma de que WhatsApp, Telegram, el
 * correo y Drive lo traten como un documento que se puede enviar. Con `application/zip` algunos
 * destinatarios lo bloquean.
 *
 * La carpeta tiene que ser una de las declaradas en `file_paths.xml` o el `FileProvider` se niega a
 * entregar el archivo.
 */
@Composable
actual fun rememberCompartidorPaquete(): (String, String) -> ResultadoCompartir {
    val context = LocalContext.current

    return { nombreArchivo, rutaLocal ->
        try {
            val origen = File(rutaLocal)
            if (!origen.isFile) {
                ResultadoCompartir.ERROR
            } else {
                val carpeta = File(context.cacheDir, "exportaciones").apply { mkdirs() }
                val destino = File(carpeta, nombreArchivo)
                origen.copyTo(destino, overwrite = true)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destino
                )
                val enviar = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, nombreArchivo)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val elegir = Intent.createChooser(enviar, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(elegir)
                ResultadoCompartir.COMPARTIDO
            }
        } catch (e: ActivityNotFoundException) {
            ResultadoCompartir.SIN_APLICACION
        } catch (e: Exception) {
            println("[CompartidorPaquete] error: ${e.message}")
            ResultadoCompartir.ERROR
        }
    }
}
