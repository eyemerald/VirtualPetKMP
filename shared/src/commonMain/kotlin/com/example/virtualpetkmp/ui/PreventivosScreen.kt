package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.PreventivoViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreventivosScreen(
    viewModel: PreventivoViewModel,
    onBack: () -> Unit
) {
    val preventivos by viewModel.preventivos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf<String?>(null) }
    var preventivoSeleccionado by remember { mutableStateOf<Preventivo?>(null) }
    var preventivoAEliminar by remember { mutableStateOf<Long?>(null) }

    val pipetas = preventivos.filter { it.tipo == "Pipeta" }
    val desparasitaciones = preventivos.filter { it.tipo == "Desparasitación" }
    val otros = preventivos.filter { it.tipo == "Otro" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preventivos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BloquePreventivo(
                    titulo = "Pipetas",
                    items = pipetas,
                    onAdd = { showAddDialog = "Pipeta" },
                    onEdit = { preventivoSeleccionado = it },
                    onDelete = { preventivoAEliminar = it.id }
                )
            }

            item {
                BloquePreventivo(
                    titulo = "Desparasitación",
                    items = desparasitaciones,
                    onAdd = { showAddDialog = "Desparasitación" },
                    onEdit = { preventivoSeleccionado = it },
                    onDelete = { preventivoAEliminar = it.id }
                )
            }

            item {
                BloquePreventivo(
                    titulo = "Otros preventivos",
                    items = otros,
                    onAdd = { showAddDialog = "Otro" },
                    onEdit = { preventivoSeleccionado = it },
                    onDelete = { preventivoAEliminar = it.id }
                )
            }

            if (errorMessage != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    showAddDialog?.let { tipo ->
        PreventivoDialog(
            tipo = tipo,
            preventivo = null,
            onDismiss = { showAddDialog = null },
            onSave = { tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote ->
                viewModel.addPreventivo(tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote)
                showAddDialog = null
            }
        )
    }

    preventivoSeleccionado?.let { preventivo ->
        PreventivoDialog(
            tipo = preventivo.tipo,
            preventivo = preventivo,
            onDismiss = { preventivoSeleccionado = null },
            onSave = { tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote ->
                viewModel.updatePreventivo(
                    preventivo.id!!,
                    tipo,
                    nombre,
                    fechaAplicacion,
                    fechaProximaDosis,
                    veterinario,
                    lote
                )
                preventivoSeleccionado = null
            }
        )
    }

    preventivoAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { preventivoAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar este preventivo?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePreventivo(id)
                    preventivoAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { preventivoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BloquePreventivo(
    titulo: String,
    items: List<Preventivo>,
    onAdd: () -> Unit,
    onEdit: (Preventivo) -> Unit,
    onDelete: (Preventivo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir")
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "Sin registros",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { preventivo ->
                        PreventivoItem(
                            preventivo = preventivo,
                            onClick = { onEdit(preventivo) },
                            onDelete = { onDelete(preventivo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreventivoItem(
    preventivo: Preventivo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val diasHastaProxima = calculateDaysBetween(hoy, preventivo.fechaProximaDosis)
    
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preventivo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preventivo.fechaProximaDosis.toFormatoEuropeo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreventivoDialog(
    tipo: String,
    preventivo: Preventivo?,
    onDismiss: () -> Unit,
    onSave: (String, String, kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate, String?, String?) -> Unit
) {
    var nombre by remember { mutableStateOf(preventivo?.nombre ?: if (tipo == "Otro") "" else tipo) }
    var fechaAplicacionStr by remember { mutableStateOf(
        preventivo?.fechaAplicacion?.toFormatoEuropeo() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
    )}
    var fechaProximaDosisStr by remember { mutableStateOf(
        preventivo?.fechaProximaDosis?.toFormatoEuropeo() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
    )}
    var veterinario by remember { mutableStateOf(preventivo?.veterinario ?: "") }
    var lote by remember { mutableStateOf(preventivo?.lote ?: "") }
    var errorFecha by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preventivo == null) "Añadir preventivo" else "Editar preventivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tipo == "Otro") {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = "Tipo: $tipo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
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
                    label = { Text("Veterinario (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    label = { Text("Lote (opcional)") },
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
                    val nombreFinal = if (tipo == "Otro") nombre else tipo
                    onSave(
                        tipo,
                        nombreFinal,
                        fechaAplicacion,
                        fechaProximaDosis,
                        veterinario.ifBlank { null },
                        lote.ifBlank { null }
                    )
                },
                enabled = (if (tipo == "Otro") nombre.isNotBlank() else true) && fechaAplicacionStr.isNotBlank() && fechaProximaDosisStr.isNotBlank()
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
