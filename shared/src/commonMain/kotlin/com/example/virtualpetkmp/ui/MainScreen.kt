package com.example.virtualpetkmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_volver
import virtualpetkmp.shared.generated.resources.logo
import virtualpetkmp.shared.generated.resources.pestana_mascotas
import virtualpetkmp.shared.generated.resources.pestana_veterinarios

/** Opacidad de la marca de agua del logo, lo bastante baja para no molestar la lectura. */
private const val OPACIDAD_MARCA_DE_AGUA = 0.05f

/**
 * Contenedor raíz de la app: pestañas globales (Mascotas / Veterinarios) y el contenido de
 * la pestaña activa.
 *
 * No hay barra superior: el único botón superpuesto es el de **volver**, que aparece como
 * botón flotante circular en la esquina superior izquierda del detalle. Las demás acciones de
 * la ficha (compartir, editar) viven en su menú de tres puntos, para no llenar de iconos
 * sueltos el contenido.
 *
 * @param mostrarAcciones true solo en el detalle de una mascota con la pestaña Mascotas
 *   activa. Con la pestaña de Veterinarios se oculta y la lista ocupa toda la pantalla.
 * @param onVolver acción del botón flotante de volver.
 */
@Composable
fun MainScreen(
    contenidoMascotas: @Composable () -> Unit,
    contenidoVeterinarios: @Composable () -> Unit,
    mostrarAcciones: Boolean = false,
    onVolver: (() -> Unit)? = null
) {
    var tabActual by rememberSaveable { mutableStateOf(0) }
    val accionesVisibles = mostrarAcciones && tabActual == 0

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Las pestañas son lo primero, respetando el safe area de arriba para no
                // chocar con el reloj ni la cámara.
                TabRow(
                    selectedTabIndex = tabActual,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                ) {
                    Tab(
                        selected = tabActual == 0,
                        onClick = { tabActual = 0 },
                        text = { Text(stringResource(Res.string.pestana_mascotas)) }
                    )
                    Tab(
                        selected = tabActual == 1,
                        onClick = { tabActual = 1 },
                        text = { Text(stringResource(Res.string.pestana_veterinarios)) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Marca de agua: el logo de la app, muy tenue, detrás del contenido
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.62f)
                            .alpha(OPACIDAD_MARCA_DE_AGUA)
                    )

                    when (tabActual) {
                        0 -> contenidoMascotas()
                        1 -> contenidoVeterinarios()
                    }

                    // Botón flotante de volver, superpuesto sobre el contenido y por debajo
                    // de las pestañas (que son la navegación global).
                    //
                    // El safe area ya viene aplicado por el `Scaffold` de arriba (su
                    // `contentWindowInsets` es `safeDrawing`), así que aquí **no** se vuelve a
                    // pedir: hacerlo otra vez empujaría el botón hacia abajo y quedaría
                    // descolgado respecto a las pestañas.
                    if (accionesVisibles && onVolver != null) {
                        BotonFlotanteVolver(
                            onClick = onVolver,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botón flotante de volver: un círculo con fondo claro, filo suave y sombra.
 *
 * Antes era solo el icono suelto sobre el contenido, y se perdía entre la marca de agua y las
 * tarjetas. El círculo le da la forma de botón que se espera de un elemento flotante, y la
 * sombra lo despega del fondo sin necesidad de una barra superior.
 */
@Composable
private fun BotonFlotanteVolver(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        // Los colores del tema garantizan el contraste en claro y en oscuro.
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.a11y_volver),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
