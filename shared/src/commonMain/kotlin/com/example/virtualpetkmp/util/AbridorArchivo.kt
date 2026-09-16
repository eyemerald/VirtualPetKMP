package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que, dada una ruta de archivo, intenta abrirlo con
 * la aplicación por defecto del sistema. Devuelve true si se pudo abrir,
 * false si el archivo no existe o no se pudo lanzar.
 */
@Composable
expect fun rememberAbridorArchivo(): (rutaArchivo: String) -> Boolean
