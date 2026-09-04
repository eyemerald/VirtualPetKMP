package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MascotaFormScreen(
    viewModel: MascotaViewModel,
    mascotaId: Long? = null,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("") }
    var raza by remember { mutableStateOf("") }
    var fechaNacimientoStr by remember { mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()) }
    var sexo by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var microchip by remember { mutableStateOf("") }
    

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val mascotas by viewModel.mascotas.collectAsState()

    // Load existing mascota data if editing
    LaunchedEffect(mascotaId) {
        if (mascotaId != null) {
            mascotas.find { it.id == mascotaId }?.let { mascota ->
                nombre = mascota.nombre
                especie = mascota.especie
                raza = mascota.raza
                fechaNacimientoStr = mascota.fechaNacimiento.toString()
                sexo = mascota.sexo
                color = mascota.color
                microchip = mascota.microchip ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mascotaId == null) "Nueva Mascota" else "Editar Mascota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombre.isBlank()
            )

            OutlinedTextField(
                value = especie,
                onValueChange = { especie = it },
                label = { Text("Especie *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = especie.isBlank()
            )

            OutlinedTextField(
                value = raza,
                onValueChange = { raza = it },
                label = { Text("Raza *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = raza.isBlank()
            )

            OutlinedTextField(
                            value = fechaNacimientoStr,
                            onValueChange = { fechaNacimientoStr = it },
                            label = { Text("Fecha de Nacimiento (YYYY-MM-DD) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = fechaNacimientoStr.isBlank()
                        )

            OutlinedTextField(
                value = sexo,
                onValueChange = { sexo = it },
                label = { Text("Sexo *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = sexo.isBlank()
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = color.isBlank()
            )

            OutlinedTextField(
                value = microchip,
                onValueChange = { microchip = it },
                label = { Text("Microchip (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (errorMessage != null) {
                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (nombre.isBlank() || especie.isBlank() || raza.isBlank() || 
                        sexo.isBlank() || color.isBlank()) {
                        viewModel.clearError()
                        return@Button
                    }

                    val mascota = Mascota(
                        id = mascotaId,
                        nombre = nombre,
                        especie = especie,
                        raza = raza,
                                            fechaNacimiento = kotlinx.datetime.LocalDate.parse(fechaNacimientoStr),
                        sexo = sexo,
                        color = color,
                        microchip = microchip.ifBlank { null }
                    )

                    viewModel.saveMascota(mascota) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colors.onPrimary
                    )
                } else {
                    Text(if (mascotaId == null) "Guardar Mascota" else "Actualizar Mascota")
                }
            }
        }
    }

}
