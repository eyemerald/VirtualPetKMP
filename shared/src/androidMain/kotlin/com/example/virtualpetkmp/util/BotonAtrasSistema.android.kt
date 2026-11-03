package com.example.virtualpetkmp.util

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android: se apunta al `OnBackPressedDispatcher` de la actividad.
 *
 * Con `enabled = false` cuando no hay nada a lo que volver, el gesto sigue cerrando la app como
 * siempre (por ejemplo, estando ya en la lista de mascotas).
 */
@Composable
actual fun BotonAtrasSistema(onAtras: (() -> Unit)?) {
    BackHandler(enabled = onAtras != null, onBack = { onAtras?.invoke() })
}
