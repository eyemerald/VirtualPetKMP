package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.example.virtualpetkmp.util.FiltroPeso
import com.example.virtualpetkmp.util.GranularidadPeso
import com.example.virtualpetkmp.util.Localizacion
import com.example.virtualpetkmp.util.PuntoPeso
import com.example.virtualpetkmp.util.RotuloEjePeso
import com.example.virtualpetkmp.util.UnidadPeso
import com.example.virtualpetkmp.util.escalaDePeso
import com.example.virtualpetkmp.util.filtrarPesos
import com.example.virtualpetkmp.util.presentacionDe
import com.example.virtualpetkmp.util.serieDePeso
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import com.example.virtualpetkmp.util.parseKilos
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
    // Por defecto, "6 meses": es lo que se quiere ver al entrar y evita cargar todo el historial
    // de golpe. El usuario puede ampliar con el resto de filtros.
    var filtro by rememberSaveable { mutableStateOf(FiltroPeso.RECIENTE) }
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

    // `hoy` entra en las claves porque el filtro "mes actual" depende de él: si la hoja se
    // quedara abierta pasada la medianoche, el filtro tiene que recalcularse.
    val filtrados = remember(pesos, filtro, desdeTexto, hastaTexto, anioSeleccionado, hoy) {
        filtrarPesos(
            pesos = pesos,
            filtro = filtro,
            hoy = hoy,
            anio = anioSeleccionado,
            desde = desdeTexto.toFechaONull(formato),
            hasta = hastaTexto.toFechaONull(formato)
        )
    }

    val ultimo = filtrados.lastOrNull()

    // Cómo se dibuja el gráfico con este filtro (un punto por pesaje o por mes, y qué se rotula
    // bajo el eje). Lo decide `util`, no la pantalla, para poder probarlo.
    val presentacion = remember(filtro, filtrados) { presentacionDe(filtro, filtrados) }

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
                    // La serie se construye aquí y no dentro del gráfico: así la granularidad la
                    // decide el filtro (ver `presentacionDe`) en un solo sitio y con tests.
                    serie = serieDePeso(filtrados, presentacion.granularidad),
                    rotulo = presentacion.rotulo,
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

/**
 * Gráfico de la evolución del peso como una línea (igual que la tarjeta de la ficha y no como
 * barras), con las etiquetas de fecha debajo.
 *
 * Cómo se dibuja lo decide [presentacionDe] a partir del filtro: un punto por pesaje o uno por
 * mes (media), y el eje rotulado con meses o con días. La serie llega ya construida para que esa
 * decisión esté en un solo sitio y con tests.
 *
 * Si la serie no cabe en el ancho disponible con [SEPARACION_MINIMA_PUNTO] de aire, el gráfico se
 * hace más ancho que la pantalla y se puede arrastrar en horizontal; además arranca enseñando el
 * final, que es el pesaje más reciente.
 */
