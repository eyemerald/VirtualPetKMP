package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Mascota
import kotlinx.coroutines.launch
import com.example.virtualpetkmp.util.ExportadorPdf
import com.example.virtualpetkmp.util.VersionPdf
import com.example.virtualpetkmp.util.calcularEdad
import com.example.virtualpetkmp.util.guardarArchivo
import com.example.virtualpetkmp.util.rememberAbridorArchivo
import com.example.virtualpetkmp.util.rememberSelectorArchivo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private data class TarjetaFicha(
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val titulo: String,
    val valorPrincipal: String,
    val subtexto: String,
    val destino: String,
    val colorAcento: Color,
    val destacar: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaMascotaScreen(
    mascota: Mascota,
    viewModel: FichaMascotaViewModel,
    onBack: () -> Unit,
    onModificar: () -> Unit,
    onNavegar: (String) -> Unit
) {
    val resumen by viewModel.resumen.collectAsState()
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())

    var mostrarDialogoExportar by remember { mutableStateOf(false) }
    var exportando by remember { mutableStateOf(false) }
    var mensajeExportar by remember { mutableStateOf<String?>(null) }
    var versionSeleccionada by remember { mutableStateOf(VersionPdf.COMPLETA) }

    val scope = rememberCoroutineScope()
    val exportador = remember { ExportadorPdf() }
    val abrirArchivo = rememberAbridorArchivo()
    val seleccionarDestino = rememberSelectorArchivo { _, rutaArchivo ->
        scope.launch {
            try {
                exportando = true
                val rutaValida = normalizarRutaDestinoPdf(rutaArchivo, mascota.nombre)
                val datosPdf = viewModel.obtenerDatosPdf()
                val pdf = exportador.exportarFichaMascota(
                    mascota = mascota,
                    vacunas = datosPdf.vacunas,
                    revisiones = datosPdf.revisiones,
                    tratamientos = datosPdf.tratamientos,
                    pesos = datosPdf.pesos,
                    informes = datosPdf.informes,
                    notas = datosPdf.notas,
                    version = versionSeleccionada
                )

                val isPdf = pdf.size >= 4 && pdf[0] == 0x25.toByte() && pdf[1] == 0x50.toByte() && pdf[2] == 0x44.toByte() && pdf[3] == 0x46.toByte()
                if (!isPdf) {
                    mensajeExportar = "El contenido generado no parece un PDF válido (size=${pdf.size}). No se guardó."
                    exportando = false
                    mostrarDialogoExportar = false
                    return@launch
                }

                val guardado = guardarArchivo(rutaValida, pdf)

                if (guardado) {
                    val abierto = abrirArchivo(rutaValida)
                    mensajeExportar = if (abierto) {
                        "PDF generado y abierto correctamente."
                    } else {
                        "PDF generado y guardado en: $rutaValida"
                    }
                } else {
                    mensajeExportar = "No se pudo guardar el PDF en la ruta seleccionada."
                }
            } catch (e: Exception) {
                mensajeExportar = "Error al generar el PDF: ${e.message ?: "desconocido"}"
            } finally {
                exportando = false
                mostrarDialogoExportar = false
            }
        }
    }

    val tarjetas = listOf(
        TarjetaFicha(
            icono = Icons.Default.MedicalServices,
            titulo = "Vacunas",
            valorPrincipal = "${resumen.totalVacunas}",
            subtexto = if (resumen.vacunasVencidas > 0) "${resumen.vacunasVencidas} vencida(s)" else "al día",
            destino = "vacunas",
            colorAcento = MaterialTheme.colorScheme.primary,
            destacar = resumen.vacunasVencidas > 0
        ),
        TarjetaFicha(
            icono = Icons.Default.MedicalInformation,
            titulo = "Revisiones",
            valorPrincipal = "${resumen.totalRevisiones}",
            subtexto = "registradas",
            destino = "revisiones",
            colorAcento = MaterialTheme.colorScheme.tertiary
        ),
        TarjetaFicha(
            icono = Icons.Default.Medication,
            titulo = "Tratamientos",
            valorPrincipal = "${resumen.tratamientosActivos}",
            subtexto = "activo(s) de ${resumen.totalTratamientos}",
            destino = "tratamientos",
            colorAcento = MaterialTheme.colorScheme.secondary,
            destacar = resumen.tratamientosActivos > 0
        ),
        TarjetaFicha(
            icono = Icons.Default.Description,
            titulo = "Informes",
            valorPrincipal = "${resumen.totalInformes}",
            subtexto = "documento(s)",
            destino = "informes",
            colorAcento = MaterialTheme.colorScheme.primary
        ),
        TarjetaFicha(
            icono = Icons.Default.Note,
            titulo = "A tener en cuenta",
            valorPrincipal = "${resumen.totalNotas}",
            subtexto = "nota(s)",
            destino = "notas",
            colorAcento = MaterialTheme.colorScheme.tertiary
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mascota.nombre) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoExportar = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar PDF")
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
            // Cabecera con avatar y datos básicos
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mascota.nombre.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${mascota.especie} · ${mascota.raza}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = calcularEdad(mascota.fechaNacimiento, hoy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaPesoHero(
                ultimoPeso = resumen.ultimoPeso,
                pesoAnterior = resumen.pesoAnterior,
                pesos = resumen.pesosRecientes,
                onClick = { onNavegar("pesos") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rejilla de tarjetas (2 columnas), formando parte del mismo scroll que el resto
            tarjetas.chunked(2).forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { tarjeta ->
                        TarjetaResumen(
                            tarjeta = tarjeta,
                            onClick = { onNavegar(tarjeta.destino) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (fila.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onModificar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Modificar datos")
            }

            if (mensajeExportar != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensajeExportar!!,
                    color = if (mensajeExportar!!.contains("Error", ignoreCase = true) || mensajeExportar!!.contains("No se pudo", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (mostrarDialogoExportar) {
        AlertDialog(
            onDismissRequest = { if (!exportando) mostrarDialogoExportar = false },
            title = { Text("Exportar ficha PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona la versión que quieres exportar:")
                    FilterChip(
                        selected = versionSeleccionada == VersionPdf.RESUMIDA,
                        onClick = { versionSeleccionada = VersionPdf.RESUMIDA },
                        label = { Text("Resumida") }
                    )
                    FilterChip(
                        selected = versionSeleccionada == VersionPdf.COMPLETA,
                        onClick = { versionSeleccionada = VersionPdf.COMPLETA },
                        label = { Text("Completa") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { seleccionarDestino() }, enabled = !exportando) {
                    Text(if (exportando) "Exportando..." else "Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!exportando) mostrarDialogoExportar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun normalizarRutaDestinoPdf(rutaArchivo: String, nombreMascota: String): String {
    val nombreBase = nombreMascota.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "mascota" }
    val ruta = rutaArchivo.trim()
    if (ruta.isEmpty()) {
        val carpeta = java.io.File(System.getProperty("user.home"), ".virtualpet/exportaciones")
        carpeta.mkdirs()
        return java.io.File(carpeta, "${nombreBase}_${System.currentTimeMillis()}.pdf").absolutePath
    }

    if (ruta.startsWith("content://") || ruta.startsWith("file://")) return ruta

    val archivo = java.io.File(ruta)
    return if (archivo.exists() && archivo.isDirectory) {
        java.io.File(archivo, "${nombreBase}_${System.currentTimeMillis()}.pdf").absolutePath
    } else {
        val pathConExtension = if (ruta.lowercase().endsWith(".pdf")) ruta else "$ruta.pdf"
        pathConExtension
    }
}

@Composable
private fun TarjetaPesoHero(
    ultimoPeso: Double?,
    pesoAnterior: Double?,
    pesos: List<com.example.virtualpetkmp.Peso>,
    onClick: () -> Unit
) {
    val subiendo = ultimoPeso != null && pesoAnterior != null && ultimoPeso > pesoAnterior
    val bajando = ultimoPeso != null && pesoAnterior != null && ultimoPeso < pesoAnterior
    val colorTendencia = when {
        subiendo -> Color(0xFFB3261E)
        bajando -> Color(0xFF3F6B4A)
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Peso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ultimoPeso?.let { "$it kg" } ?: "Sin datos",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            if (pesos.size >= 2) {
                MiniGraficoPeso(
                    pesos = pesos,
                    colorLinea = colorTendencia,
                    colorTexto = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun MiniGraficoPeso(
    pesos: List<com.example.virtualpetkmp.Peso>,
    colorLinea: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    val ultimos = remember(pesos) {
        pesos.sortedBy { it.fecha }.takeLast(8)
    }

    val minPeso = ultimos.minOf { it.peso }
    val maxPeso = ultimos.maxOf { it.peso }
    val rango = (maxPeso - minPeso).coerceAtLeast(0.1)

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val margenDerechoPx = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val anchoGrafico = (size.width - margenDerechoPx).coerceAtLeast(1f)
        val alto = size.height

        fun x(indice: Int): Float {
            if (ultimos.size == 1) return anchoGrafico / 2f
            return (indice.toFloat() / (ultimos.size - 1)) * anchoGrafico
        }

        fun y(peso: Double): Float {
            return alto - ((peso - minPeso) / rango).toFloat() * alto
        }

        val etiquetaMax = "$maxPeso"
        val etiquetaMin = "$minPeso"
        val medidaMax = textMeasurer.measure(etiquetaMax, style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = colorTexto))
        val medidaMin = textMeasurer.measure(etiquetaMin, style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = colorTexto))

        drawText(
            textMeasurer = textMeasurer,
            text = etiquetaMax,
            topLeft = Offset(anchoGrafico + 6f, 0f),
            style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = colorTexto)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = etiquetaMin,
            topLeft = Offset(anchoGrafico + 6f, alto - medidaMin.size.height),
            style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = colorTexto)
        )

        val path = Path()
        val puntos = ultimos.indices.map { Offset(x(it), y(ultimos[it].peso)) }
        if (puntos.size >= 2) {
            path.moveTo(puntos[0].x, puntos[0].y)
            for (i in 0 until puntos.size - 1) {
                val p0 = if (i == 0) puntos[i] else puntos[i - 1]
                val p1 = puntos[i]
                val p2 = puntos[i + 1]
                val p3 = if (i + 2 < puntos.size) puntos[i + 2] else puntos[i + 1]
                val factor = 0.125f
                val c1x = p1.x + (p2.x - p0.x) * factor
                val c1y = p1.y + (p2.y - p0.y) * factor
                val c2x = p2.x - (p3.x - p1.x) * factor
                val c2y = p2.y - (p3.y - p1.y) * factor
                path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }
        }
        drawPath(path = path, color = colorLinea, style = Stroke(width = 2.5.dp.toPx()))

        ultimos.forEachIndexed { indice, registro ->
            drawCircle(
                color = colorLinea,
                radius = 3.dp.toPx(),
                center = Offset(x(indice), y(registro.peso))
            )
        }
    }
}

@Composable
private fun TarjetaResumen(
    tarjeta: TarjetaFicha,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFondo = if (tarjeta.destacar) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val colorTexto = if (tarjeta.destacar) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val colorIcono = if (tarjeta.destacar) {
        MaterialTheme.colorScheme.error
    } else {
        tarjeta.colorAcento
    }

    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tarjeta.icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = tarjeta.valorPrincipal,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Text(
                    text = tarjeta.titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorTexto
                )
                Text(
                    text = tarjeta.subtexto,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.7f)
                )
            }
        }
    }
}
