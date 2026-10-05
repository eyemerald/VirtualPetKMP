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

/**
 * Devuelve una función que, al invocarla, abre el diálogo nativo para GUARDAR
 * un archivo nuevo. Se usa para exportar el PDF de la ficha de mascota.
 * El callback recibe el nombre y la ruta donde se guardará el archivo.
 * La ruta en Android es un content:// URI que luego hay que resolver con
 * el guardador de archivos existente.
 */
@Composable
expect fun rememberSelectorDestino(
    nombreSugerido: String,
    onDestinoSeleccionado: (nombreArchivo: String, rutaArchivo: String) -> Unit
): () -> Unit
