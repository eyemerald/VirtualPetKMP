package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.util.aDiasEpoch
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.PesoViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.math.pow

private enum class ModoFiltro { TODO, ANIO, RANGO }

/**
 * Calcula un "paso" de eje legible (1, 2 o 5 por una potencia de 10) a partir
 * de un valor aproximado deseado. Es el mismo principio que usan las librerías
 * de gráficos para que las marcas caigan en números redondos, no en cifras raras.
 */
private fun pasoLegible(valorAproximado: Double): Double {
    if (valorAproximado <= 0) return 1.0
    val exponente = kotlin.math.floor(kotlin.math.log10(valorAproximado))
    val base = 10.0.pow(exponente)
    val fraccion = valorAproximado / base
    val pasoFraccion = when {
        fraccion <= 1 -> 1.0
        fraccion <= 2 -> 2.0
        fraccion <= 5 -> 5.0
        else -> 10.0
    }
    return pasoFraccion * base
}

/** Formatea un número quitando decimales sobrantes, sin depender de String.format (no disponible en común). */
private fun formatearNumero(valor: Double): String {
    val redondeado = kotlin.math.round(valor * 100) / 100.0
    if (redondeado == redondeado.toLong().toDouble()) {
        return redondeado.toLong().toString()
    }
    var texto = redondeado.toString()
    if (texto.contains('.')) {
        texto = texto.trimEnd('0').trimEnd('.')
    }
    return texto
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesosScreen(
    viewModel: PesoViewModel,
    onBack: () -> Unit
) {
    val pesos by viewModel.pesos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var modo by remember { mutableStateOf(ModoFiltro.TODO) }
    var anioSeleccionado by remember { mutableStateOf<Int?>(null) }
    var fechaDesdeTexto by remember { mutableStateOf("") }
    var fechaHastaTexto by remember { mutableStateOf("") }

    var fechaTexto by remember {
        mutableStateOf(
            Clock.System.todayIn(TimeZone.currentSystemDefault()).toFormatoEuropeo()
        )
    }
    var pesoTexto by remember { mutableStateOf("") }
    var notasTexto by remember { mutableStateOf("") }
    var registroSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var registroAEliminar by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de peso") },
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
                .verticalScroll(rememberScrollState())
        ) {
            if (pesos.isEmpty() && !isLoading) {
                Text(
                    text = "Esta mascota no tiene registros de peso todavía",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modo == ModoFiltro.TODO,
                        onClick = { modo = ModoFiltro.TODO },
                        label = { Text("Todo") }
                    )
                    FilterChip(
                        selected = modo == ModoFiltro.ANIO,
                        onClick = {
                            modo = ModoFiltro.ANIO
                            if (anioSeleccionado == null) {
                                anioSeleccionado = pesos.maxOf { it.fecha.year }
                            }
                        },
                        label = { Text("Año") }
                    )
                    FilterChip(
                        selected = modo == ModoFiltro.RANGO,
                        onClick = { modo = ModoFiltro.RANGO },
                        label = { Text("Elegir fechas") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                var fechaDesdeParsed: LocalDate? = null
                var fechaHastaParsed: LocalDate? = null
                var errorRango: String? = null

                when (modo) {
                    ModoFiltro.ANIO -> {
                        var expandedAnio by remember { mutableStateOf(false) }
                        val aniosDisponibles = remember(pesos) {
                            pesos.map { it.fecha.year }.distinct().sortedDescending()
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedAnio,
                            onExpandedChange = { expandedAnio = it }
                        ) {
                            OutlinedTextField(
                                value = anioSeleccionado?.toString() ?: "Elige un año",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Año") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAnio) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedAnio,
                                onDismissRequest = { expandedAnio = false }
                            ) {
                                aniosDisponibles.forEach { anio ->
                                    DropdownMenuItem(
                                        text = { Text(anio.toString()) },
                                        onClick = {
                                            anioSeleccionado = anio
                                            expandedAnio = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ModoFiltro.RANGO -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
                                fechaHastaTexto = hoy.toFormatoEuropeo()
                                fechaDesdeTexto = hoy.minus(DatePeriod(days = 7)).toFormatoEuropeo()
                            }) {
                                Text("Últimos 7 días")
                            }
                            OutlinedButton(onClick = {
                                val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
                                fechaHastaTexto = hoy.toFormatoEuropeo()
                                fechaDesdeTexto = hoy.minus(DatePeriod(days = 30)).toFormatoEuropeo()
                            }) {
                                Text("Último mes")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoFecha(
                                valor = fechaDesdeTexto,
                                onValorCambia = { fechaDesdeTexto = it },
                                etiqueta = "Desde",
                                modifier = Modifier.weight(1f)
                            )
                            CampoFecha(
                                valor = fechaHastaTexto,
                                onValorCambia = { fechaHastaTexto = it },
                                etiqueta = "Hasta",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        fechaDesdeParsed = fechaDesdeTexto.let { runCatching { parseFormatoEuropeo(it) }.getOrNull() }
                        fechaHastaParsed = fechaHastaTexto.let { runCatching { parseFormatoEuropeo(it) }.getOrNull() }

                        errorRango = when {
                            fechaDesdeTexto.isBlank() || fechaHastaTexto.isBlank() -> null
                            fechaDesdeParsed == null || fechaHastaParsed == null -> "Formato de fecha inválido (usa DD/MM/AAAA)"
                            fechaDesdeParsed > fechaHastaParsed -> "La fecha 'Desde' debe ser anterior a 'Hasta'"
                            else -> null
                        }

                        if (errorRango != null) {
                            Text(
                                text = errorRango,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ModoFiltro.TODO -> {
                        Text(
                            text = "Toca un punto del gráfico para ver su fecha y peso. Arrastra para desplazarte por el tiempo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (modo != ModoFiltro.TODO) {
                    Text(
                        text = "Toca un punto del gráfico para ver su fecha y peso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val datosFiltrados = remember(pesos, modo, anioSeleccionado, fechaDesdeParsed, fechaHastaParsed, errorRango) {
                    when (modo) {
                        ModoFiltro.TODO -> pesos.sortedBy { it.fecha }
                        ModoFiltro.ANIO -> pesos.filter { anioSeleccionado != null && it.fecha.year == anioSeleccionado }
                            .sortedBy { it.fecha }
                        ModoFiltro.RANGO -> if (errorRango == null && fechaDesdeParsed != null && fechaHastaParsed != null) {
                            pesos.filter { it.fecha >= fechaDesdeParsed && it.fecha <= fechaHastaParsed }
                                .sortedBy { it.fecha }
                        } else {
                            emptyList()
                        }
                    }
                }

                GraficoPeso(datos = datosFiltrados, permitirDesplazamiento = modo == ModoFiltro.TODO)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Toca un registro para editarlo:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pesos.sortedByDescending { it.fecha }.forEach { registro ->
                        RegistroPesoItem(
                            registro = registro,
                            seleccionado = registro.id == registroSeleccionadoId,
                            onClick = {
                                if (registroSeleccionadoId == registro.id) {
                                    registroSeleccionadoId = null
                                    pesoTexto = ""
                                    notasTexto = ""
                                    fechaTexto = Clock.System.todayIn(TimeZone.currentSystemDefault()).toFormatoEuropeo()
                                } else {
                                    registroSeleccionadoId = registro.id
                                    fechaTexto = registro.fecha.toFormatoEuropeo()
                                    pesoTexto = registro.peso.toString()
                                    notasTexto = registro.notas ?: ""
                                }
                            },
                            onDelete = { registroAEliminar = registro.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = if (registroSeleccionadoId == null) "Añadir nuevo registro:" else "Editar registro seleccionado:",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoFecha(
                valor = fechaTexto,
                onValorCambia = { fechaTexto = it },
                etiqueta = "Fecha (DD/MM/AAAA)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pesoTexto,
                onValueChange = { pesoTexto = it },
                label = { Text("Peso (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notasTexto,
                onValueChange = { notasTexto = it },
                label = { Text("Notas (opcional, ej: pesada en casa)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val fecha = try {
                            parseFormatoEuropeo(fechaTexto)
                        } catch (e: Exception) {
                            viewModel.setError("La fecha debe tener el formato DD/MM/AAAA")
                            return@Button
                        }

                        val pesoValor = pesoTexto.trim().replace(",", ".").toDoubleOrNull()
                        if (pesoValor == null || pesoValor <= 0) {
                            viewModel.setError("El peso debe ser un número positivo")
                            return@Button
                        }

                        val notas = notasTexto.ifBlank { null }
                        val id = registroSeleccionadoId

                        if (id == null) {
                            viewModel.addPeso(fecha, pesoValor, notas)
                        } else {
                            viewModel.updatePeso(id, fecha, pesoValor, notas)
                        }

                        registroSeleccionadoId = null
                        pesoTexto = ""
                        notasTexto = ""
                        fechaTexto = Clock.System.todayIn(TimeZone.currentSystemDefault()).toFormatoEuropeo()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (registroSeleccionadoId == null) "Añadir registro" else "Guardar cambios")
                }

                if (registroSeleccionadoId != null) {
                    OutlinedButton(
                        onClick = {
                            registroSeleccionadoId = null
                            pesoTexto = ""
                            notasTexto = ""
                            fechaTexto = Clock.System.todayIn(TimeZone.currentSystemDefault()).toFormatoEuropeo()
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    registroAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { registroAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres borrar este registro de peso?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePeso(id)
                    if (registroSeleccionadoId == id) {
                        registroSeleccionadoId = null
                        pesoTexto = ""
                        notasTexto = ""
                    }
                    registroAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { registroAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RegistroPesoItem(
    registro: Peso,
    seleccionado: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${registro.fecha.toFormatoEuropeo()} — ${registro.peso} kg")
                if (!registro.notas.isNullOrBlank()) {
                    Text(
                        text = registro.notas,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar registro",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Decide cómo se muestra el gráfico: con desplazamiento horizontal y ancho
 * proporcional a los datos (modo "Todo"), o con ancho fijo (Año / rango de fechas).
 * La lógica de dibujo y detección de toque vive toda en GraficoPesoCanvas,
 * reutilizada sin duplicar en ambos casos.
 */
@Composable
private fun GraficoPeso(datos: List<Peso>, permitirDesplazamiento: Boolean) {
    if (datos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin datos en este rango",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    // De momento, ancho fijo en todos los modos (el desplazamiento horizontal
    // para "Todo" con muchos datos se revisará más adelante como mejora aparte).
    GraficoPesoCanvas(
        filtrados = datos,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
private fun GraficoPesoCanvas(filtrados: List<Peso>, modifier: Modifier) {
    var indiceSeleccionado by remember(filtrados) { mutableStateOf<Int?>(null) }
    var tamanioCanvas by remember { mutableStateOf(IntSize.Zero) }

    val colorLinea = MaterialTheme.colorScheme.primary
    val colorDestacado = MaterialTheme.colorScheme.secondary
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorFondoEtiqueta = MaterialTheme.colorScheme.surfaceVariant

    val minEpoch = filtrados.first().fecha.aDiasEpoch()
    val maxEpoch = filtrados.last().fecha.aDiasEpoch()
    val diasSpan = (maxEpoch - minEpoch).coerceAtLeast(1)

    val minPeso = filtrados.minOf { it.peso }
    val maxPeso = filtrados.maxOf { it.peso }

    // Escala del eje Y: siempre empieza en 0. Si el peso máximo es menor de 1 kg,
    // se trabaja en gramos para que las marcas tengan sentido (ej: pájaros, crías).
    val enGramos = maxPeso < 1.0
    val maxEnUnidad = if (enGramos) maxPeso * 1000.0 else maxPeso
    val numeroDivisiones = 4
    val paso = pasoLegible(maxEnUnidad / numeroDivisiones)
    val maximoEscalaEnUnidad = kotlin.math.ceil(maxEnUnidad / paso) * paso
    val sufijoUnidad = if (enGramos) "g" else "kg"

    val rangoMin = 0.0
    val rango = (if (enGramos) maximoEscalaEnUnidad / 1000.0 else maximoEscalaEnUnidad).coerceAtLeast(0.001)

    fun etiquetaPeso(pesoEnKg: Double): String {
        val valorEnUnidad = if (enGramos) pesoEnKg * 1000.0 else pesoEnKg
        return "${formatearNumero(valorEnUnidad)} $sufijoUnidad"
    }

    val mesesAbrev = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

    val density = LocalDensity.current
    val margenIzquierdoPx = with(density) { 44.dp.toPx() }
    val margenInferiorPx = with(density) { 20.dp.toPx() }
    val margenHorizontalPx = with(density) { 10.dp.toPx() }

    fun anchoGrafico() = (tamanioCanvas.width - margenIzquierdoPx - margenHorizontalPx).coerceAtLeast(1f)
    fun altoGrafico() = (tamanioCanvas.height - margenInferiorPx).coerceAtLeast(1f)

    fun y(peso: Double): Float {
        if (tamanioCanvas.height == 0) return 0f
        return altoGrafico() - ((peso - rangoMin) / rango).toFloat() * altoGrafico()
    }

    // Única fuente de verdad para dónde está cada punto: se usa igual
    // para dibujar y para detectar el toque, así nunca se desincronizan.
    fun posicionDe(indice: Int): Offset {
        val registro = filtrados[indice]
        val px = if (filtrados.size == 1) {
            margenIzquierdoPx + anchoGrafico() / 2f
        } else {
            val epoch = registro.fecha.aDiasEpoch()
            margenIzquierdoPx + ((epoch - minEpoch).toFloat() / diasSpan.toFloat()) * anchoGrafico()
        }
        return Offset(px, y(registro.peso))
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .onSizeChanged { tamanioCanvas = it }
            .pointerInput(filtrados, tamanioCanvas) {
                detectTapGestures { toque ->
                    var mejorIndice: Int? = null
                    var mejorDistancia = Float.MAX_VALUE
                    filtrados.indices.forEach { indice ->
                        val posicion = posicionDe(indice)
                        val dx = posicion.x - toque.x
                        val dy = posicion.y - toque.y
                        val distancia = dx.pow(2) + dy.pow(2)
                        if (distancia < mejorDistancia) {
                            mejorDistancia = distancia
                            mejorIndice = indice
                        }
                    }
                    indiceSeleccionado = if (mejorDistancia < 40f.pow(2)) mejorIndice else null
                }
            }
    ) {
        // Etiquetas de peso (eje Y): varias marcas desde 0 hasta el máximo de la escala
        for (i in 0..numeroDivisiones) {
            val valorEnUnidad = (maximoEscalaEnUnidad / numeroDivisiones) * i
            val valorEnKg = if (enGramos) valorEnUnidad / 1000.0 else valorEnUnidad
            val etiqueta = etiquetaPeso(valorEnKg)
            val medida = textMeasurer.measure(etiqueta, style = TextStyle(fontSize = 11.sp, color = colorTexto))
            val posY = (y(valorEnKg) - medida.size.height / 2f).coerceIn(0f, altoGrafico() - medida.size.height)
            drawText(
                textMeasurer = textMeasurer,
                text = etiqueta,
                topLeft = Offset(0f, posY),
                style = TextStyle(fontSize = 11.sp, color = colorTexto)
            )
        }

        // Etiquetas de fecha (eje X): una marca por cada año real presente en los
        // datos (si el rango cruza varios años), o una por cada mes real presente
        // (si todos los datos caen en el mismo año). Así nunca se salta un año o
        // mes que sí tiene datos, ni se muestran fechas que no correspondan a nada.
        val anioInicio = filtrados.first().fecha.year
        val anioFin = filtrados.last().fecha.year

        fun dibujarMarcaX(epoch: Long, etiqueta: String) {
            val fraccion = ((epoch - minEpoch).toFloat() / diasSpan.toFloat()).coerceIn(0f, 1f)
            val medida = textMeasurer.measure(etiqueta, style = TextStyle(fontSize = 10.sp, color = colorTexto))
            val posX = margenIzquierdoPx + fraccion * anchoGrafico()
            val etiquetaX = (posX - medida.size.width / 2f).coerceIn(margenIzquierdoPx, (tamanioCanvas.width - medida.size.width).toFloat())
            drawText(
                textMeasurer = textMeasurer,
                text = etiqueta,
                topLeft = Offset(etiquetaX, altoGrafico() + 2f),
                style = TextStyle(fontSize = 10.sp, color = colorTexto)
            )
        }

        if (anioFin > anioInicio) {
            for (anio in anioInicio..anioFin) {
                val epochEnero = LocalDate(anio, 1, 1).aDiasEpoch()
                dibujarMarcaX(epochEnero, anio.toString())
            }
        } else {
            val mesesPresentes = filtrados.map { it.fecha.monthNumber }.distinct().sorted()
            mesesPresentes.forEach { mes ->
                val epochInicioMes = LocalDate(anioInicio, mes, 1).aDiasEpoch()
                dibujarMarcaX(epochInicioMes, mesesAbrev[mes - 1])
            }
        }

        if (filtrados.size == 1) {
            drawCircle(
                color = colorLinea,
                radius = 5.dp.toPx(),
                center = posicionDe(0)
            )
        } else {
            val path = Path()
            filtrados.indices.forEach { indice ->
                val posicion = posicionDe(indice)
                if (indice == 0) path.moveTo(posicion.x, posicion.y) else path.lineTo(posicion.x, posicion.y)
            }
            drawPath(path = path, color = colorLinea, style = Stroke(width = 2.dp.toPx()))

            filtrados.indices.forEach { indice ->
                val esSeleccionado = indice == indiceSeleccionado
                drawCircle(
                    color = if (esSeleccionado) colorDestacado else colorLinea,
                    radius = if (esSeleccionado) 7.dp.toPx() else 4.dp.toPx(),
                    center = posicionDe(indice)
                )
            }
        }

        indiceSeleccionado?.let { indice ->
            val registro = filtrados[indice]
            val texto = "${registro.fecha.toFormatoEuropeo()}  ${etiquetaPeso(registro.peso)}"
            val medida = textMeasurer.measure(texto, style = TextStyle(fontSize = 12.sp))

            val posicion = posicionDe(indice)

            var etiquetaX = posicion.x - medida.size.width / 2f
            etiquetaX = etiquetaX.coerceIn(0f, (tamanioCanvas.width - medida.size.width).toFloat().coerceAtLeast(0f))
            val etiquetaY = (posicion.y - medida.size.height - 16f).coerceAtLeast(0f)

            drawRoundRect(
                color = colorFondoEtiqueta,
                topLeft = Offset(etiquetaX - 6f, etiquetaY - 4f),
                size = Size(medida.size.width + 12f, medida.size.height + 8f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = texto,
                topLeft = Offset(etiquetaX, etiquetaY),
                style = TextStyle(fontSize = 12.sp, color = colorTexto)
            )
        }
    }
}
