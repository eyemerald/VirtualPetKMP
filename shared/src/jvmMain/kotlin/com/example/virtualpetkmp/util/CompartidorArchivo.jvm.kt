package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

/**
 * Escritorio: no existe una hoja de compartir del sistema, así que se deja el PDF en la carpeta
 * de exportaciones y se abre con la aplicación por defecto. Desde ahí se adjunta al correo o se
 * sube donde haga falta, que es lo más parecido a "compartir" que ofrece el sistema.
 */
@Composable
actual fun rememberCompartidorArchivo(): (String, ByteArray) -> ResultadoCompartir {
    return { nombreArchivo, contenido ->
        try {
            val carpeta = File(File(System.getProperty("user.home"), ".virtualpet"), "exportaciones")
            carpeta.mkdirs()
            val archivo = File(carpeta, nombreArchivo)
            archivo.writeBytes(contenido)

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo)
                ResultadoCompartir.COMPARTIDO
            } else {
                // Sin escritorio no hay con qué abrirlo, pero el archivo queda guardado.
                ResultadoCompartir.SIN_APLICACION
            }
        } catch (e: Exception) {
            ResultadoCompartir.ERROR
        }
    }
}
