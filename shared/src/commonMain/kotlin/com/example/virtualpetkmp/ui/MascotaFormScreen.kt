package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.util.ESPECIES
import com.example.virtualpetkmp.util.RAZAS_POR_ESPECIE
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaFormScreen(
    viewModel: MascotaViewModel,
    mascotaId: Long? = null,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    var especieSeleccionada by remember { mutableStateOf(ESPECIES.first()) }
    var especieRazaOtroTexto by remember { mutableStateOf("") }

    var razaSeleccionada by remember { mutableStateOf("") }
    var razaPersonalizadaTexto by remember { mutableStateOf("") }

    var sexo by remember { mutableStateOf("Macho") }

    var fechaNacimientoStr by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
        )
    }
    var color by remember { mutableStateOf("") }
    var microchip by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val mascotas by viewModel.mascotas.collectAsState()

    // Cargar los datos de la mascota existente si se está editando
    LaunchedEffect(mascotaId) {
        if (mascotaId != null) {
            mascotas.find { it.id == mascotaId }?.let { mascota ->
                nombre = mascota.nombre
                fechaNacimientoStr = mascota.fechaNacimiento.toFormatoEuropeo()
                sexo = mascota.sexo
                color = mascota.color
                microchip = mascota.microchip ?: ""

                if (mascota.especie in ESPECIES && mascota.especie != "Otro") {
                    especieSeleccionada = mascota.especie
                    val razasDeEstaEspecie = RAZAS_POR_ESPECIE[mascota.especie].orEmpty()
                    if (mascota.raza in razasDeEstaEspecie) {
                        razaSeleccionada = mascota.raza
                    } else {
                        razaSeleccionada = "Otra"
                        razaPersonalizadaTexto = mascota.raza
                    }
                } else {
                    especieSeleccionada = "Otro"
                    especieRazaOtroTexto = if (mascota.raza.isNotBlank()) {
                        "${mascota.especie} - ${mascota.raza}"
                    } else {
                        mascota.especie
                    }
                }
            }
        }
    }

    // Al cambiar de especie, selecciona la primera raza disponible por defecto
    LaunchedEffect(especieSeleccionada) {
        val razas = RAZAS_POR_ESPECIE[especieSeleccionada].orEmpty()
        if (razaSeleccionada !in razas) {
            razaSeleccionada = razas.firstOrNull() ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mascotaId == null) "Nueva Mascota" else "Editar Mascota") },
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombre.isBlank()
            )

            // Especie
            var expandedEspecie by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedEspecie,
                onExpandedChange = { expandedEspecie = it }
            ) {
                OutlinedTextField(
                    value = especieSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Especie *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEspecie) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedEspecie,
                    onDismissRequest = { expandedEspecie = false }
                ) {
                    ESPECIES.forEach { especie ->
                        DropdownMenuItem(
                            text = { Text(especie) },
                            onClick = {
                                especieSeleccionada = especie
                                expandedEspecie = false
                            }
                        )
                    }
                }
            }

            if (especieSeleccionada == "Otro") {
                OutlinedTextField(
                    value = especieRazaOtroTexto,
                    onValueChange = { especieRazaOtroTexto = it },
                    label = { Text("Especifica especie y raza *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = especieRazaOtroTexto.isBlank()
                )
            } else {
                // Raza (depende de la especie elegida)
                var expandedRaza by remember { mutableStateOf(false) }
                val razasDisponibles = RAZAS_POR_ESPECIE[especieSeleccionada].orEmpty()

                ExposedDropdownMenuBox(
                    expanded = expandedRaza,
                    onExpandedChange = { expandedRaza = it }
                ) {
                    OutlinedTextField(
                        value = razaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Raza *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRaza,
                        onDismissRequest = { expandedRaza = false }
                    ) {
                        razasDisponibles.forEach { raza ->
                            DropdownMenuItem(
                                text = { Text(raza) },
                                onClick = {
                                    razaSeleccionada = raza
                                    expandedRaza = false
                                }
                            )
                        }
                    }
                }

                if (razaSeleccionada == "Otra") {
                    OutlinedTextField(
                        value = razaPersonalizadaTexto,
                        onValueChange = { razaPersonalizadaTexto = it },
                        label = { Text("Especifica la raza *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = razaPersonalizadaTexto.isBlank()
                    )
                }
            }

            // Sexo
            Column {
                Text("Sexo *", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Macho", "Hembra").forEach { opcion ->
                        FilterChip(
                            selected = sexo == opcion,
                            onClick = { sexo = opcion },
                            label = { Text(opcion) }
                        )
                    }
                }
            }

            CampoFecha(
                valor = fechaNacimientoStr,
                onValorCambia = { fechaNacimientoStr = it },
                etiqueta = "Fecha de Nacimiento (DD/MM/AAAA) *",
                modifier = Modifier.fillMaxWidth(),
                esError = fechaNacimientoStr.isBlank()
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
                    val especieRazaOtroValida = especieSeleccionada != "Otro" || especieRazaOtroTexto.isNotBlank()
                    val razaPersonalizadaValida = razaSeleccionada != "Otra" || razaPersonalizadaTexto.isNotBlank()

                    if (nombre.isBlank() || color.isBlank() || !especieRazaOtroValida || !razaPersonalizadaValida) {
                        viewModel.setError("Completa todos los campos obligatorios")
                        return@Button
                    }

                    val fecha = try {
                        parseFormatoEuropeo(fechaNacimientoStr)
                    } catch (e: Exception) {
                        viewModel.setError("La fecha debe tener el formato DD/MM/AAAA")
                        return@Button
                    }

                    val especieFinal: String
                    val razaFinal: String
                    if (especieSeleccionada == "Otro") {
                        especieFinal = especieRazaOtroTexto
                        razaFinal = ""
                    } else {
                        especieFinal = especieSeleccionada
                        razaFinal = if (razaSeleccionada == "Otra") razaPersonalizadaTexto else razaSeleccionada
                    }

                    val mascota = Mascota(
                        id = mascotaId,
                        nombre = nombre,
                        especie = especieFinal,
                        raza = razaFinal,
                        fechaNacimiento = fecha,
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
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (mascotaId == null) "Guardar Mascota" else "Actualizar Mascota")
                }
            }
        }
    }
}
