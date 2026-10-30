package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.dialogo_elegir_foto
import virtualpetkmp.shared.generated.resources.dialogo_filtro_imagenes

@Composable
actual fun rememberFotoMascota(
    onFotoSeleccionada: (FotoSeleccionada) -> Unit,
    onError: (ErrorFoto) -> Unit
): AccionesFotoMascota {
    return AccionesFotoMascota(
        elegirDeArchivos = {
            try {
                val selector = JFileChooser()
                selector.dialogTitle = runBlocking { getString(Res.string.dialogo_elegir_foto) }
                selector.fileSelectionMode = JFileChooser.FILES_ONLY
                selector.isAcceptAllFileFilterUsed = true
                selector.fileFilter = FileNameExtensionFilter(
                    runBlocking { getString(Res.string.dialogo_filtro_imagenes) } + " (jpg, jpeg, png, gif, bmp, webp)",
                    "jpg", "jpeg", "png", "gif", "bmp", "webp"
                )

                if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val archivo = selector.selectedFile
                    if (archivo != null && archivo.exists()) {
                        val ruta = copiarALocal(archivo)
                        if (ruta != null) {
                            onFotoSeleccionada(FotoSeleccionada(ruta = ruta, nombre = archivo.name))
                        } else {
                            onError(ErrorFoto.COPIAR)
                        }
                    }
                }
            } catch (e: Exception) {
                registrarFallo(e)
                onError(ErrorFoto.ABRIR_SELECTOR)
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

        // Se guarda ya reducida: en la ficha se ve a 84 dp (ver `RedimensionFoto.jvm.kt`).
        reducirFotoDeMascota(destino.absolutePath)

        destino.absolutePath
    } catch (e: Exception) {
        registrarFallo(e)
        null
    }
}