@Composable
private fun GraficoLineaMeses(
    serie: List<PuntoPeso>,
    rotulo: RotuloEjePeso,
    modifier: Modifier = Modifier,
    /** Peso ideal de la mascota: si existe, se dibuja como referencia discontinua. */
    pesoIdeal: Double? = null
) {
    if (serie.isEmpty()) {
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
        // El lienzo mide lo que pida la serie, pero nunca menos que la pantalla: si cabe, se ve
        // entera (como siempre) y si no, se arrastra. Con muchos puntos quedan más juntos y las
        // etiquetas se van saltando para no solaparse.
        val anchoContenido = anchoDelGrafico(anchoDisponible = maxWidth, puntos = serie.size)
        val saltoEtiquetas = (serie.size / 8 + 1).coerceAtLeast(1)

        val desplazamiento = rememberScrollState()
        // Si hay más gráfico que pantalla, se arranca por el final (el pesaje más reciente) en
        // vez de por el más antiguo. Solo la primera vez de cada serie, para no pelearse con el
        // usuario si decide arrastrar hacia atrás.
        var colocadoAlFinal by remember(serie) { mutableStateOf(false) }
        LaunchedEffect(desplazamiento.maxValue) {
            if (!colocadoAlFinal && desplazamiento.maxValue > 0) {
                desplazamiento.scrollTo(desplazamiento.maxValue)
                colocadoAlFinal = true
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(desplazamiento)
        ) {
            Canvas(
                modifier = Modifier
                    .width(anchoContenido)
                    .fillMaxHeight()
            ) {
                // Márgenes internos del área de dibujo: la línea flota dentro del lienzo y no toca
                // ningún borde. Van en píxeles porque el lienzo trabaja en píxeles.
                val margenIzq = MARGENES_GRAFICO_PESO.izquierda.toPx()
                val margenDer = MARGENES_GRAFICO_PESO.derecha.toPx()
                val margenArriba = MARGENES_GRAFICO_PESO.arriba.toPx()
                val margenAbajo = MARGENES_GRAFICO_PESO.abajo.toPx()

                // El alto útil descuenta la franja de las etiquetas de mes (que va debajo del todo)
                // y los márgenes de arriba y abajo.
                val altoGrafico = (size.height - altoEtiquetas).coerceAtLeast(1f)
                val altoUtil = (altoGrafico - margenArriba - margenAbajo).coerceAtLeast(1f)
                val anchoUtil = (size.width - margenIzq - margenDer).coerceAtLeast(1f)
                val baseY = margenArriba + altoUtil

            fun x(indice: Int): Float =
                margenIzq + (indice.toFloat() / (serie.size - 1).coerceAtLeast(1)) * anchoUtil

            // Solo los puntos con pesaje: los huecos (meses sin datos) no se dibujan como
            // puntos, pero la línea sí los atraviesa para que la evolución se vea entera.
            val conDatos = serie.indices.filter { serie[it].media != null }

            // El eje lo calcula `escalaDePeso`, que **incluye el peso ideal además de los
            // datos** y deja un 10 % de margen arriba y abajo. Es lo que hace que la línea del
            // ideal se dibuje a su altura exacta y que ningún trazo toque los bordes.
            val escala = escalaDePeso(
                pesosKilos = serie.mapNotNull { it.media },
                pesoIdealKilos = pesoIdeal,
                unidad = unidad
            )

            // La escala recibe kilos y devuelve la posición ya convertida a la unidad mostrada.
            fun y(pesoKilos: Double): Float =
                margenArriba + altoUtil * (1f - escala.normalizarKilos(pesoKilos))

            val puntos = conDatos.map { Offset(x(it), y(serie[it].media!!)) }

            // Referencias horizontales sutiles, como en la tarjeta de la ficha. Se quedan
            // dentro del área de dibujo, sin llegar a los bordes del lienzo.
            listOf(0f, 0.5f, 1f).forEach { fraccion ->
                val lineaY = margenArriba + altoUtil * fraccion
                drawLine(
                    color = colorReferencia,
                    start = Offset(margenIzq, lineaY),
                    end = Offset(size.width - margenDer, lineaY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Retícula vertical muy tenue: ayuda a seguir el eje sin ensuciar el gráfico.
            serie.forEachIndexed { indice, _ ->
                val lineaX = x(indice)
                if (lineaX >= margenIzq && lineaX <= size.width - margenDer) {
                    drawLine(
                        color = colorReferencia,
                        start = Offset(lineaX, margenArriba),
                        end = Offset(lineaX, baseY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Línea del PESO IDEAL: discontinua y en tono neutro, para que se lea como una
            // referencia y no como un dato medido. Lleva su valor rotulado a la izquierda.
            if (pesoIdeal != null) {
                val yIdeal = y(pesoIdeal)
                val etiquetaIdeal = etiquetaIdealTexto
                val medidaIdeal = textMeasurer.measure(etiquetaIdeal, style = estiloEtiqueta)
                // La etiqueta va en el margen izquierdo, centrada en la línea pero sin salirse
                // del lienzo; la línea arranca después para no atravesarla.
                val inicioLineaIdeal = (margenIzq + medidaIdeal.size.width + 8.dp.toPx())
                    .coerceAtMost(size.width - margenDer)
                drawLine(
                    color = colorIdeal,
                    start = Offset(inicioLineaIdeal, yIdeal),
                    end = Offset(size.width - margenDer, yIdeal),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 5.dp.toPx())
                    )
                )
                val yEtiquetaIdeal = (yIdeal - medidaIdeal.size.height / 2f)
                    .coerceIn(0f, (size.height - medidaIdeal.size.height).coerceAtLeast(0f))
                drawText(
                    textMeasurer = textMeasurer,
                    text = etiquetaIdeal,
                    topLeft = Offset(margenIzq, yEtiquetaIdeal),
                    style = TextStyle(fontSize = 9.sp, color = colorIdeal)
                )
            }

            if (puntos.size >= 2) {
                // Degradado bajo la curva: da volumen al gráfico. Va ANTES de la línea para
                // que el trazo quede por encima del relleno.
                rellenarBajoLaCurva(puntos = puntos, baseY = baseY, color = colorLinea)
                drawPath(
                    path = pathCurvaSuave(puntos),
                    color = colorLinea,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Cada pesaje de verdad lleva su marca: se ve de un vistazo cuándo se pesó.
            puntos.dropLast(1).forEach { punto ->
                drawCircle(color = colorLinea, radius = 2.5.dp.toPx(), center = punto)
            }

            // Etiquetas del eje inferior, según lo que toque rotular:
            //
            // - Eje de MESES: una por mes, con el año la primera vez que aparece ("nov 25 ene
            //   feb 26"). Es lo que se quiere ver en "6 meses" y en "Año".
            // - Eje de DÍAS: el número del día, y la primera dice de qué mes es. Es lo que se
            //   quiere ver en "Mes actual", donde todos los puntos son del mismo mes.
            val datosEtiquetas = mutableListOf<Pair<String, Float>>()
            var bordeDerecho = -Float.MAX_VALUE
            serie.forEachIndexed { indice, punto ->
                val cambiaDeMes = indice == 0 ||
                    serie[indice - 1].fecha.year != punto.fecha.year ||
                    serie[indice - 1].fecha.monthNumber != punto.fecha.monthNumber
                val cambiaDeAnio = indice == 0 || serie[indice - 1].fecha.year != punto.fecha.year

                // Con el eje en meses solo se rotula cuando empieza uno nuevo; con el de días se
                // van saltando para que no se solapen.
                if (rotulo == RotuloEjePeso.MES && !cambiaDeMes) return@forEachIndexed
                if (rotulo == RotuloEjePeso.DIA && !cambiaDeMes && indice % saltoEtiquetas != 0) {
                    return@forEachIndexed
                }

                val etiqueta = when {
                    rotulo == RotuloEjePeso.DIA && indice == 0 ->
                        "${etiquetasMes[punto.fecha.monthNumber - 1]} ${punto.fecha.year % 100}"

                    rotulo == RotuloEjePeso.DIA -> "${punto.fecha.dayOfMonth}"
                    cambiaDeAnio -> "${etiquetasMes[punto.fecha.monthNumber - 1]} ${punto.fecha.year % 100}"
                    else -> etiquetasMes[punto.fecha.monthNumber - 1]
                }
                val medida = textMeasurer.measure(etiqueta, style = estiloEtiqueta)
                // Se centra en el punto, se mete dentro del margen y, si quedaría pegada a la
                // etiqueta anterior, se salta: mejor un punto sin rótulo que dos montados.
                val limiteDerecho = (size.width - margenDer - medida.size.width).coerceAtLeast(margenIzq)
                val idealX = (x(indice) - medida.size.width / 2f).coerceIn(margenIzq, limiteDerecho)
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



