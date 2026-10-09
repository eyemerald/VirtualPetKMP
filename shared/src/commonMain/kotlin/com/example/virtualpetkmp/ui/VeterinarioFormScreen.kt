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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_volver
import virtualpetkmp.shared.generated.resources.mascota_completar_campos
import virtualpetkmp.shared.generated.resources.vet_actualizar
import virtualpetkmp.shared.generated.resources.vet_campo_clinica
import virtualpetkmp.shared.generated.resources.vet_campo_direccion
import virtualpetkmp.shared.generated.resources.vet_campo_telefono
import virtualpetkmp.shared.generated.resources.vet_campo_urgencias
import virtualpetkmp.shared.generated.resources.vet_campo_veterinario
import virtualpetkmp.shared.generated.resources.vet_editar
import virtualpetkmp.shared.generated.resources.vet_guardar
import virtualpetkmp.shared.generated.resources.vet_nuevo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinarioFormScreen(
    viewModel: VeterinarioViewModel,
    veterinarioId: Long? = null,
    onBack: () -> Unit
) {
    var nombreClinica by rememberSaveable { mutableStateOf("") }
    var nombreVeterinario by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var esUrgencias by rememberSaveable { mutableStateOf(false) }

    // Texto resuelto aquí porque dentro del onClick no hay contexto composable.
    val textoCamposObligatorios = stringResource(Res.string.mascota_completar_campos)

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
                title = {
                    Text(
                        stringResource(
                            if (veterinarioId == null) Res.string.vet_nuevo else Res.string.vet_editar
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.a11y_volver))
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
                label = { Text(stringResource(Res.string.vet_campo_clinica)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombreClinica.isBlank()
            )

            OutlinedTextField(
                value = nombreVeterinario,
                onValueChange = { nombreVeterinario = it },
                label = { Text(stringResource(Res.string.vet_campo_veterinario)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text(stringResource(Res.string.vet_campo_telefono)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = telefono.isBlank()
            )

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text(stringResource(Res.string.vet_campo_direccion)) },
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
                Text(stringResource(Res.string.vet_campo_urgencias))
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
                        viewModel.setError(textoCamposObligatorios)
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
                    Text(
                        stringResource(
                            if (veterinarioId == null) Res.string.vet_guardar else Res.string.vet_actualizar
                        )
                    )
                }
            }
        }
    }
}
