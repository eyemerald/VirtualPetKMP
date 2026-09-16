package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.rememberAbridorArchivo
import com.example.virtualpetkmp.util.rememberSelectorArchivo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.InformeViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformesScreen(
    viewModel: InformeViewModel,
    onBack: () -> Unit
) {
    val informes by viewModel.informes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var informeSeleccionado by remember { mutableStateOf<Informe?>(null) }
    var informeAEliminar by remember { mutableStateOf<Long?>(null) }

    val abrirArchivo = rememberAbridorArchivo()
    var errorAlAbrir by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informes y documentos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir informe")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (informes.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay informes registrados",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(informes, key = { it.id ?: 0 }) { informe ->
                        InformeItem(
                            informe = informe,
                            onEditar = { informeSeleccionado = informe },
                            onAbrir = {
                                val abierto = abrirArchivo(informe.rutaArchivo)
                                errorAlAbrir = !abierto
                            },
                            onDelete = { informeAEliminar = informe.id }
                        )
                    }
                }
            }

            if (errorAlAbrir) {
                Text(
                    text = "No se ha podido abrir el archivo (puede que se haya movido o borrado)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showAddDialog) {
        InformeDialog(
            informe = null,
            onDismiss = { showAddDialog = false },
            onSave = { tipo, descripcion, fecha, nombreArchivo, rutaArchivo ->
                viewModel.addInforme(tipo, descripcion, fecha, nombreArchivo, rutaArchivo)
                showAddDialog = false
            }
        )
    }

    informeSeleccionado?.let { informe ->
        InformeDialog(
            informe = informe,
            onDismiss = { informeSeleccionado = null },
            onSave = { tipo, descripcion, fecha, _, _ ->
                viewModel.updateInforme(
                    informe.id!!,
                    tipo,
                    descripcion,
                    fecha
                )
                informeSeleccionado = null
            }
        )
    }

    informeAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { informeAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar este informe?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteInforme(id)
                    informeAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { informeAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InformeItem(
    informe: Informe,
    onEditar: () -> Unit,
    onAbrir: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        onClick = onEditar
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = informe.tipo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = informe.fecha.toFormatoEuropeo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onAbrir) {
                Text("Abrir")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar informe",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InformeDialog(
    informe: Informe?,
    onDismiss: () -> Unit,
    onSave: (String, String?, kotlinx.datetime.LocalDate, String, String) -> Unit
) {
    var tipo by remember { mutableStateOf(informe?.tipo ?: "") }
    var descripcion by remember { mutableStateOf(informe?.descripcion ?: "") }
    var fechaStr by remember {
        mutableStateOf(
            informe?.fecha?.toFormatoEuropeo()
                ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
        )
    }
    var nombreArchivo by remember { mutableStateOf(informe?.nombreArchivo ?: "") }
    var rutaArchivo by remember { mutableStateOf(informe?.rutaArchivo ?: "") }
    var errorFecha by remember { mutableStateOf<String?>(null) }

    val seleccionarArchivo = rememberSelectorArchivo { nombre, ruta ->
        nombreArchivo = nombre
        rutaArchivo = ruta
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (informe == null) "Añadir informe" else "Editar informe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (ej: analítica, radiografía)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fechaStr,
                    onValueChange = { fechaStr = it },
                    label = { Text("Fecha (DD/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorFecha != null
                )
                if (errorFecha != null) {
                    Text(
                        text = errorFecha!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (informe == null) {
                    OutlinedButton(
                        onClick = seleccionarArchivo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (nombreArchivo.isBlank()) "Seleccionar archivo" else "Cambiar archivo")
                    }
                    if (nombreArchivo.isNotBlank()) {
                        Text(
                            text = "Archivo elegido: $nombreArchivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Archivo: $nombreArchivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fechaParsed = try {
                        parseFormatoEuropeo(fechaStr)
                    } catch (e: Exception) {
                        errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                        return@TextButton
                    }
                    onSave(tipo, descripcion.ifBlank { null }, fechaParsed, nombreArchivo, rutaArchivo)
                },
                enabled = tipo.isNotBlank() && fechaStr.isNotBlank() &&
                    (informe != null || (nombreArchivo.isNotBlank() && rutaArchivo.isNotBlank()))
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
