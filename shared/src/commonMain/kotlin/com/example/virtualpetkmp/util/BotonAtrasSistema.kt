package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Engancha la vuelta atrás del sistema a una acción de la app.
 *
 * Existe porque la navegación de esta app no usa `NavHost`: es un `when` sobre una clave
 * (`"list"`, `"ficha"`, `"form"`) en `App.kt`, así que no hay una pila que "atrás" pueda
 * deshacer sola. Sin esto, el gesto o el botón del sistema cierra la app desde la ficha de una
 * mascota, que no es lo que espera nadie.
 *
 * En Android se conecta al `OnBackPressedDispatcher` (que es lo que además hace funcionar la
 * **vuelta atrás predictiva** de Android 13+); en escritorio no hace nada, porque allí no hay
 * botón del sistema y la navegación se hace con el botón de volver de la pantalla.
 *
 * @param onAtras qué hacer al pulsar atrás. Solo se registra si no es `null`.
 */
@Composable
expect fun BotonAtrasSistema(onAtras: (() -> Unit)?)
