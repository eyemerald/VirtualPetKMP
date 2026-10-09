package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors
import com.example.virtualpetkmp.util.Localizacion
import com.example.virtualpetkmp.util.UnidadPeso
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import com.example.virtualpetkmp.util.parseKilos
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_historial_mostrar
import virtualpetkmp.shared.generated.resources.a11y_historial_ocultar
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.accion_guardar
import virtualpetkmp.shared.generated.resources.hoja_peso_titulo
import virtualpetkmp.shared.generated.resources.mes_corto_1
import virtualpetkmp.shared.generated.resources.mes_corto_10
import virtualpetkmp.shared.generated.resources.mes_corto_11
import virtualpetkmp.shared.generated.resources.mes_corto_12
import virtualpetkmp.shared.generated.resources.mes_corto_2
import virtualpetkmp.shared.generated.resources.mes_corto_3
import virtualpetkmp.shared.generated.resources.mes_corto_4
import virtualpetkmp.shared.generated.resources.mes_corto_5
import virtualpetkmp.shared.generated.resources.mes_corto_6
import virtualpetkmp.shared.generated.resources.mes_corto_7
import virtualpetkmp.shared.generated.resources.mes_corto_8
import virtualpetkmp.shared.generated.resources.mes_corto_9
import virtualpetkmp.shared.generated.resources.peso_anadir
import virtualpetkmp.shared.generated.resources.peso_contador
import virtualpetkmp.shared.generated.resources.peso_desde
import virtualpetkmp.shared.generated.resources.peso_en_grafico
import virtualpetkmp.shared.generated.resources.peso_etiqueta_ideal
import virtualpetkmp.shared.generated.resources.peso_filtro_anio
import virtualpetkmp.shared.generated.resources.peso_filtro_fechas
import virtualpetkmp.shared.generated.resources.peso_filtro_todo
import virtualpetkmp.shared.generated.resources.peso_formato_fechas
import virtualpetkmp.shared.generated.resources.peso_grafico_vacio
import virtualpetkmp.shared.generated.resources.peso_hasta
import virtualpetkmp.shared.generated.resources.peso_historico
import virtualpetkmp.shared.generated.resources.peso_historico_vacio
import virtualpetkmp.shared.generated.resources.peso_ideal
import virtualpetkmp.shared.generated.resources.peso_ideal_ayuda
import virtualpetkmp.shared.generated.resources.peso_ideal_cambiar
import virtualpetkmp.shared.generated.resources.peso_ideal_ejemplo
import virtualpetkmp.shared.generated.resources.peso_ideal_fijar
import virtualpetkmp.shared.generated.resources.peso_ideal_indicacion
import virtualpetkmp.shared.generated.resources.peso_ideal_kg
import virtualpetkmp.shared.generated.resources.peso_ideal_quitar
import virtualpetkmp.shared.generated.resources.peso_mas_opciones
import virtualpetkmp.shared.generated.resources.peso_resumen_ideal
import virtualpetkmp.shared.generated.resources.peso_resumen_sin_datos
import virtualpetkmp.shared.generated.resources.peso_sin_pesajes_en_rango
import virtualpetkmp.shared.generated.resources.peso_todavia_no_hay

/**
 * Qué rango de pesajes se está mostrando en el gráfico.
 *
 * Guarda el recurso de su etiqueta y no el texto: así el chip se traduce solo al pintarlo.
 */
private enum class FiltroPeso(val etiqueta: StringResource) {
    TODO(Res.string.peso_filtro_todo),
    ANIO(Res.string.peso_filtro_anio),
    FECHAS(Res.string.peso_filtro_fechas)
}

/**
 * Gráfico de peso completo, agrupado por meses para que sea legible aunque haya años de
 * historial, con filtro para acotar el rango. Debajo van el alta y el histórico.
 */
