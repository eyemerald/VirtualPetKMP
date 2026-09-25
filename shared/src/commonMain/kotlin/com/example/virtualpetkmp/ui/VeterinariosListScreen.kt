package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    val llamar = rememberLlamador()
    val abrirMapa = rememberAbridorMapa()

    val veterinariosUrgencias = veterinarios.filter { it.esUrgencias }
    val veterinariosNormales = veterinarios.filter { !it.esUrgencias }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veterinarios") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVeterinario) {
                Icon(Icons.Default.Add, contentDescription = "Agregar veterinario")
            }
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
                        contentPadding = PaddingValues(16.dp),
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
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = veterinario.nombreClinica,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = veterinario.telefono,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = veterinario.direccion,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { llamar(veterinario.telefono) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("LLAMAR URGENCIAS")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(
                                                onClick = { abrirMapa(veterinario.direccion) }
                                            ) {
                                                Text("Cómo llegar")
                                            }
                                            TextButton(
                                                onClick = { veterinario.id?.let { onVeterinarioClick(it) } }
                                            ) {
                                                Text("Editar")
                                            }
                                        }
                                    }
                                }
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
