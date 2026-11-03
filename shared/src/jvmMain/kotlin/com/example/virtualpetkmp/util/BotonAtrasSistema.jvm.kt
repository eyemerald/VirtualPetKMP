package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Escritorio: no hay vuelta atrás del sistema, así que esto no hace nada.
 *
 * Se deja como función vacía en vez de no existir para que `App.kt` (código común) pueda llamarla
 * sin ramas por plataforma. La navegación de escritorio sigue con el botón de volver de la
 * pantalla y, en el formulario, con su propio botón.
 */
@Composable
actual fun BotonAtrasSistema(onAtras: (() -> Unit)?) {
}
