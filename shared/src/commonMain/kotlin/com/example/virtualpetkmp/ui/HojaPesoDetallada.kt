package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Peso
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
    ULTIMO_ANIO("Último año"),
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

    val filtrados = remember(pesos, filtro, desdeTexto, hastaTexto) {
        val ordenados = pesos.sortedBy { it.fecha }
        when (filtro) {
            FiltroPeso.TODO -> ordenados
            FiltroPeso.ULTIMO_ANIO -> {
                val limite = LocalDate.fromEpochDays(hoy.toEpochDays() - 365)
                ordenados.filter { it.fecha >= limite }
            }
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

    val porMes = remember(filtrados) { agruparPorMes(filtrados) }
    val ultimo = filtrados.lastOrNull()

    HojaFicha(titulo = "Peso", onCerrar = onCerrar) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "resumen") {
                Column {
                    Text(
                        text = ultimo?.let { "${formatearKilosHoja(it.peso)} kg" } ?: "Sin datos",
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
                GraficoBarrasMeses(
                    meses = porMes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                    titulo = "${formatearKilosHoja(registro.peso)} kg · ${registro.fecha.toFormatoEuropeo()}",
                    detalle = registro.notas,
                    onAbrir = { onEditar(registro) },
                    onBorrar = registro.id?.let { id -> { onBorrar(id) } }
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Button(
                onClick = onAgregar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir pesaje")
            }
        }
    }
}

/** Un mes del gráfico: etiqueta y peso medio de ese mes. */
private data class MesPeso(val etiqueta: String, val kilos: Double)

/** Agrupa los pesajes por mes (media) para que el gráfico no se sature. */
private fun agruparPorMes(pesos: List<Peso>): List<MesPeso> {
    if (pesos.isEmpty()) return emptyList()
    val meses = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )
    return pesos
        .groupBy { it.fecha.year to it.fecha.monthNumber }
        .toSortedMap(compareBy({ it.first }, { it.second }))
        .map { (clave, delMes) ->
            val media = delMes.sumOf { it.peso } / delMes.size
            MesPeso("${meses[clave.second - 1]} ${clave.first}", media)
        }
}

/** Convierte el texto de una fecha del filtro, o null si está vacío o mal formado. */
private fun String.toFechaONull(): LocalDate? = try {
    if (isBlank()) null else parseFormatoEuropeo(this)
} catch (e: Exception) {
    null
}

/**
 * Gráfico de barras por meses: barras juntas, la del último mes en azul y el resto en
 * oscuro, con el mes debajo de cada barra.
 */
@Composable
private fun GraficoBarrasMeses(
    meses: List<MesPeso>,
    modifier: Modifier = Modifier
) {
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

    val colorBarra = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val colorActual = LocalExtrasColors.current.barraActual
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val radio = with(androidx.compose.ui.platform.LocalDensity.current) { 3.dp.toPx() }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val estiloEtiqueta = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = colorTexto)
    val altoEtiqueta = with(androidx.compose.ui.platform.LocalDensity.current) { 14.dp.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val minPeso = meses.minOf { it.kilos }
        val maxPeso = meses.maxOf { it.kilos }
        val rango = (maxPeso - minPeso).toFloat()
        val altoBarras = (size.height - altoEtiqueta).coerceAtLeast(1f)
        val hueco = 6.dp.toPx()
        val anchoBarra = ((size.width - hueco * (meses.size - 1)) / meses.size).coerceAtLeast(1f)

        meses.forEachIndexed { indice, mes ->
            val normalizado = if (rango <= 0.0001f) 1f else ((mes.kilos - minPeso) / rango).toFloat()
            val fraccion = 0.15f + normalizado * 0.85f
            val altoBarra = (altoBarras * fraccion).coerceAtLeast(3.dp.toPx())
            val x = indice * (anchoBarra + hueco)
            val esUltimo = indice == meses.lastIndex

            drawRoundRect(
                color = if (esUltimo) colorActual else colorBarra,
                topLeft = Offset(x, altoBarras - altoBarra),
                size = Size(anchoBarra, altoBarra),
                cornerRadius = CornerRadius(radio, radio)
            )

            // Etiqueta del mes, solo si cabe
            val medida = textMeasurer.measure(mes.etiqueta, style = estiloEtiqueta)
            if (medida.size.width <= anchoBarra + hueco) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = mes.etiqueta,
                    topLeft = Offset(
                        x + (anchoBarra - medida.size.width) / 2f,
                        altoBarras + 2.dp.toPx()
                    ),
                    style = estiloEtiqueta
                )
            }
        }
    }
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

/** Kilos con dos decimales y coma, como se escriben en español. */
private fun formatearKilosHoja(valor: Double): String {
    val centesimas = kotlin.math.round(valor * 100).toLong()
    val entero = centesimas / 100
    val decimales = centesimas % 100
    return "$entero,${decimales.toString().padStart(2, '0')}"
}
