package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.logo

/** Opacidad de la marca de agua del logo, lo bastante baja para no molestar la lectura. */
private const val OPACIDAD_MARCA_DE_AGUA = 0.05f

/**
 * Contenedor raíz de la app: TopAppBar compacta contextual, pestañas globales
 * (Mascotas / Veterinarios) y el contenido de la pestaña activa.
 *
 * @param titulo texto de la TopAppBar; el llamador decide si es la marca o el nombre de
 *   la mascota según la pantalla activa.
 * @param onVolver si no es null, se muestra la flecha de volver a la izquierda.
 * @param onCompartir si no es null, se muestra la acción de compartir a la derecha.
 * @param acciones extras opcionales que se añaden antes del botón de compartir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    contenidoMascotas: @Composable () -> Unit,
    contenidoVeterinarios: @Composable () -> Unit,
    titulo: String = "VirtualPet",
    onVolver: (() -> Unit)? = null,
    onCompartir: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {}
) {
    var tabActual by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titulo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    acciones()
                    if (onCompartir != null) {
                        IconButton(onClick = onCompartir) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Pestañas globales, pegadas a la barra superior
                TabRow(selectedTabIndex = tabActual) {
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
                }
            }
        }
    }
}
