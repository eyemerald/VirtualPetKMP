package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.base_mas_nueva_mensaje
import virtualpetkmp.shared.generated.resources.base_mas_nueva_titulo

/**
 * Aviso que se enseña cuando la base de datos es **de una versión más nueva** que la app.
 *
 * Pasa al instalar una versión anterior encima de una que ya actualizó los datos (volver a un
 * APK viejo). La app no puede leer un esquema que no conoce, así que no toca nada y enseña esto.
 * Antes la lista mostraba el mensaje de la excepción, en español y sin traducir.
 *
 * **Dónde se usa:** en Android, desde `App` (la composición existe desde el arranque, así que aquí
 * sí se puede pintar). En escritorio **no** se usa: el arranque de `desktopApp` revisa la versión
 * antes de abrir la ventana y enseña un diálogo del sistema (`avisarSiLaBaseEsMasNueva`), porque
 * cortar la composición nada más empezar dejaba la ventana en blanco (comprobado).
 *
 * El texto sale de los recursos (los seis idiomas de la app) y lleva las dos versiones: la del
 * fichero y la que entiende esta app.
 *
 * **Sin `Scaffold` a propósito:** esta pantalla se pinta cuando el arranque se corta, fuera del
 * árbol normal de la app, y no necesita barra ni botones.
 */
@Composable
fun PantallaBaseMasNueva(versionFichero: Long, versionApp: Long) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.base_mas_nueva_titulo),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    Res.string.base_mas_nueva_mensaje,
                    versionFichero,
                    versionApp
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
