package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.util.ESPECIES
import com.example.virtualpetkmp.util.RAZAS_POR_ESPECIE
import com.example.virtualpetkmp.util.TraduccionesDatos
import com.example.virtualpetkmp.util.UnidadPeso
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import com.example.virtualpetkmp.util.parseKilos
import com.example.virtualpetkmp.util.rememberFotoMascota
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_volver
import virtualpetkmp.shared.generated.resources.form_ejemplo_peso
import virtualpetkmp.shared.generated.resources.form_error_peso
import virtualpetkmp.shared.generated.resources.foto_elegir
import virtualpetkmp.shared.generated.resources.foto_hacer
import virtualpetkmp.shared.generated.resources.foto_quitar
import virtualpetkmp.shared.generated.resources.mascota_actualizar
import virtualpetkmp.shared.generated.resources.mascota_anadir_foto
import virtualpetkmp.shared.generated.resources.mascota_cambiar_foto
import virtualpetkmp.shared.generated.resources.mascota_campo_color
import virtualpetkmp.shared.generated.resources.mascota_campo_especie
import virtualpetkmp.shared.generated.resources.mascota_campo_fecha_nacimiento
import virtualpetkmp.shared.generated.resources.mascota_campo_microchip
import virtualpetkmp.shared.generated.resources.mascota_campo_nombre
import virtualpetkmp.shared.generated.resources.mascota_campo_peso_ideal
import virtualpetkmp.shared.generated.resources.mascota_campo_raza
import virtualpetkmp.shared.generated.resources.mascota_campo_sexo
import virtualpetkmp.shared.generated.resources.mascota_completar_campos
import virtualpetkmp.shared.generated.resources.mascota_editar
import virtualpetkmp.shared.generated.resources.mascota_error_fecha
import virtualpetkmp.shared.generated.resources.mascota_especifica_especie_raza
import virtualpetkmp.shared.generated.resources.mascota_especifica_raza
import virtualpetkmp.shared.generated.resources.mascota_foto_titulo
import virtualpetkmp.shared.generated.resources.mascota_guardar
import virtualpetkmp.shared.generated.resources.mascota_nueva

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaFormScreen(
    viewModel: MascotaViewModel,
    mascotaId: Long? = null,
    onBack: () -> Unit
) {
    // Unidad de peso, orden de fecha y separador decimal del usuario.
    val formato = localizacion
    val pistaFecha = formato.pistaFormatoFecha
    // Textos de error resueltos fuera del onClick: dentro no hay contexto composable.
    val textoCamposObligatorios = stringResource(Res.string.mascota_completar_campos)
    val textoErrorFecha = stringResource(Res.string.mascota_error_fecha, pistaFecha)
    // La unidad se pasa al texto: el aviso tiene que hablar de kg o de lb según la región.
    val textoErrorPeso = stringResource(
        Res.string.form_error_peso,
        formato.unidadPesoSufijo,
        stringResource(Res.string.form_ejemplo_peso)
    )

    var nombre by rememberSaveable { mutableStateOf("") }

    var especieSeleccionada by rememberSaveable { mutableStateOf(ESPECIES.first()) }
    var especieRazaOtroTexto by rememberSaveable { mutableStateOf("") }

    var razaSeleccionada by rememberSaveable { mutableStateOf("") }
    var razaPersonalizadaTexto by rememberSaveable { mutableStateOf("") }

    var sexo by rememberSaveable { mutableStateOf("Macho") }

    var fechaNacimientoStr by rememberSaveable {
        mutableStateOf(
            formato.formatearFecha(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            )
        )
    }
    var color by rememberSaveable { mutableStateOf("") }
    var microchip by rememberSaveable { mutableStateOf("") }
    var pesoIdealTexto by rememberSaveable { mutableStateOf("") }
    var errorPesoIdeal by remember { mutableStateOf<String?>(null) }

    // Foto de la mascota: ruta local de la copia guardada por la app.
    var rutaFoto by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarOpcionesFoto by rememberSaveable { mutableStateOf(false) }
    var errorFoto by remember { mutableStateOf<String?>(null) }

    val accionesFoto = rememberFotoMascota(
        onFotoSeleccionada = { foto ->
            errorFoto = null
            rutaFoto = foto.ruta
            mostrarOpcionesFoto = false
        },
        onError = { mensaje ->
            errorFoto = mensaje
            mostrarOpcionesFoto = false
        }
    )

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val mascotas by viewModel.mascotas.collectAsState()

    // Bandera de "ya cargado" desde la BD. Es rememberSaveable a propósito: sobrevive a la
    // rotación, de modo que al recrearse la Activity NO se recargan los datos y se respetan
    // los cambios que el usuario ya hubiera hecho en los campos.
    var yaCargado by rememberSaveable { mutableStateOf(false) }

    // Cargar los datos de la mascota existente si se está editando
    LaunchedEffect(mascotaId) {
        if (mascotaId != null && !yaCargado) {
            mascotas.find { it.id == mascotaId }?.let { mascota ->
                nombre = mascota.nombre
                fechaNacimientoStr = formato.formatearFecha(mascota.fechaNacimiento)
                sexo = mascota.sexo
                color = mascota.color
                microchip = mascota.microchip ?: ""
                pesoIdealTexto = mascota.pesoIdeal?.let { formato.formatearPeso(it) } ?: ""
                rutaFoto = mascota.foto

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
                yaCargado = true
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
                title = {
                    Text(
                        stringResource(
                            if (mascotaId == null) {
                                Res.string.mascota_nueva
                            } else {
                                Res.string.mascota_editar
                            }
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(stringResource(Res.string.mascota_campo_nombre)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombre.isBlank()
            )

            // Foto de la mascota: se puede elegir del almacenamiento o hacer con la cámara
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarMascota(
                    nombre = nombre,
                    rutaFoto = rutaFoto,
                    tamano = 120.dp,
                    onCambiarFoto = { mostrarOpcionesFoto = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                    if (rutaFoto.isNullOrBlank()) {
                        Res.string.mascota_anadir_foto
                    } else {
                        Res.string.mascota_cambiar_foto
                    }
                ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (errorFoto != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorFoto!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Especie
            var expandedEspecie by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedEspecie,
                onExpandedChange = { expandedEspecie = it }
            ) {
                OutlinedTextField(
                    value = TraduccionesDatos.etiqueta(especieSeleccionada, formato.idioma),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.mascota_campo_especie)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEspecie) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedEspecie,
                    onDismissRequest = { expandedEspecie = false }
                ) {
                    ESPECIES.forEach { especie ->
                        DropdownMenuItem(
                            text = { Text(TraduccionesDatos.etiqueta(especie, formato.idioma)) },
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
                    label = { Text(stringResource(Res.string.mascota_especifica_especie_raza)) },
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
                        value = TraduccionesDatos.etiqueta(razaSeleccionada, formato.idioma),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.mascota_campo_raza)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRaza,
                        onDismissRequest = { expandedRaza = false }
                    ) {
                        razasDisponibles.forEach { raza ->
                            DropdownMenuItem(
                                text = { Text(TraduccionesDatos.etiqueta(raza, formato.idioma)) },
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
                        label = { Text(stringResource(Res.string.mascota_especifica_raza)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = razaPersonalizadaTexto.isBlank()
                    )
                }
            }

            // Sexo
            Column {
                Text(stringResource(Res.string.mascota_campo_sexo), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // El valor guardado sigue siendo "Macho"/"Hembra": solo se traduce al pintar.
                    listOf("Macho", "Hembra").forEach { opcion ->
                        FilterChip(
                            selected = sexo == opcion,
                            onClick = { sexo = opcion },
                            label = { Text(TraduccionesDatos.etiqueta(opcion, formato.idioma)) }
                        )
                    }
                }
            }

            CampoFecha(
                valor = fechaNacimientoStr,
                onValorCambia = { fechaNacimientoStr = it },
                etiqueta = stringResource(Res.string.mascota_campo_fecha_nacimiento, pistaFecha),
                modifier = Modifier.fillMaxWidth(),
                esError = fechaNacimientoStr.isBlank()
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text(stringResource(Res.string.mascota_campo_color)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = color.isBlank()
            )

            OutlinedTextField(
                value = microchip,
                onValueChange = { microchip = it },
                label = { Text(stringResource(Res.string.mascota_campo_microchip)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Peso ideal: opcional y solo informativo. Sirve para comparar con el último
            // pesaje en la lista y para dibujar la referencia en el gráfico de peso.
            OutlinedTextField(
                value = pesoIdealTexto,
                onValueChange = { pesoIdealTexto = it; errorPesoIdeal = null },
                label = { Text(stringResource(Res.string.mascota_campo_peso_ideal, formato.unidadPesoSufijo)) },
                placeholder = { Text(stringResource(Res.string.form_ejemplo_peso)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorPesoIdeal != null,
                supportingText = errorPesoIdeal?.let { { Text(it) } }
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
                        viewModel.setError(textoCamposObligatorios)
                        return@Button
                    }

                    // La fecha se lee en el orden de la región, con respaldo al otro formato.
                    val fecha = parseFechaLocalizada(fechaNacimientoStr, formato.ordenFechaMostrada)
                    if (fecha == null) {
                        viewModel.setError(textoErrorFecha)
                        return@Button
                    }

                    // El peso ideal es opcional; si se escribe algo tiene que ser un número.
                    val pesoIdeal = if (pesoIdealTexto.isBlank()) {
                        null
                    } else {
                        val valorMostrado = parseKilos(pesoIdealTexto)
                        if (valorMostrado == null || valorMostrado <= 0.0) {
                            errorPesoIdeal = textoErrorPeso
                            return@Button
                        }
                        errorPesoIdeal = null
                        UnidadPeso.aKilos(valorMostrado, formato.unidadPeso)
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
                        microchip = microchip.ifBlank { null },
                        foto = rutaFoto,
                        pesoIdeal = pesoIdeal
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
                    Text(stringResource(if (mascotaId == null) Res.string.mascota_guardar else Res.string.mascota_actualizar))
                }
            }
        }
    }

    if (mostrarOpcionesFoto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarOpcionesFoto = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.mascota_foto_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                if (accionesFoto.soportaCamara) {
                    OpcionFotoFormulario(
                        icono = Icons.Default.PhotoCamera,
                        texto = stringResource(Res.string.foto_hacer),
                        onClick = { accionesFoto.hacerFoto?.invoke() }
                    )
                }

                OpcionFotoFormulario(
                    icono = Icons.Default.PhotoLibrary,
                    texto = stringResource(Res.string.foto_elegir),
                    onClick = accionesFoto.elegirDeArchivos
                )

                if (!rutaFoto.isNullOrBlank()) {
                    OpcionFotoFormulario(
                        icono = Icons.Default.Delete,
                        texto = stringResource(Res.string.foto_quitar),
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            mostrarOpcionesFoto = false
                            errorFoto = null
                            rutaFoto = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionFotoFormulario(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
