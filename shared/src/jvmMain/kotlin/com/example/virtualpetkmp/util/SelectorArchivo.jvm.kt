package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser

@Composable
actual fun rememberSelectorArchivo(
    onArchivoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    return {
        try {
            val selector = JFileChooser()
            selector.dialogTitle = "Seleccionar archivo"
            selector.fileSelectionMode = JFileChooser.FILES_ONLY

            val resultado = selector.showOpenDialog(null)
            if (resultado == JFileChooser.APPROVE_OPTION) {
                val archivoSeleccionado = selector.selectedFile
                if (archivoSeleccionado != null && archivoSeleccionado.exists()) {
                    val rutaLocal = copiarArchivoALocal(archivoSeleccionado)
                    if (rutaLocal != null) {
                        onArchivoSeleccionado(archivoSeleccionado.name, rutaLocal)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error abriendo selector JVM: ${e.message}")
        }
    }
}

private fun copiarArchivoALocal(archivoOriginal: File): String? {
    return try {
        val carpetaInformes = File(System.getProperty("user.home"), ".virtualpet/informes")
        if (!carpetaInformes.exists()) {
            carpetaInformes.mkdirs()
        }

        val nombreSeguro = archivoOriginal.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val archivoDestino = File(carpetaInformes, "${System.currentTimeMillis()}_$nombreSeguro")

        archivoOriginal.copyTo(archivoDestino, overwrite = true)
        archivoDestino.absolutePath
    } catch (e: Exception) {
        println("Error copiando archivo a local en JVM: ${e.message}")
        null
    }
}
