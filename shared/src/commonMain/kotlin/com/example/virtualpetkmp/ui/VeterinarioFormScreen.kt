package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinarioFormScreen(
    viewModel: VeterinarioViewModel,
    veterinarioId: Long? = null,
    onBack: () -> Unit
) {
    var nombreClinica by remember { mutableStateOf("") }
    var nombreVeterinario by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var esUrgencias by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val veterinarios by viewModel.veterinarios.collectAsState()

    LaunchedEffect(veterinarioId) {
        if (veterinarioId != null) {
            veterinarios.find { it.id == veterinarioId }?.let { veterinario ->
                nombreClinica = veterinario.nombreClinica
                nombreVeterinario = veterinario.nombreVeterinario ?: ""
                telefono = veterinario.telefono
                direccion = veterinario.direccion
                esUrgencias = veterinario.esUrgencias
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (veterinarioId == null) "Nuevo Veterinario" else "Editar Veterinario") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nombreClinica,
                onValueChange = { nombreClinica = it },
                label = { Text("Nombre de la clínica *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombreClinica.isBlank()
            )

            OutlinedTextField(
                value = nombreVeterinario,
                onValueChange = { nombreVeterinario = it },
                label = { Text("Nombre del veterinario (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = telefono.isBlank()
            )

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                isError = direccion.isBlank()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Switch(
                    checked = esUrgencias,
                    onCheckedChange = { esUrgencias = it }
                )
                Text("Es veterinario de urgencias 24h")
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (nombreClinica.isBlank() || telefono.isBlank() || direccion.isBlank()) {
                        viewModel.setError("Completa todos los campos obligatorios")
                        return@Button
                    }

                    val veterinario = Veterinario(
                        id = veterinarioId,
                        nombreClinica = nombreClinica,
                        nombreVeterinario = nombreVeterinario.ifBlank { null },
                        telefono = telefono,
                        direccion = direccion,
                        esUrgencias = esUrgencias
                    )

                    viewModel.saveVeterinario(veterinario) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (veterinarioId == null) "Guardar Veterinario" else "Actualizar Veterinario")
                }
            }
        }
    }
}
