package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.viewmodel.NotaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotasScreen(
    viewModel: NotaViewModel,
    onBack: () -> Unit
) {
    val notas by viewModel.notas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var textoActual by remember { mutableStateOf("") }
    var notaSeleccionadaId by remember { mutableStateOf<Long?>(null) }
    var notaAEliminar by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A tener en cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Toca una nota para editarla, o escribe una nueva abajo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (notas.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay notas todavía",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notas, key = { it.id ?: 0 }) { nota ->
                        NotaItem(
                            nota = nota,
                            seleccionada = nota.id == notaSeleccionadaId,
                            onClick = {
                                notaSeleccionadaId = nota.id
                                textoActual = nota.texto
                            },
                            onDelete = { notaAEliminar = nota.id }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = textoActual,
                onValueChange = { textoActual = it },
                label = { Text("Ej: es agresiva con otros perros machos") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val id = notaSeleccionadaId
                        if (id == null) {
                            viewModel.addNota(textoActual)
                        } else {
                            viewModel.updateNota(id, textoActual)
                        }
                        textoActual = ""
                        notaSeleccionadaId = null
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (notaSeleccionadaId == null) "Añadir nota" else "Guardar cambios")
                }

                if (notaSeleccionadaId != null) {
                    OutlinedButton(
                        onClick = {
                            textoActual = ""
                            notaSeleccionadaId = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    notaAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { notaAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar esta nota?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNota(id)
                    if (notaSeleccionadaId == id) {
                        notaSeleccionadaId = null
                        textoActual = ""
                    }
                    notaAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { notaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun NotaItem(
    nota: Nota,
    seleccionada: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorFondo = if (seleccionada) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val colorTexto = if (seleccionada) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
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
                    imageVector = Icons.Default.Note,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nota.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorTexto,
                    maxLines = 2
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar nota",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
