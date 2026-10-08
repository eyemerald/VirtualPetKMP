package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.util.rememberAbridorMapa
import com.example.virtualpetkmp.util.rememberLlamador
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinariosListScreen(
    viewModel: VeterinarioViewModel,
    onVeterinarioClick: (Long) -> Unit,
    onAddVeterinario: () -> Unit
) {
    val veterinarios by viewModel.veterinarios.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf<Long?>(null) }

    val llamar = rememberLlamador()
    val abrirMapa = rememberAbridorMapa()

    val veterinariosUrgencias = veterinarios.filter { it.esUrgencias }
    val veterinariosNormales = veterinarios.filter { !it.esUrgencias }

    Scaffold(
        // Sin TopAppBar: el título se dibuja dentro de la lista, pegado a las pestañas.
        // El Scaffold se mantiene por el botón flotante y por el hueco del banner.
        //
        // contentWindowInsets = 0 a propósito: esta pantalla se dibuja DENTRO del Scaffold
        // de MainScreen, que ya aplica los insets del sistema. Si el Scaffold de aquí
        // volviera a aplicarlos, se sumarían y aparecería un hueco muerto arriba.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVeterinario) {
                Icon(Icons.Default.Add, contentDescription = "Agregar veterinario")
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
                            viewModel.loadVeterinarios()
                        }) {
                            Text("Reintentar")
                        }
                    }
                }
                veterinarios.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { abrirMapa("veterinarios cerca de mí") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Veterinarios cercanos")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No hay veterinarios registrados",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca el botón + para agregar uno",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Sin título dentro: ya lo pone la barra superior global, así que la
                        // lista empieza directamente con el contenido. El hueco del banner va
                        // aparte (bottomBar), para que no se desplace con el scroll.
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            OutlinedButton(
                                onClick = { abrirMapa("veterinarios cerca de mí") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Veterinarios cercanos")
                            }
                        }

                        if (veterinariosUrgencias.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Urgencias 24h",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(veterinariosUrgencias, key = { it.id ?: 0 }) { veterinario ->
                                VeterinarioUrgenciaCard(
                                    veterinario = veterinario,
                                    onLlamar = { llamar(veterinario.telefono) },
                                    onAbrirMapa = { abrirMapa(veterinario.direccion) },
                                    onEditar = { veterinario.id?.let { onVeterinarioClick(it) } }
                                )
                            }
                        }

                        if (veterinariosNormales.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Todos los veterinarios",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(veterinariosNormales, key = { it.id ?: 0 }) { veterinario ->
                                VeterinarioCard(
                                    veterinario = veterinario,
                                    onClick = { veterinario.id?.let { onVeterinarioClick(it) } },
                                    onEdit = { veterinario.id?.let { onVeterinarioClick(it) } },
                                    onDelete = { veterinario.id?.let { showDeleteDialog = it } },
                                    onLlamar = { llamar(veterinario.telefono) },
                                    onAbrirMapa = { abrirMapa(veterinario.direccion) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este veterinario? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVeterinario(id) {
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta de un veterinario de urgencias 24h: mismo tamaño que las demás, con los datos a
 * la izquierda y un **botón circular rojo de llamada** a la derecha. Antes era una tarjeta
 * roja entera con un botón "LLAMAR URGENCIAS" a ancho completo que ocupaba media pantalla y
 * dejaba sin ver al resto de veterinarios.
 *
 * El rojo se reserva para el botón de llamar, que es la acción de urgencia: el nombre de la
 * clínica sigue en su color para no perder legibilidad.
 */
@Composable
fun VeterinarioUrgenciaCard(
    veterinario: Veterinario,
    onLlamar: () -> Unit,
    onAbrirMapa: () -> Unit,
    onEditar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = veterinario.nombreClinica,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                veterinario.nombreVeterinario?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = veterinario.telefono,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = veterinario.direccion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onAbrirMapa) {
                        Icon(Icons.Default.Place, contentDescription = "Mapa")
                    }
                    IconButton(onClick = onEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de llamada: círculo rojo relleno con el teléfono en blanco
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onLlamar),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Llamar a urgencias",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
