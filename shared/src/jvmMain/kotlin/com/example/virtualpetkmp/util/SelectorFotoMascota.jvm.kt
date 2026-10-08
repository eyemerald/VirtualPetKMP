package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberFotoMascota(
    onFotoSeleccionada: (FotoSeleccionada) -> Unit,
    onError: (String) -> Unit
): AccionesFotoMascota {
    return AccionesFotoMascota(
        elegirDeArchivos = {
            try {
                val selector = JFileChooser()
                selector.dialogTitle = "Elegir foto de la mascota"
                selector.fileSelectionMode = JFileChooser.FILES_ONLY
                selector.isAcceptAllFileFilterUsed = true
                selector.fileFilter = FileNameExtensionFilter(
                    "Imágenes (jpg, jpeg, png, gif, bmp, webp)",
                    "jpg", "jpeg", "png", "gif", "bmp", "webp"
                )

                if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val archivo = selector.selectedFile
                    if (archivo != null && archivo.exists()) {
                        val ruta = copiarALocal(archivo)
                        if (ruta != null) {
                            onFotoSeleccionada(FotoSeleccionada(ruta = ruta, nombre = archivo.name))
                        } else {
                            onError("No se pudo copiar la imagen seleccionada")
                        }
                    }
                }
            } catch (e: Exception) {
                onError("No se pudo abrir el selector de imágenes: ${e.message ?: "desconocido"}")
            }
        },
        // En escritorio no se integra cámara: el botón de hacer foto se oculta.
        hacerFoto = null
    )
}

private fun copiarALocal(archivoOriginal: File): String? {
    return try {
        val carpeta = File(System.getProperty("user.home"), ".virtualpet/fotos")
        if (!carpeta.exists()) carpeta.mkdirs()

        val nombreSeguro = archivoOriginal.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destino = File(carpeta, "${System.currentTimeMillis()}_$nombreSeguro")
        archivoOriginal.copyTo(destino, overwrite = true)
        destino.absolutePath
    } catch (e: Exception) {
        println("Error copiando foto a local en JVM: ${e.message}")
        null
    }
}
