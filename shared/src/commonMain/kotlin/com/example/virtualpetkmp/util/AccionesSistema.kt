package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que intenta abrir el marcador telefónico con el número dado.
 * Devuelve true si se pudo abrir, false si hubo error.
 */
@Composable
expect fun rememberLlamador(): (telefono: String) -> Boolean

/**
 * Devuelve una función que intenta abrir Google Maps con la búsqueda indicada.
 * El parámetro query puede ser una dirección o una búsqueda como "veterinarios cercanos".
 * Devuelve true si se pudo abrir, false si hubo error.
 */
@Composable
expect fun rememberAbridorMapa(): (query: String) -> Boolean
