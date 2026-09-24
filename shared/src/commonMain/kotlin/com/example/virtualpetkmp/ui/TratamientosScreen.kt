package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.TratamientoViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TratamientosScreen(
    viewModel: TratamientoViewModel,
    onBack: () -> Unit
) {
    val tratamientos by viewModel.tratamientos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var tratamientoSeleccionado by remember { mutableStateOf<Tratamiento?>(null) }
    var tratamientoAEliminar by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tratamientos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir tratamiento")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (tratamientos.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay tratamientos registrados",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tratamientos, key = { it.id ?: 0 }) { tratamiento ->
                        TratamientoItem(
                            tratamiento = tratamiento,
                            onClick = { tratamientoSeleccionado = tratamiento },
                            onDelete = { tratamientoAEliminar = tratamiento.id }
                        )
                    }
                }
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
        TratamientoDialog(
            tratamiento = null,
            onDismiss = { showAddDialog = false },
            onSave = { nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin ->
                viewModel.addTratamiento(nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin)
                showAddDialog = false
            }
        )
    }

    tratamientoSeleccionado?.let { tratamiento ->
        TratamientoDialog(
            tratamiento = tratamiento,
            onDismiss = { tratamientoSeleccionado = null },
            onSave = { nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin ->
                viewModel.updateTratamiento(
                    tratamiento.id!!,
                    nombreMedicamento,
                    dosis,
                    frecuencia,
                    fechaInicio,
                    fechaFin
                )
                tratamientoSeleccionado = null
            }
        )
    }

    tratamientoAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { tratamientoAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar este tratamiento?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTratamiento(id)
                    tratamientoAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { tratamientoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TratamientoItem(
    tratamiento: Tratamiento,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val esActivo = tratamiento.fechaFin == null || tratamiento.fechaFin > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    val colorFondo = if (esActivo) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val colorTexto = if (esActivo) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        onClick = onClick
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
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tratamiento.nombreMedicamento,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tratamiento.fechaInicio.toFormatoEuropeo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar tratamiento",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TratamientoDialog(
    tratamiento: Tratamiento?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate?) -> Unit
) {
    var nombreMedicamento by remember { mutableStateOf(tratamiento?.nombreMedicamento ?: "") }
    var dosis by remember { mutableStateOf(tratamiento?.dosis ?: "") }
    var frecuencia by remember { mutableStateOf(tratamiento?.frecuencia ?: "") }
    var fechaInicioStr by remember { mutableStateOf(
        tratamiento?.fechaInicio?.toFormatoEuropeo() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
    )}
    var fechaFinStr by remember { mutableStateOf(tratamiento?.fechaFin?.toFormatoEuropeo() ?: "") }
    var esCronico by remember { mutableStateOf(tratamiento?.fechaFin == null) }
    var errorFecha by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tratamiento == null) "Añadir tratamiento" else "Editar tratamiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombreMedicamento,
                    onValueChange = { nombreMedicamento = it },
                    label = { Text("Medicamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text("Dosis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text("Frecuencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = fechaInicioStr,
                    onValorCambia = { fechaInicioStr = it },
                    etiqueta = "Fecha inicio (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = errorFecha != null
                )
                if (errorFecha != null) {
                    Text(
                        text = errorFecha!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = esCronico,
                        onCheckedChange = { 
                            esCronico = it
                            if (it) fechaFinStr = ""
                        }
                    )
                    Text("Sin fecha fin (crónico)")
                }
                if (!esCronico) {
                    CampoFecha(
                        valor = fechaFinStr,
                        onValorCambia = { fechaFinStr = it },
                        etiqueta = "Fecha fin (DD/MM/AAAA)",
                        modifier = Modifier.fillMaxWidth(),
                        esError = errorFecha != null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fechaInicioParsed = try {
                        parseFormatoEuropeo(fechaInicioStr)
                    } catch (e: Exception) {
                        errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                        return@TextButton
                    }
                    val fechaFinParsed = if (!esCronico && fechaFinStr.isNotBlank()) {
                        try {
                            parseFormatoEuropeo(fechaFinStr)
                        } catch (e: Exception) {
                            errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                            return@TextButton
                        }
                    } else null
                    onSave(
                        nombreMedicamento,
                        dosis.ifBlank { null },
                        frecuencia.ifBlank { null },
                        fechaInicioParsed,
                        fechaFinParsed
                    )
                },
                enabled = nombreMedicamento.isNotBlank() && fechaInicioStr.isNotBlank()
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

