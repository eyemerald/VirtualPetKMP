package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.util.formatearKilos
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Qué rango de pesajes se está mostrando en el gráfico. */
private enum class FiltroPeso(val etiqueta: String) {
    TODO("Todo"),
    ANIO("Año"),
    FECHAS("Elegir fechas")
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
    onBorrar: (Long) -> Unit
) {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var filtro by rememberSaveable { mutableStateOf(FiltroPeso.TODO) }
    var desdeTexto by rememberSaveable { mutableStateOf("") }
    var hastaTexto by rememberSaveable { mutableStateOf("") }
    var errorFechas by remember { mutableStateOf<String?>(null) }

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
                val desde = desdeTexto.toFechaONull()
                val hasta = hastaTexto.toFechaONull()
                ordenados.filter { registro ->
                    (desde == null || registro.fecha >= desde) &&
                        (hasta == null || registro.fecha <= hasta)
                }
            }
        }
    }

    val ultimo = filtrados.lastOrNull()

    HojaFicha(titulo = "Peso", onCerrar = onCerrar) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "resumen") {
                Column {
                    Text(
                        text = ultimo?.let { "${formatearKilos(it.peso)} kg" } ?: "Sin datos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (filtrados.isEmpty()) {
                            "No hay pesajes en este rango"
                        } else {
                            plural(filtrados.size, "pesaje en el gráfico", "pesajes en el gráfico")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "filtro") {
                Column {
                    Text(
                        text = "Filtro",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FiltroPeso.entries.forEach { opcion ->
                            FilterChip(
                                selected = filtro == opcion,
                                onClick = {
                                    filtro = opcion
                                    errorFechas = null
                                },
                                label = { Text(opcion.etiqueta) }
                            )
                        }
                    }
                }
            }

            // Con el filtro "Año" se ofrecen solo los años que tienen pesajes, del más
            // reciente al más antiguo.
            if (filtro == FiltroPeso.ANIO) {
                item(key = "anios") {
                    Column {
                        if (aniosConDatos.isEmpty()) {
                            Text(
                                text = "Todavía no hay pesajes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                aniosConDatos.forEach { anio ->
                                    FilterChip(
                                        selected = anioSeleccionado == anio,
                                        onClick = { anioSeleccionado = anio },
                                        label = { Text("$anio") }
                                    )
                                }
                            }
                        }
                    }
                }
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
                        if (desdeTexto.isNotBlank() && desdeTexto.toFechaONull() == null ||
                            hastaTexto.isNotBlank() && hastaTexto.toFechaONull() == null
                        ) {
                            Text(
                                text = "Usa el formato DD/MM/AAAA en las fechas",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }

            item(key = "titulo-historico") {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (filtrados.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        text = "Aún no hay pesajes registrados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(filtrados.sortedByDescending { it.fecha }, key = { it.id ?: 0 }) { registro ->
                TarjetaContenidoHoja(
                    titulo = "${formatearKilos(registro.peso)} kg · ${registro.fecha.toFormatoEuropeo()}",
                    detalle = registro.notas,
                    onAbrir = { onEditar(registro) },
                    onBorrar = registro.id?.let { id -> { onBorrar(id) } }
                )
            }
        }

        FilaAccionHoja(etiquetaAccion = "Añadir pesaje", onAccion = onAgregar)
    }
}

/** Un mes del gráfico: su posición temporal y el peso medio de ese mes (null si no hay). */
private data class MesPeso(val anio: Int, val mes: Int, val media: Double?)

private val MESES_CORTOS_GRAFICO = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
)

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
    modifier: Modifier = Modifier
) {
    val meses = remember(pesos) { serieMensual(pesos) }

    if (meses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Sin datos para el filtro elegido",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val colorLinea = LocalExtrasColors.current.barraActual
    val colorReferencia = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val estiloEtiqueta = TextStyle(fontSize = 9.sp, color = colorTexto)
    val densidad = LocalDensity.current
    val altoEtiquetas = with(densidad) { 16.dp.toPx() }
    val margenLados = with(densidad) { 10.dp.toPx() }

    // Posición de desplazamiento en píxeles: se guarda entre recomposiciones para no perderla
    // al abrir la hoja otra vez.
    var desplazamiento by rememberSaveable { mutableStateOf(0f) }

    BoxWithConstraints(modifier = modifier) {
        val margenLadosDp = 10.dp
        val anchoMaxDp = (maxWidth - margenLadosDp * 2).coerceAtLeast(1.dp)
        val anchoPx = with(densidad) { anchoMaxDp.toPx() }

        // Si hay meses de sobra, se ven unas 6 unidades por pantalla; si son pocos, se
        // reparten a lo ancho para no dejar huecos.
        val pasoMinimoDp = 52.dp
        val pasoMaximoDp = 60.dp
        val pasoTeoricoDp = anchoMaxDp / 6
        val pasoDp = pasoTeoricoDp.coerceIn(pasoMinimoDp, pasoMaximoDp)
        val pasoPx = with(densidad) { pasoDp.toPx() }
        val anchoContenidoPx = pasoPx * (meses.size - 1).coerceAtLeast(1)
        val maxDesplazamiento = (anchoContenidoPx - anchoPx).coerceAtLeast(0f)

        // Al abrir, se enseña el final del histórico, que es el peso más reciente.
        LaunchedEffect(anchoPx, maxDesplazamiento) {
            if (desplazamiento == 0f) desplazamiento = maxDesplazamiento
        }

        // Las etiquetas se adelgazan si hay muchísimos meses, para que no se solapen.
        val saltoEtiquetas = (meses.size / 12 + 1).coerceAtLeast(1)
        val desplazamientoPx = desplazamiento.coerceIn(0f, maxDesplazamiento)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(anchoPx, maxDesplazamiento) {
                    detectDragGestures { cambio, arrastre ->
                        cambio.consume()
                        desplazamiento =
                            (desplazamiento - arrastre.x).coerceIn(0f, maxDesplazamiento)
                    }
                }
        ) {
            val altoGrafico = (size.height - altoEtiquetas).coerceAtLeast(1f)
            val margenInterno = margenLados.coerceAtMost(size.width / 6f)
            val altoUtil = (altoGrafico - margenInterno * 2).coerceAtLeast(1f)
            // Ancho del hueco visible: las etiquetas que caen fuera del recorte no se dibujan.
            val anchoVisible = size.width

            fun x(indice: Int): Float = margenInterno + indice * pasoPx - desplazamientoPx

            // Solo los meses con pesaje: los huecos (meses sin datos) no se dibujan como
            // puntos, pero la línea sí los atraviesa para que la evolución se vea entera.
            val conDatos = meses.indices.filter { meses[it].media != null }
            val valores = conDatos.map { meses[it].media!! }
            val minPeso = valores.minOrNull() ?: 0.0
            val maxPeso = valores.maxOrNull() ?: 0.0
            val rango = (maxPeso - minPeso).toFloat()

            fun y(peso: Double): Float {
                val normalizado = if (rango <= 0.0001f) 0.5f else ((peso - minPeso) / rango).toFloat()
                return margenInterno + altoUtil * (1f - normalizado)
            }

            val puntos = conDatos.map { Offset(x(it), y(meses[it].media!!)) }

            // Referencias horizontales sutiles, como en la tarjeta de la ficha.
            listOf(0f, 0.5f, 1f).forEach { fraccion ->
                val lineaY = margenInterno + altoUtil * fraccion
                drawLine(
                    color = colorReferencia,
                    start = Offset(0f, lineaY),
                    end = Offset(size.width, lineaY),
                    strokeWidth = 1.dp.toPx()
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
                    "${MESES_CORTOS_GRAFICO[mes.mes - 1]} ${mes.anio % 100}"
                } else {
                    MESES_CORTOS_GRAFICO[mes.mes - 1]
                }
                val medida = textMeasurer.measure(etiqueta, style = estiloEtiqueta)
                // Se centra en el mes, se mete dentro del gráfico y, si quedaría pegada a la
                // etiqueta anterior, se salta: mejor un mes sin rótulo que dos montados.
                val idealX = (x(indice) - medida.size.width / 2f)
                    .coerceIn(0f, (anchoVisible - medida.size.width).coerceAtLeast(0f))
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

/** Convierte el texto de una fecha del filtro, o null si está vacío o mal formado. */
private fun String.toFechaONull(): LocalDate? = try {
    if (isBlank()) null else parseFormatoEuropeo(this)
} catch (e: Exception) {
    null
}

/** Dos campos de fecha para el filtro "elegir fechas". */
@Composable
private fun FieldFechasFiltro(
    desde: String,
    hasta: String,
    onDesde: (String) -> Unit,
    onHasta: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoFecha(
            valor = desde,
            onValorCambia = onDesde,
            etiqueta = "Desde (DD/MM/AAAA)",
            modifier = Modifier.weight(1f),
            esError = false
        )
        CampoFecha(
            valor = hasta,
            onValorCambia = onHasta,
            etiqueta = "Hasta (DD/MM/AAAA)",
            modifier = Modifier.weight(1f),
            esError = false
        )
    }
}

/** "1 pesaje" / "2 pesajes". */
private fun plural(cantidad: Int, singular: String, plural: String): String =
    "$cantidad ${if (cantidad == 1) singular else plural}"

