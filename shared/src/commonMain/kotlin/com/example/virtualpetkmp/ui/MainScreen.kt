package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.logo

/** Opacidad de la marca de agua del logo, lo bastante baja para no molestar la lectura. */
private const val OPACIDAD_MARCA_DE_AGUA = 0.05f

/**
 * Contenedor raíz de la app: pestañas globales (Mascotas / Veterinarios) y el contenido de
 * la pestaña activa.
 *
 * No hay barra superior: las acciones de la ficha van como iconos flotantes superpuestos
 * sobre el contenido, y solo se muestran cuando tienen sentido (ver [mostrarAcciones]).
 *
 * @param mostrarAcciones true solo en el detalle de una mascota con la pestaña Mascotas
 *   activa. Con la pestaña de Veterinarios se ocultan y la lista ocupa toda la pantalla.
 * @param onVolver acción del icono flotante de volver.
 * @param onCompartir acción del icono flotante de compartir.
 */
@Composable
fun MainScreen(
    contenidoMascotas: @Composable () -> Unit,
    contenidoVeterinarios: @Composable () -> Unit,
    mostrarAcciones: Boolean = false,
    onVolver: (() -> Unit)? = null,
    onCompartir: (() -> Unit)? = null
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
                        text = { Text("Mascotas") }
                    )
                    Tab(
                        selected = tabActual == 1,
                        onClick = { tabActual = 1 },
                        text = { Text("Veterinarios") }
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

                    // Iconos flotantes de la ficha, superpuestos sobre el contenido y por
                    // debajo de las pestañas (que son la navegación global).
                    if (accionesVisibles) {
                        if (onVolver != null) {
                            IconoFlotante(
                                icono = Icons.AutoMirrored.Filled.ArrowBack,
                                descripcion = "Volver",
                                onClick = onVolver,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                        if (onCompartir != null) {
                            IconoFlotante(
                                icono = Icons.Default.Share,
                                descripcion = "Compartir",
                                onClick = onCompartir,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Botón redondo superpuesto, con fondo semitransparente para que se lea sobre el contenido
 * que quede debajo.
 */
@Composable
private fun IconoFlotante(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
    ) {
        Icon(imageVector = icono, contentDescription = descripcion)
    }
}