@Composable
fun HojaPesoDetallada(
    pesos: List<Peso>,
    onCerrar: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Peso) -> Unit,
    onBorrar: (Long) -> Unit,
    /** Peso ideal configurado de la mascota, o null si no tiene. */
    pesoIdeal: Double? = null,
    /** Fija o quita (null) el peso ideal sin salir de esta hoja. */
    onCambiarPesoIdeal: (Double?) -> Unit = {}
) {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    // Unidades, orden de fecha y separador decimal salen de aquí.
    val formato = localizacion
    var filtro by rememberSaveable { mutableStateOf(FiltroPeso.TODO) }
    var desdeTexto by rememberSaveable { mutableStateOf("") }
    var hastaTexto by rememberSaveable { mutableStateOf("") }
    var errorFechas by remember { mutableStateOf<String?>(null) }
    var menuAbierto by remember { mutableStateOf(false) }
    var editandoPesoIdeal by remember { mutableStateOf(false) }

    // Años que tienen algún pesaje, del más reciente al más antiguo: solo esos se ofrecen.
    val aniosConDatos = remember(pesos) {
        pesos.map { it.fecha.year }.distinct().sortedDescending()
    }
    // Por defecto, el año actual si tiene datos; si no, el más reciente que tenga.
    val anioPorDefecto = aniosConDatos.firstOrNull { it == hoy.year } ?: aniosConDatos.firstOrNull()
    var anioSeleccionado by rememberSaveable(aniosConDatos) {
        mutableStateOf(anioPorDefecto)
    }

    val filtrados = remember(pesos, filtro, desdeTexto, hastaTexto, anioSeleccionado) {
        val ordenados = pesos.sortedBy { it.fecha }
        when (filtro) {
            FiltroPeso.TODO -> ordenados
            FiltroPeso.ANIO -> ordenados.filter { it.fecha.year == anioSeleccionado }
            FiltroPeso.FECHAS -> {
                val desde = desdeTexto.toFechaONull(formato)
                val hasta = hastaTexto.toFechaONull(formato)
                ordenados.filter { registro ->
                    (desde == null || registro.fecha >= desde) &&
                        (hasta == null || registro.fecha <= hasta)
                }
            }
        }
    }

    val ultimo = filtrados.lastOrNull()

    HojaFicha(
        titulo = stringResource(Res.string.hoja_peso_titulo),
        onCerrar = onCerrar,
        acciones = {
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.peso_mas_opciones)
                    )
                }
                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (pesoIdeal == null) {
                                        Res.string.peso_ideal_fijar
                                    } else {
                                        Res.string.peso_ideal_cambiar
                                    }
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            editandoPesoIdeal = true
                        }
                    )
                    if (pesoIdeal != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.peso_ideal_quitar)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            },
                            onClick = {
                                menuAbierto = false
                                onCambiarPesoIdeal(null)
                            }
                        )
                    }
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "resumen") {
                Column {
                    Text(
                        text = ultimo?.let { formato.formatearPeso(it.peso, conUnidad = true) }
                            ?: stringResource(Res.string.peso_resumen_sin_datos),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(
                                if (filtrados.isEmpty()) {
                                    stringResource(Res.string.peso_sin_pesajes_en_rango)
                                } else {
                                    "${filtrados.size} " + pluralStringResource(
                                        Res.plurals.peso_en_grafico,
                                        filtrados.size
                                    )
                                }
                            )
                            pesoIdeal?.let {
                                append(
                                    stringResource(
                                        Res.string.peso_resumen_ideal,
                                        formato.formatearPeso(it, conUnidad = true)
                                    )
                                )
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Filtros en una sola fila compacta, para dejar el protagonismo al gráfico.
            item(key = "filtro") {
                FechaFiltroCompacto(
                    filtro = filtro,
                    aniosConDatos = aniosConDatos,
                    anioSeleccionado = anioSeleccionado,
                    onFiltro = { filtro = it; errorFechas = null },
                    onAnio = { anioSeleccionado = it }
                )
            }

            if (filtro == FiltroPeso.FECHAS) {
                item(key = "fechas") {
                    Column {
                        FieldFechasFiltro(
                            desde = desdeTexto,
                            hasta = hastaTexto,
                            onDesde = { desdeTexto = it; errorFechas = null },
                            onHasta = { hastaTexto = it; errorFechas = null }
                        )
                        if (desdeTexto.isNotBlank() && desdeTexto.toFechaONull(formato) == null ||
                            hastaTexto.isNotBlank() && hastaTexto.toFechaONull(formato) == null
                        ) {
                            Text(
                                // El formato esperado se toma de la región, no se escribe a mano:
                                // en inglés el mismo aviso tiene que pedir MM/DD/AAAA.
                                text = stringResource(Res.string.peso_formato_fechas, formato.pistaFormatoFecha),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item(key = "grafico") {
                GraficoLineaMeses(
                    pesos = filtrados,
                    pesoIdeal = pesoIdeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            // Histórico plegado por defecto: el gráfico es el protagonista de la hoja.
            item(key = "historico") {
                var expandido by rememberSaveable { mutableStateOf(false) }

                Column {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { expandido = !expandido },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.peso_historico),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${filtrados.size} " + pluralStringResource(
                                    Res.plurals.peso_contador,
                                    filtrados.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (expandido) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = stringResource(
                                    if (expandido) {
                                        Res.string.a11y_historial_ocultar
                                    } else {
                                        Res.string.a11y_historial_mostrar
                                    }
                                )
                            )
                        }
                    }

                    if (expandido) {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (filtrados.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.peso_historico_vacio),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filtrados.sortedByDescending { it.fecha }.forEach { registro ->
                                    FilaPesoHistorico(
                                        registro = registro,
                                        onClick = { onEditar(registro) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FilaAccionHoja(
            etiquetaAccion = stringResource(Res.string.peso_anadir),
            onAccion = onAgregar
        )
    }

    if (editandoPesoIdeal) {
        DialogoPesoIdeal(
            pesoIdealActual = pesoIdeal,
            onCancelar = { editandoPesoIdeal = false },
            onGuardar = { nuevo ->
                onCambiarPesoIdeal(nuevo)
                editandoPesoIdeal = false
            }
        )
    }
}

/**
 * Fila de filtros: los tres chips y, si toca "Año", los años con datos.
 *
 * Los chips van en un `FlowRow` y no en un `Row`: en alemán las etiquetas son bastante más
 * largas ("Zeitraum wählen" en vez de "Pick dates") y en una sola línea se salían de la
 * pantalla. Con `FlowRow` pasan a una segunda línea solo cuando hace falta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FechaFiltroCompacto(
    filtro: FiltroPeso,
    aniosConDatos: List<Int>,
    anioSeleccionado: Int?,
    onFiltro: (FiltroPeso) -> Unit,
    onAnio: (Int) -> Unit
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FiltroPeso.entries.forEach { opcion ->
                FilterChip(
                    selected = filtro == opcion,
                    onClick = { onFiltro(opcion) },
                    label = {
                        Text(
                            stringResource(opcion.etiqueta),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        if (filtro == FiltroPeso.ANIO) {
            Spacer(modifier = Modifier.height(4.dp))
            if (aniosConDatos.isEmpty()) {
                Text(
                    text = stringResource(Res.string.peso_todavia_no_hay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    aniosConDatos.forEach { anio ->
                        FilterChip(
                            selected = anioSeleccionado == anio,
                            onClick = { onAnio(anio) },
                            label = {
                                Text("$anio", style = MaterialTheme.typography.labelMedium)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Un pesaje del histórico: fecha amigable y peso, sin iconos de acción. Al pulsarlo se abre
 * el modal para editarlo o borrarlo, que es donde tienen sentido esas acciones.
 */
@Composable
private fun FilaPesoHistorico(
    registro: Peso,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fechaLarga(registro.fecha),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = localizacion.formatearPeso(registro.peso, conUnidad = true),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** "7 Oct 2026", más legible que el formato numérico para una lista de lectura rápida. */
@Composable
private fun fechaLarga(fecha: LocalDate): String =
    "${fecha.dayOfMonth} ${mesCortoHoja(fecha.monthNumber)} ${fecha.year}"

/**
 * Abreviatura del mes con la inicial en mayúscula, para la fecha larga del histórico.
 * Se reutilizan las mismas cadenas del eje del gráfico.
 */
@Composable
private fun mesCortoHoja(mes: Int): String = when (mes) {
    1 -> stringResource(Res.string.mes_corto_1)
    2 -> stringResource(Res.string.mes_corto_2)
    3 -> stringResource(Res.string.mes_corto_3)
    4 -> stringResource(Res.string.mes_corto_4)
    5 -> stringResource(Res.string.mes_corto_5)
    6 -> stringResource(Res.string.mes_corto_6)
    7 -> stringResource(Res.string.mes_corto_7)
    8 -> stringResource(Res.string.mes_corto_8)
    9 -> stringResource(Res.string.mes_corto_9)
    10 -> stringResource(Res.string.mes_corto_10)
    11 -> stringResource(Res.string.mes_corto_11)
    else -> stringResource(Res.string.mes_corto_12)
}

/**
 * Diálogo para fijar o cambiar el peso ideal sin salir de la hoja de Peso. Guardar en blanco
 * lo quita (vuelve a quedar sin configurar).
 *
 * El valor se muestra y se pide **en la unidad del usuario** (kg o lb), pero se guarda siempre
 * en kilos: al aceptar se convierte. Si no, un usuario estadounidense metería 59 "kilos"
 * creyendo que son libras.
 */
@Composable
private fun DialogoPesoIdeal(
    pesoIdealActual: Double?,
    onCancelar: () -> Unit,
    onGuardar: (Double?) -> Unit
) {
    val formato = localizacion
    // El valor guardado está en kilos; se convierte a la unidad mostrada para el campo.
    var texto by remember {
        mutableStateOf(
            pesoIdealActual?.let { formato.formatearPeso(it) } ?: ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    // Se resuelven fuera del `onClick`, que no es composable.
    val textoIndicacion = stringResource(Res.string.peso_ideal_indicacion)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(Res.string.peso_ideal)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.peso_ideal_ayuda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; error = null },
                    label = {
                        Text(stringResource(Res.string.peso_ideal_kg, formato.unidadPesoSufijo))
                    },
                    placeholder = { Text(stringResource(Res.string.peso_ideal_ejemplo)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (texto.isBlank()) {
                        onGuardar(null)
                    } else {
                        val valorMostrado = parseKilos(texto)
                        if (valorMostrado == null || valorMostrado <= 0.0) {
                            error = textoIndicacion
                        } else {
                            // Se guarda en kilos, que es la unidad de la base de datos.
                            onGuardar(UnidadPeso.aKilos(valorMostrado, formato.unidadPeso))
                        }
                    }
                }
            ) {
                Text(stringResource(Res.string.accion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
        }
    )
}

/** Un mes del gráfico: su posición temporal y el peso medio de ese mes (null si no hay). */
private data class MesPeso(val anio: Int, val mes: Int, val media: Double?)

/** Cuántos meses hay entre dos claves (año, mes). */
private fun mesesEntre(anioInicio: Int, mesInicio: Int, anioFin: Int, mesFin: Int): Int =
    (anioFin - anioInicio) * 12 + (mesFin - mesInicio)

/**
 * Serie mensual del gráfico: un punto por cada mes entre el primer y el último pesaje, con la
 * media de los pesajes de ese mes o null si ese mes no tiene ninguno. Así el eje inferior va
 * mes a mes seguido, sin saltos, como una línea de tiempo.
 */
private fun serieMensual(pesos: List<Peso>): List<MesPeso> {
    if (pesos.isEmpty()) return emptyList()
    val medias = pesos.groupBy { it.fecha.year to it.fecha.monthNumber }
        .mapValues { (_, delMes) -> delMes.sumOf { it.peso } / delMes.size }
    val ordenados = pesos.sortedBy { it.fecha }
    val primero = ordenados.first().fecha
    val ultimo = ordenados.last().fecha

    val total = mesesEntre(primero.year, primero.monthNumber, ultimo.year, ultimo.monthNumber)
    return (0..total).map { avance ->
        val bruto = primero.monthNumber - 1 + avance
        val anio = primero.year + bruto / 12
        val mes = bruto % 12 + 1
        MesPeso(anio = anio, mes = mes, media = medias[anio to mes])
    }
}

/**
 * Gráfico de la evolución del peso, mes a mes, como una línea (igual que la tarjeta de la
 * ficha y no como barras) con las etiquetas de mes debajo. Se puede arrastrar en horizontal
 * para recorrer todo el histórico.
 */
@Composable
private fun GraficoLineaMeses(
    pesos: List<Peso>,
    modifier: Modifier = Modifier,
    /** Peso ideal de la mascota: si existe, se dibuja como referencia discontinua. */
    pesoIdeal: Double? = null
) {
    val meses = remember(pesos) { serieMensual(pesos) }

    if (meses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.peso_grafico_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val colorLinea = LocalExtrasColors.current.barraActual
    val colorIdeal = LocalExtrasColors.current.lineaIdeal
    val colorReferencia = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val estiloEtiqueta = TextStyle(fontSize = 9.sp, color = colorTexto)
    val densidad = LocalDensity.current
    val altoEtiquetas = with(densidad) { 16.dp.toPx() }
    // Unidad y separador decimal del usuario. Los datos se guardan en kilos, así que se
    // convierte al dibujar, tanto para la escala como para las etiquetas.
    val formato = localizacion
    val unidad = formato.unidadPeso
    // Las abreviaturas de mes se resuelven ANTES del lienzo: `stringResource` es una función
    // composable y dentro de un Canvas no hay contexto de composición.
    val etiquetasMes = listOf(
        stringResource(Res.string.mes_corto_1),
        stringResource(Res.string.mes_corto_2),
        stringResource(Res.string.mes_corto_3),
        stringResource(Res.string.mes_corto_4),
        stringResource(Res.string.mes_corto_5),
        stringResource(Res.string.mes_corto_6),
        stringResource(Res.string.mes_corto_7),
        stringResource(Res.string.mes_corto_8),
        stringResource(Res.string.mes_corto_9),
        stringResource(Res.string.mes_corto_10),
        stringResource(Res.string.mes_corto_11),
        stringResource(Res.string.mes_corto_12)
    )
    // El texto del peso ideal se resuelve fuera del lienzo: dentro no hay contexto composable.
    val etiquetaIdealTexto = stringResource(
        Res.string.peso_etiqueta_ideal,
        formato.formatearPeso(pesoIdeal ?: 0.0)
    )

    BoxWithConstraints(modifier = modifier) {
        // Todo el histórico se ajusta al ancho disponible: no hay desplazamiento, así que la
        // línea nunca puede quedar cortada contra los bordes. Con muchos meses los puntos
        // quedan más juntos, y por eso las etiquetas se van saltando para no solaparse.
        val topeLadosPx = with(densidad) { 14.dp.toPx() }
        val saltoEtiquetas = (meses.size / 8 + 1).coerceAtLeast(1)

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val altoGrafico = (size.height - altoEtiquetas).coerceAtLeast(1f)
            val margenInterno = topeLadosPx.coerceAtMost(size.width / 6f)
            val altoUtil = (altoGrafico - margenInterno * 2).coerceAtLeast(1f)
            val anchoVisible = size.width
            val anchoUtil = (size.width - margenInterno * 2).coerceAtLeast(1f)

            fun x(indice: Int): Float =
                margenInterno + (indice.toFloat() / (meses.size - 1).coerceAtLeast(1)) * anchoUtil

            // Solo los meses con pesaje: los huecos (meses sin datos) no se dibujan como
            // puntos, pero la línea sí los atraviesa para que la evolución se vea entera.
            // Se trabaja en la unidad mostrada: si el eje fuera en kilos, los números de la
            // derecha (en libras) no cuadrarían con la línea.
            val conDatos = meses.indices.filter { meses[it].media != null }
            val valores = conDatos.map { UnidadPeso.desdeKilos(meses[it].media!!, unidad) }
            val minDatos = valores.minOrNull() ?: 0.0
            val maxDatos = valores.maxOrNull() ?: 0.0
            val idealMostrado = pesoIdeal?.let { UnidadPeso.desdeKilos(it, unidad) }

            // El peso ideal entra en la escala: si queda fuera del rango de los datos, la
            // referencia se vería pegada al borde (o fuera) y no se entendería.
            val minPeso = if (idealMostrado != null) minOf(minDatos, idealMostrado) else minDatos
            val maxPeso = if (idealMostrado != null) maxOf(maxDatos, idealMostrado) else maxDatos
            val rango = (maxPeso - minPeso).toFloat()

            fun y(peso: Double): Float {
                val normalizado = if (rango <= 0.0001f) 0.5f else ((peso - minPeso) / rango).toFloat()
                return margenInterno + altoUtil * (1f - normalizado)
            }

            val puntos = conDatos.map { Offset(x(it), y(UnidadPeso.desdeKilos(meses[it].media!!, unidad))) }

            // Referencias horizontales sutiles, como en la tarjeta de la ficha.
            listOf(0f, 0.5f, 1f).forEach { fraccion ->
                val lineaY = margenInterno + altoUtil * fraccion
                drawLine(
                    color = colorReferencia,
                    start = Offset(margenInterno, lineaY),
                    end = Offset(size.width - margenInterno, lineaY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Línea del PESO IDEAL: discontinua y en tono neutro, para que se lea como una
            // referencia y no como un dato medido. Lleva su valor rotulado a la izquierda.
            if (idealMostrado != null) {
                val yIdeal = y(idealMostrado)
                val etiquetaIdeal = etiquetaIdealTexto
                val medidaIdeal = textMeasurer.measure(etiquetaIdeal, style = estiloEtiqueta)
                // Encima de la línea si hay sitio, debajo si está muy arriba.
                val yEtiqueta = if (yIdeal - medidaIdeal.size.height - 2.dp.toPx() >= 0f) {
                    yIdeal - medidaIdeal.size.height - 2.dp.toPx()
                } else {
                    yIdeal + 2.dp.toPx()
                }
                // Se reserva el ancho de la etiqueta para que la línea no la atraviese.
                drawLine(
                    color = colorIdeal,
                    start = Offset(medidaIdeal.size.width + 8.dp.toPx(), yIdeal),
                    end = Offset(size.width - margenInterno, yIdeal),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 5.dp.toPx())
                    )
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = etiquetaIdeal,
                    topLeft = Offset(2.dp.toPx(), yEtiqueta),
                    style = TextStyle(fontSize = 9.sp, color = colorIdeal)
                )
            }

            // Retícula vertical muy tenue: ayuda a seguir el eje sin ensuciar el gráfico.
            meses.forEachIndexed { indice, _ ->
                val lineaX = x(indice)
                if (lineaX >= 0f && lineaX <= size.width) {
                    drawLine(
                        color = colorReferencia,
                        start = Offset(lineaX, margenInterno),
                        end = Offset(lineaX, margenInterno + altoUtil),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            if (puntos.size >= 2) {
                val path = Path()
                path.moveTo(puntos.first().x, puntos.first().y)
                for (i in 0 until puntos.size - 1) {
                    val p1 = puntos[i]
                    val p2 = puntos[i + 1]
                    val medio = (p1.x + p2.x) / 2f
                    path.cubicTo(medio, p1.y, medio, p2.y, p2.x, p2.y)
                }
                drawPath(path = path, color = colorLinea, style = Stroke(width = 2.dp.toPx()))
            }

            // Cada pesaje de verdad lleva su marca: se ve de un vistazo cuándo se pesó.
            puntos.dropLast(1).forEach { punto ->
                drawCircle(color = colorLinea, radius = 2.5.dp.toPx(), center = punto)
            }

            // Etiquetas de mes debajo. El año aparece solo cuando cambia, como en una línea
            // de tiempo: "nov dic 25 ene feb 26".
            val datosEtiquetas = mutableListOf<Pair<String, Float>>()
            var bordeDerecho = -Float.MAX_VALUE
            meses.forEachIndexed { indice, mes ->
                val primeraDelAnio = indice == 0 || meses[indice - 1].anio != mes.anio
                if (indice % saltoEtiquetas != 0 && !primeraDelAnio) return@forEachIndexed
                val etiqueta = if (primeraDelAnio) {
                    "${etiquetasMes[mes.mes - 1]} ${mes.anio % 100}"
                } else {
                    etiquetasMes[mes.mes - 1]
                }
                val medida = textMeasurer.measure(etiqueta, style = estiloEtiqueta)
                // Se centra en el mes, se mete dentro del margen y, si quedaría pegada a la
                // etiqueta anterior, se salta: mejor un mes sin rótulo que dos montados.
                val idealX = (x(indice) - medida.size.width / 2f)
                    .coerceIn(margenInterno, (anchoVisible - margenInterno - medida.size.width).coerceAtLeast(margenInterno))
                if (idealX < bordeDerecho + 6.dp.toPx()) return@forEachIndexed
                datosEtiquetas.add(etiqueta to idealX)
                bordeDerecho = idealX + medida.size.width
            }
            datosEtiquetas.forEach { (texto, posicionX) ->
                drawText(
                    textMeasurer = textMeasurer,
                    text = texto,
                    topLeft = Offset(x = posicionX, y = altoGrafico + 3.dp.toPx()),
                    style = estiloEtiqueta
                )
            }

            // Punto del último peso: el dato que más importa, destacado.
            if (puntos.isNotEmpty()) {
                drawCircle(
                    color = colorLinea,
                    radius = 4.dp.toPx(),
                    center = puntos.last()
                )
            }
        }
    }
}

/**
 * Convierte el texto de una fecha del filtro, o null si está vacío o mal formado.
 *
 * Se lee en el orden de la región (con respaldo al otro), igual que el resto de campos de
 * fecha: si no, en inglés un "03/04/2026" se interpretaría como 3 de abril.
 */
private fun String.toFechaONull(formato: Localizacion): LocalDate? =
    if (isBlank()) null else parseFechaLocalizada(this, formato.ordenFechaMostrada)

/** Dos campos de fecha para el filtro "elegir fechas". */
@Composable
private fun FieldFechasFiltro(
    desde: String,
    hasta: String,
    onDesde: (String) -> Unit,
    onHasta: (String) -> Unit
) {
    // La pista de formato acompaña al orden que pide la región.
    val pista = localizacion.pistaFormatoFecha
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoFecha(
            valor = desde,
            onValorCambia = onDesde,
            etiqueta = stringResource(Res.string.peso_desde, pista),
            modifier = Modifier.weight(1f),
            esError = false
        )
        CampoFecha(
            valor = hasta,
            onValorCambia = onHasta,
            etiqueta = stringResource(Res.string.peso_hasta, pista),
            modifier = Modifier.weight(1f),
            esError = false
        )
    }
}



