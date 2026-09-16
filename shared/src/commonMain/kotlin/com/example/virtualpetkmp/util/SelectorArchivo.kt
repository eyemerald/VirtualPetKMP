package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que, al invocarla, abre el selector de archivos nativo
 * de la plataforma. Si el usuario elige un archivo, se copia a una carpeta
 * propia de la app y se llama a onArchivoSeleccionado con su nombre original
 * y la ruta donde ha quedado guardada la copia.
 */
@Composable
expect fun rememberSelectorArchivo(
    onArchivoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit
