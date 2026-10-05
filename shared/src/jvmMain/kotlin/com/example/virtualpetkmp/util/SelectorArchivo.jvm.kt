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

@Composable
actual fun rememberSelectorDestino(
    nombreSugerido: String,
    onDestinoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    return {
        try {
            val selector = JFileChooser()
            selector.dialogTitle = "Guardar PDF como"
            selector.fileSelectionMode = JFileChooser.FILES_ONLY
            selector.selectedFile = File(System.getProperty("user.home"), nombreSugerido.ifBlank { "ficha-mascota.pdf" })

            val resultado = selector.showSaveDialog(null)
            if (resultado == JFileChooser.APPROVE_OPTION) {
                val archivoSeleccionado = selector.selectedFile
                if (archivoSeleccionado != null) {
                    val rutaFinal = if (archivoSeleccionado.absolutePath.lowercase().endsWith(".pdf")) {
                        archivoSeleccionado
                    } else {
                        File(archivoSeleccionado.parentFile ?: File(System.getProperty("user.home")), "${archivoSeleccionado.name}.pdf")
                    }
                    onDestinoSeleccionado(rutaFinal.name, rutaFinal.absolutePath)
                }
            }
        } catch (e: Exception) {
            println("Error abriendo selector destino JVM: ${e.message}")
        }
    }
}
