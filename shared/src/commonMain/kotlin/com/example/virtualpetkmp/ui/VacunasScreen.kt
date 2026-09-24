package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.VacunaViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacunasScreen(
    viewModel: VacunaViewModel,
    onBack: () -> Unit
) {
    val vacunas by viewModel.vacunas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var vacunaSeleccionada by remember { mutableStateOf<Vacuna?>(null) }
    var vacunaAEliminar by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vacunas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir vacuna")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (vacunas.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay vacunas registradas",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vacunas, key = { it.id ?: 0 }) { vacuna ->
                        VacunaItem(
                            vacuna = vacuna,
                            onClick = { vacunaSeleccionada = vacuna },
                            onDelete = { vacunaAEliminar = vacuna.id }
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
        VacunaDialog(
            vacuna = null,
            onDismiss = { showAddDialog = false },
            onSave = { nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote ->
                viewModel.addVacuna(nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote)
                showAddDialog = false
            }
        )
    }

    vacunaSeleccionada?.let { vacuna ->
        VacunaDialog(
            vacuna = vacuna,
            onDismiss = { vacunaSeleccionada = null },
            onSave = { nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote ->
                viewModel.updateVacuna(
                    vacuna.id!!,
                    nombre,
                    fechaAplicacion,
                    fechaProximaDosis,
                    veterinario,
                    lote
                )
                vacunaSeleccionada = null
            }
        )
    }

    vacunaAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { vacunaAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar esta vacuna?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVacuna(id)
                    vacunaAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { vacunaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun VacunaItem(
    vacuna: Vacuna,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val diasHastaProxima = calculateDaysBetween(hoy, vacuna.fechaProximaDosis)
    
    val colorFondo = when {
        diasHastaProxima < 0 -> MaterialTheme.colorScheme.errorContainer
        diasHastaProxima <= 30 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val colorTexto = when {
        diasHastaProxima < 0 -> MaterialTheme.colorScheme.onErrorContainer
        diasHastaProxima <= 30 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
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
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vacuna.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vacuna.fechaProximaDosis.toFormatoEuropeo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar vacuna",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacunaDialog(
    vacuna: Vacuna?,
    onDismiss: () -> Unit,
    onSave: (String, kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate, String?, String?) -> Unit
) {
    var nombre by remember { mutableStateOf(vacuna?.nombre ?: "") }
    var fechaAplicacionStr by remember { mutableStateOf(
        vacuna?.fechaAplicacion?.toFormatoEuropeo() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
    )}
    var fechaProximaDosisStr by remember { mutableStateOf(
        vacuna?.fechaProximaDosis?.toFormatoEuropeo() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
    )}
    var veterinario by remember { mutableStateOf(vacuna?.veterinario ?: "") }
    var lote by remember { mutableStateOf(vacuna?.lote ?: "") }
    var errorFecha by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vacuna == null) "Añadir vacuna" else "Editar vacuna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la vacuna") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = fechaAplicacionStr,
                    onValorCambia = { fechaAplicacionStr = it },
                    etiqueta = "Fecha de aplicación (DD/MM/AAAA)",
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
                CampoFecha(
                    valor = fechaProximaDosisStr,
                    onValorCambia = { fechaProximaDosisStr = it },
                    etiqueta = "Próxima dosis (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = errorFecha != null
                )
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    label = { Text("Veterinario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    label = { Text("Lote") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fechaAplicacion = try {
                        parseFormatoEuropeo(fechaAplicacionStr)
                    } catch (e: Exception) {
                        errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                        return@TextButton
                    }
                    val fechaProximaDosis = try {
                        parseFormatoEuropeo(fechaProximaDosisStr)
                    } catch (e: Exception) {
                        errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                        return@TextButton
                    }
                    onSave(
                        nombre,
                        fechaAplicacion,
                        fechaProximaDosis,
                        veterinario.ifBlank { null },
                        lote.ifBlank { null }
                    )
                },
                enabled = nombre.isNotBlank() && fechaAplicacionStr.isNotBlank() && fechaProximaDosisStr.isNotBlank()
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

private fun calculateDaysBetween(from: kotlinx.datetime.LocalDate, to: kotlinx.datetime.LocalDate): Int {
    return (to.toEpochDays() - from.toEpochDays()).toInt()
}
