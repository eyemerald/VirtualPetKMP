package com.example.virtualpetkmp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val CARPETA_FOTOS = "fotos"

@Composable
actual fun rememberFotoMascota(
    onFotoSeleccionada: (FotoSeleccionada) -> Unit,
    onError: (ErrorFoto) -> Unit
): AccionesFotoMascota {
    val context = LocalContext.current

    // Archivo destino de la cámara: se crea antes de lanzar el Intent porque el
    // FileProvider necesita un Uri con permiso de escritura para la app de cámara.
    val archivoCamara = remember { arrayOfNulls<File>(1) }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val nombreOriginal = nombreDesdeUri(context, uri) ?: "foto_${System.currentTimeMillis()}.jpg"
                val ruta = copiarABiblioteca(context, uri, nombreOriginal)
                if (ruta != null) {
                    onFotoSeleccionada(FotoSeleccionada(ruta = ruta, nombre = nombreOriginal))
                } else {
                    onError(ErrorFoto.COPIAR)
                }
            } catch (e: Exception) {
                registrarFallo(e)
                onError(ErrorFoto.LEER)
            }
        }
    }

    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito: Boolean ->
        val archivo = archivoCamara[0]
        archivoCamara[0] = null
        if (!exito || archivo == null) return@rememberLauncherForActivityResult
        try {
            if (archivo.exists() && archivo.length() > 0) {
                val ruta = copiarABiblioteca(context, Uri.fromFile(archivo), "foto_${System.currentTimeMillis()}.jpg")
                archivo.delete()
                if (ruta != null) {
                    onFotoSeleccionada(FotoSeleccionada(ruta = ruta, nombre = "Foto de cámara"))
                } else {
                    onError(ErrorFoto.GUARDAR_CAMARA)
                }
            } else {
                archivo.delete()
                onError(ErrorFoto.CAMARA_SIN_IMAGEN)
            }
        } catch (e: Exception) {
            registrarFallo(e)
            onError(ErrorFoto.GUARDAR_CAMARA)
        }
    }

    return remember(launcherGaleria, launcherCamara) {
        AccionesFotoMascota(
            elegirDeArchivos = {
                try {
                    launcherGaleria.launch("image/*")
                } catch (e: Exception) {
                    registrarFallo(e)
                    onError(ErrorFoto.SIN_GALERIA)
                }
            },
            hacerFoto = {
                try {
                    val archivo = File(carpetaTemporal(context), "captura_${System.currentTimeMillis()}.jpg")
                    archivo.parentFile?.mkdirs()
                    archivo.createNewFile()
                    archivoCamara[0] = archivo

                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        archivo
                    )
                    launcherCamara.launch(uri)
                } catch (e: Exception) {
                    archivoCamara[0] = null
                    registrarFallo(e)
                    onError(ErrorFoto.ABRIR_CAMARA)
                }
            }
        )
    }
}

/**
 * Carpeta privada donde se guardan las copias de las fotos de las mascotas
 * (`filesDir/fotos`). Es la misma ruta que expone el FileProvider.
 */
private fun carpetaFotos(context: Context): File {
    val carpeta = File(context.filesDir, CARPETA_FOTOS)
    if (!carpeta.exists()) carpeta.mkdirs()
    return carpeta
}

private fun carpetaTemporal(context: Context): File {
    val carpeta = File(context.cacheDir, "capturas")
    if (!carpeta.exists()) carpeta.mkdirs()
    return carpeta
}

private fun nombreDesdeUri(context: Context, uri: Uri): String? {
    var nombre: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && indice >= 0) {
                nombre = cursor.getString(indice)
            }
        }
    } catch (_: Exception) {
        // El proveedor puede no exponer DISPLAY_NAME; se usa el nombre por defecto.
    }
    return nombre
}

/**
 * Copia el contenido del [uri] a la carpeta privada de fotos y devuelve la ruta absoluta.
 * Se conserva la extensión original (o se deduce del MIME) porque los decodificadores
 * de imagen se guían por la extensión en algunos casos.
 */
private fun copiarABiblioteca(context: Context, uri: Uri, nombreOriginal: String): String? {
    return try {
        val carpeta = carpetaFotos(context)
        val nombreSeguro = nombreOriginal
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "foto.jpg" }
        val archivoDestino = File(carpeta, "${System.currentTimeMillis()}_$nombreSeguro")

        context.contentResolver.openInputStream(uri)?.use { entrada ->
            archivoDestino.outputStream().use { salida ->
                entrada.copyTo(salida)
            }
        } ?: return null

        if (archivoDestino.length() == 0L) {
            archivoDestino.delete()
            return null
        }

        // La foto se guarda ya reducida (ver `RedimensionFoto.android.kt`): en la ficha se ve a
        // 84 dp, así que guardar 12 MP es ocupar espacio para píxeles que nunca se pintan.
        reducirFotoDeMascota(archivoDestino.absolutePath)

        archivoDestino.absolutePath
    } catch (e: Exception) {
        println("Error copiando foto a local: ${e.message}")
        null
    }
}
