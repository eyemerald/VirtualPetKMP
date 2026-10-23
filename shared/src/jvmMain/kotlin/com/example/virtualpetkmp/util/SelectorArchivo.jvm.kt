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

/** Escritorio: el paquete se lee directamente de donde esté, sin copiarlo. */
@Composable
actual fun rememberSelectorCopia(
    onElegida: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    return {
        try {
            val selector = JFileChooser().apply {
                dialogTitle = "Elegir copia de seguridad"
                fileSelectionMode = JFileChooser.FILES_ONLY
                // Filtro por extensión, pero sin impedir elegir otro fichero: así se puede abrir un
                // paquete renombrado y se le da un error claro en vez de no dejarlo ni intentar.
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Copia de Virtual Pet (*.vpet)", "vpet")
                currentDirectory = File(System.getProperty("user.home"))
            }
            if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val archivo = selector.selectedFile
                if (archivo != null && archivo.isFile) {
                    onElegida(archivo.name, archivo.absolutePath)
                }
            }
        } catch (e: Exception) {
            println("Error abriendo selector de copia JVM: ${e.message}")
        }
    }
}

@Composable
actual fun rememberSelectorDestinoCopia(
    nombreSugerido: String,
    onElegido: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit {
    return {
        try {
            val sugerido = nombreSugerido.ifBlank { "VirtualPet-copia.vpet" }
            val carpetaInicial = carpetaDeDrive() ?: File(System.getProperty("user.home"))
            val selector = JFileChooser().apply {
                dialogTitle = "Guardar copia de seguridad"
                fileSelectionMode = JFileChooser.FILES_ONLY
                currentDirectory = carpetaInicial
                selectedFile = File(carpetaInicial, sugerido)
            }
            if (selector.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                val elegido = selector.selectedFile
                if (elegido != null) {
                    // Sin extensión, el fichero no se reconocería después al elegirlo para
                    // restaurar, así que se añade.
                    val rutaFinal = if (elegido.absolutePath.lowercase().endsWith(".vpet")) {
                        elegido
                    } else {
                        File(elegido.parentFile ?: carpetaInicial, "${elegido.name}.vpet")
                    }
                    onElegido(rutaFinal.name, rutaFinal.absolutePath)
                }
            }
        } catch (e: Exception) {
            println("Error abriendo selector destino de copia JVM: ${e.message}")
        }
    }
}

/**
 * La carpeta de Google Drive si está instalado "Drive para escritorio", o `null`.
 *
 * La app **no** habla con Drive por internet (no tiene permiso de red, a propósito): lo que hace es
 * empezar el diálogo de guardar en esa carpeta cuando existe, para que guardar la copia en Drive
 * sean dos clics. Se prueban los nombres que usa el cliente en español y en inglés, en el perfil del
 * usuario y en la raíz de cada unidad (Drive puede montarse como unidad virtual).
 */
private fun carpetaDeDrive(): File? {
    val casa = System.getProperty("user.home")
    val candidatas = listOf(
        File(casa, "Google Drive/My Drive"),
        File(casa, "Google Drive/Mi unidad"),
        File(casa, "Google Drive"),
        File(casa, "Mi unidad")
    )
    candidatas.firstOrNull { it.isDirectory }?.let { return it }

    return File.listRoots()?.firstOrNull { raiz ->
        raiz.path.contains("Google Drive", ignoreCase = true) && raiz.canRead()
    }
}
