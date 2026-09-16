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
            selector.dialogTitle = "Guardar PDF"
            selector.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            selector.selectedFile = File(System.getProperty("user.home"), "ficha-mascota.pdf")

            val resultado = selector.showSaveDialog(null)
            if (resultado == JFileChooser.APPROVE_OPTION) {
                val archivoSeleccionado = selector.selectedFile
                val archivoDestino = when {
                    archivoSeleccionado.isDirectory -> {
                        val carpetaDestino = archivoSeleccionado
                        val nombreArchivo = "ficha-mascota-${System.currentTimeMillis()}.pdf"
                        File(carpetaDestino, nombreArchivo)
                    }
                    archivoSeleccionado.absolutePath.lowercase().endsWith(".pdf") -> archivoSeleccionado
                    else -> File(archivoSeleccionado.parentFile ?: File(System.getProperty("user.home")), "${archivoSeleccionado.name}.pdf")
                }

                val carpetaDestino = archivoDestino.parentFile ?: File(System.getProperty("user.home"), ".virtualpet/exportaciones")
                if (!carpetaDestino.exists()) {
                    carpetaDestino.mkdirs()
                }

                val destinoFinal = if (archivoDestino.isDirectory) {
                    File(carpetaDestino, "ficha-mascota-${System.currentTimeMillis()}.pdf")
                } else {
                    File(carpetaDestino, archivoDestino.name)
                }

                onArchivoSeleccionado(destinoFinal.name, destinoFinal.absolutePath)
            }
        } catch (_: Exception) {
            val carpetaFallback = File(System.getProperty("user.home"), ".virtualpet/exportaciones")
            carpetaFallback.mkdirs()
            val rutaFallback = File(carpetaFallback, "ficha-mascota-${System.currentTimeMillis()}.pdf")
            onArchivoSeleccionado(rutaFallback.name, rutaFallback.absolutePath)
        }
    }
}
