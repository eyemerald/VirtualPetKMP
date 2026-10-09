package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.util.formatearKilos
import com.example.virtualpetkmp.viewmodel.MascotaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaListScreen(
    viewModel: MascotaViewModel,
    onMascotaClick: (Long) -> Unit,
    onAddMascota: () -> Unit
) {
    val mascotas by viewModel.mascotas.collectAsState()
    val ultimoPesoPorMascota by viewModel.ultimoPesoPorMascota.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf<Long?>(null) }
    var nombreAEliminar by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        // Sin TopAppBar: el título se dibuja dentro de la lista, pegado a las pestañas.
        // El Scaffold se mantiene por el botón flotante y por el hueco del banner.
        //
        // contentWindowInsets = 0 a propósito: esta pantalla se dibuja DENTRO del Scaffold
        // de MainScreen, que ya aplica los insets del sistema. Si el Scaffold de aquí
        // volviera a aplicarlos, se sumarían y aparecería un hueco muerto arriba.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMascota) {
                Icon(Icons.Default.Add, contentDescription = "Agregar mascota")
            }
        },
        // Hueco FIJO del banner de publicidad: el Scaffold lo coloca fuera del área de
        // contenido, así que queda anclado al fondo y la lista se desplaza por encima.
        // El inset de la barra de navegación lo añade el propio Scaffold.
        bottomBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_RESERVA_BANNER)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.clearError()
                            viewModel.loadMascotas()
                        }) {
                            Text("Reintentar")
                        }
                    }
                }
                mascotas.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No hay mascotas registradas",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca el botón + para agregar una",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Sin título dentro: ya lo pone la barra superior global, así que la
                        // lista empieza directamente con las tarjetas. El hueco del banner va
                        // aparte (bottomBar), para que no se desplace con el scroll.
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mascotas, key = { it.id ?: 0 }) { mascota ->
                            MascotaCard(
                                mascota = mascota,
                                ultimoPeso = mascota.id?.let { ultimoPesoPorMascota[it] },
                                onClick = { mascota.id?.let { onMascotaClick(it) } },
                                onEdit = { mascota.id?.let { onMascotaClick(it) } },
                                onDelete = {
                                    nombreAEliminar = mascota.nombre
                                    mascota.id?.let { showDeleteDialog = it }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar mascota") },
            text = {
                Text(
                    nombreAEliminar?.let { "¿Seguro que quieres eliminar a $it? Esta acción no se puede deshacer." }
                        ?: "¿Seguro que quieres eliminar esta mascota? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMascota(id) {
                            showDeleteDialog = null
                            nombreAEliminar = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    nombreAEliminar = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta de una mascota en la lista: avatar, identidad y, si la mascota tiene peso ideal y
 * algún pesaje, el último peso con un indicador ▲/▼ de si está por encima o por debajo.
 *
 * Las acciones (editar / eliminar) van en un único menú de tres puntos para que la tarjeta
 * quede limpia y el nombre tenga más sitio.
 */
@Composable
fun MascotaCard(
    mascota: Mascota,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    ultimoPeso: Double? = null
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de la mascota (o su inicial si aún no tiene): el mismo avatar que en la
            // ficha, en pequeño.
            AvatarMascota(
                nombre = mascota.nombre,
                rutaFoto = mascota.foto,
                tamano = 56.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Un punto más grande que antes: al quitar los dos iconos de acción sobra
                // sitio horizontal y el nombre gana protagonismo.
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${mascota.especie} - ${mascota.raza}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mascota.sexo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                // Solo se muestra si hay peso ideal configurado Y algún pesaje: sin uno de
                // los dos datos, un triángulo no significaría nada.
                val ideal = mascota.pesoIdeal
                if (ideal != null && ultimoPeso != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IndicadorPeso(
                        ultimoPeso = ultimoPeso,
                        pesoIdeal = ideal
                    )
                }
            }

            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Acciones de ${mascota.nombre}"
                    )
                }

                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar mascota") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuAbierto = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Eliminar mascota", color = MaterialTheme.colorScheme.error)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Último peso junto a su peso ideal: ▲ verde si está por encima, ▼ rojo si está por debajo.
 * Si coinciden (margen de 10 gramos), no se dibuja flecha: no hay nada que indicar.
 */
@Composable
private fun IndicadorPeso(
    ultimoPeso: Double,
    pesoIdeal: Double
) {
    val diferencia = ultimoPeso - pesoIdeal
    val hayDiferencia = kotlin.math.abs(diferencia) > 0.01

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${formatearKilos(ultimoPeso)} kg",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        if (hayDiferencia) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (diferencia > 0) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (diferencia > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "ideal ${formatearKilos(pesoIdeal)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
