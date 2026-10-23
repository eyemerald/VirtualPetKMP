package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

/**
 * Escritorio: copia el paquete a `~/.virtualpet/exportaciones` y lo abre con la aplicación que el
 * sistema tenga para ese tipo, que es desde donde se adjunta al correo o se sube a Drive.
 *
 * Si no hay escritorio no hay nada que abrir, pero el archivo queda copiado: se devuelve
 * `COMPARTIDO` para que el mensaje diga dónde está, en vez de un error que no lo es.
 */
@Composable
actual fun rememberCompartidorPaquete(): (String, String) -> ResultadoCompartir {
    return { nombreArchivo, rutaLocal ->
        try {
            val origen = File(rutaLocal)
            if (!origen.isFile) {
                ResultadoCompartir.ERROR
            } else {
                val carpeta = File(File(System.getProperty("user.home"), ".virtualpet"), "exportaciones")
                    .apply { mkdirs() }
                val destino = File(carpeta, nombreArchivo)
                origen.copyTo(destino, overwrite = true)

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(destino)
                }
                ResultadoCompartir.COMPARTIDO
            }
        } catch (e: Exception) {
            println("[CompartidorPaquete] error: ${e.message}")
            ResultadoCompartir.ERROR
        }
    }
}
