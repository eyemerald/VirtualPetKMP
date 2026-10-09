package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Etiqueta regional del dispositivo, tal y como la reporta el sistema: "es-ES", "en-US", "de".
 *
 * Se implementa por plataforma en vez de leerla desde una API de Compose porque es un dato del
 * sistema operativo, no de la interfaz, y así queda explícito qué se consulta en cada caso.
 * Devuelve `null` si la plataforma no la sabe dar, y entonces se usa el idioma por defecto.
 */
@Composable
expect fun etiquetaRegionalDelSistema(): String?

/**
 * Configuración regional que corresponde al dispositivo.
 *
 * Es el único sitio de la app que debe decidir "¿en qué idioma y con qué unidades estamos?".
 * Todo lo demás lee el resultado desde `LocalLocalizacion`.
 */
@Composable
fun localizacionDelSistema(): Localizacion =
    Localizacion.desdeRegion(etiquetaRegionalDelSistema())
