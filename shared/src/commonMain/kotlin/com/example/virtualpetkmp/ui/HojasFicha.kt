package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors
import com.example.virtualpetkmp.util.rememberAbridorArchivo
import com.example.virtualpetkmp.util.rememberAgregadorCalendario
import com.example.virtualpetkmp.util.toFormatoEuropeo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn

/** Alto máximo de las hojas modales, para que no tapen toda la ficha. */
private val ALTO_MAXIMO_HOJA = 0.86f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaFicha(
    titulo: String,
    onCerrar: () -> Unit,
    /** Acciones opcionales alineadas a la derecha del título (por ejemplo un menú ⋮). */
    acciones: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(ALTO_MAXIMO_HOJA)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                acciones?.invoke()
            }
            contenido()
        }
    }
}

/** Encabezado de sección dentro de una hoja. */
@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/** Tarjeta simple de contenido dentro de una hoja (título, subtítulo y detalle). */
@Composable
fun TarjetaContenidoHoja(
    titulo: String,
    subtitulo: String? = null,
    detalle: String? = null,
    onAbrir: (() -> Unit)? = null,
    onCalendario: (() -> Unit)? = null,
    onEditar: (() -> Unit)? = null,
    onBorrar: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LocalExtrasColors.current.tarjetaFondo),
        onClick = onAbrir ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!detalle.isNullOrBlank()) {
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onAbrir != null) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Abrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onCalendario != null) {
                IconButton(onClick = onCalendario) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Añadir al calendario",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onEditar != null) {
                IconButton(onClick = onEditar) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onBorrar != null) {
                IconButton(onClick = onBorrar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Hoja de Vacunas y preventivos: dos pestañas internas, una por categoría, con el estado
 * de cada registro (al día o vencido) y alta/baja directa sin salir de la hoja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaVacunasPreventivos(
    vacunas: List<Vacuna>,
    preventivos: List<Preventivo>,
    nombreMascota: String,
    onCerrar: () -> Unit,
    onAgregar: (esVacuna: Boolean) -> Unit,
    onEditarVacuna: (Vacuna) -> Unit,
    onEditarPreventivo: (Preventivo) -> Unit,
    onBorrarVacuna: (Long) -> Unit,
    onBorrarPreventivo: (Long) -> Unit
) {
    var pestana by rememberSaveable { mutableStateOf(0) }
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val agregarAlCalendario = rememberAgregadorCalendario()

    HojaFicha(titulo = "Vacunas y preventivos", onCerrar = onCerrar) {
        TabRow(selectedTabIndex = pestana) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text("Vacunas (${vacunas.size})") }
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text("Preventivos (${preventivos.size})") }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pestana == 0) {
                if (vacunas.isEmpty()) {
                    item { Text("Sin vacunas registradas") }
                }
                items(vacunas, key = { it.id ?: 0 }) { vacuna ->
                    val vencida = vacuna.fechaProximaDosis < hoy
                    val proxima = !vencida && vacuna.fechaProximaDosis <= hoyMasDias(hoy, 15)
                    TarjetaContenidoHoja(
                        titulo = vacuna.nombre,
                        subtitulo = "Próxima dosis: ${vacuna.fechaProximaDosis.toFormatoEuropeo()}",
                        detalle = when {
                            vencida -> "Vencida"
                            proxima -> "Vence pronto"
                            else -> "Al día"
                        },
                        // El botón de calendario es el que añade el recordatorio; para
                        // modificar los datos está el lápiz.
                        onCalendario = {
                            agregarAlCalendario(
                                "Vacuna: ${vacuna.nombre}",
                                "Recuerda poner la vacuna ${vacuna.nombre} a $nombreMascota",
                                vacuna.fechaProximaDosis.aMedianocheMillis()
                            )
                        },
                        onEditar = { onEditarVacuna(vacuna) },
                        onBorrar = vacuna.id?.let { id -> { onBorrarVacuna(id) } }
                    )
                }
            } else {
                if (preventivos.isEmpty()) {
                    item { Text("Sin preventivos registrados") }
                }
                items(preventivos, key = { it.id ?: 0 }) { preventivo ->
                    val vencido = preventivo.fechaProximaDosis < hoy
                    val proximo = !vencido && preventivo.fechaProximaDosis <= hoyMasDias(hoy, 5)
                    TarjetaContenidoHoja(
                        titulo = preventivo.nombre,
                        subtitulo = "${preventivo.tipo} · ${preventivo.fechaProximaDosis.toFormatoEuropeo()}",
                        detalle = when {
                            vencido -> "Vencido"
                            proximo -> "Vence pronto"
                            else -> "Al día"
                        },
                        onCalendario = {
                            agregarAlCalendario(
                                "Preventivo: ${preventivo.nombre}",
                                "Recuerda aplicar ${preventivo.nombre} a $nombreMascota",
                                preventivo.fechaProximaDosis.aMedianocheMillis()
                            )
                        },
                        onEditar = { onEditarPreventivo(preventivo) },
                        onBorrar = preventivo.id?.let { id -> { onBorrarPreventivo(id) } }
                    )
                }
            }
        }

        FilaAccionHoja(
            etiquetaAccion = if (pestana == 0) "Añadir vacuna" else "Añadir preventivo",
            onAccion = { onAgregar(pestana == 0) }
        )
    }
}

/** Fecha resultante de sumar [dias] a [desde]. */
private fun hoyMasDias(desde: LocalDate, dias: Int): LocalDate =
    LocalDate.fromEpochDays(desde.toEpochDays() + dias)

/**
 * Acción de alta de una hoja: el mismo botón "+" flotante que la lista de mascotas, abajo a
 * la derecha.
 *
 * Se unificó a propósito: antes había botones grandes verdes a lo ancho en unas hojas y un
 * "+" en otras, y la misma acción no debería verse distinta según dónde estés.
 *
 * @param etiquetaAccion texto solo para accesibilidad, ya que el botón no lleva texto.
 */
@Composable
fun FilaAccionHoja(
    etiquetaAccion: String,
    onAccion: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onAccion,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = etiquetaAccion)
        }
    }
}

/**
 * Milisegundos desde epoch de la medianoche de esa fecha, que es lo que espera el
 * calendario del sistema para un evento de todo el día.
 */
private fun LocalDate.aMedianocheMillis(): Long =
    this.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

/**
 * Hoja de Salud y seguimiento con tres pestañas: Tratamientos (activos + historial
 * replegable), Informes y Visitas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaSaludYSeguimiento(
    tratamientos: List<Tratamiento>,
    informes: List<Informe>,
    revisiones: List<Revision>,
    onCerrar: () -> Unit,
    onAgregar: () -> Unit,
    onEditarTratamiento: (Tratamiento) -> Unit,
    onBorrarTratamiento: (Long) -> Unit,
    onEditarRevision: (Revision) -> Unit,
    onAgregarRevision: () -> Unit,
    onBorrarRevision: (Long) -> Unit,
    onEditarInforme: (Informe) -> Unit,
    onAgregarInforme: () -> Unit,
    onBorrarInforme: (Long) -> Unit
) {
    var pestana by rememberSaveable { mutableStateOf(0) }
    var mostrarHistorial by rememberSaveable { mutableStateOf(false) }
    // errorAlAbrir NO se migra: es un aviso efímero al intentar abrir un archivo.
    var errorAlAbrir by remember { mutableStateOf(false) }
    val abrirArchivo = rememberAbridorArchivo()
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val activos = remember(tratamientos, hoy) {
        tratamientos.filter { it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy) }
    }
    val finalizados = remember(tratamientos, hoy) {
        tratamientos.filterNot { it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy) }
    }

    HojaFicha(titulo = "Salud y seguimiento", onCerrar = onCerrar) {
        TabRow(selectedTabIndex = pestana) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text("Tratamientos") }
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text("Informes") }
            )
            Tab(
                selected = pestana == 2,
                onClick = { pestana = 2 },
                text = { Text("Visitas") }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (pestana) {
                0 -> {
                    item { TituloSeccion("Activos (${activos.size})") }
                    if (activos.isEmpty()) {
                        item { Text("Sin tratamientos activos") }
                    }
                    items(activos, key = { it.id ?: 0 }) { tratamiento ->
                        TarjetaContenidoHoja(
                            titulo = tratamiento.nombreMedicamento,
                            subtitulo = listOfNotNull(tratamiento.dosis, tratamiento.frecuencia)
                                .joinToString(" · ")
                                .ifBlank { null },
                            detalle = if (tratamiento.fechaFin == null) {
                                "Crónico · desde ${tratamiento.fechaInicio.toFormatoEuropeo()}"
                            } else {
                                "Hasta ${tratamiento.fechaFin.toFormatoEuropeo()}"
                            },
                            onAbrir = { onEditarTratamiento(tratamiento) },
                            onBorrar = tratamiento.id?.let { id -> { onBorrarTratamiento(id) } }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { mostrarHistorial = !mostrarHistorial },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (mostrarHistorial) {
                                    "Ocultar historial (${finalizados.size})"
                                } else {
                                    "Ver historial (${finalizados.size})"
                                }
                            )
                        }
                    }

                    if (mostrarHistorial) {
                        items(finalizados, key = { it.id ?: 0 }) { tratamiento ->
                            TarjetaContenidoHoja(
                                titulo = tratamiento.nombreMedicamento,
                                subtitulo = listOfNotNull(tratamiento.dosis, tratamiento.frecuencia)
                                    .joinToString(" · ")
                                    .ifBlank { null },
                                detalle = "Finalizado: ${tratamiento.fechaFin?.toFormatoEuropeo() ?: "sin fecha"}",
                                onAbrir = { onEditarTratamiento(tratamiento) },
                                onBorrar = tratamiento.id?.let { id -> { onBorrarTratamiento(id) } }
                            )
                        }
                    }
                }

                1 -> {
                    item { TituloSeccion("Informes (${informes.size})") }
                    if (informes.isEmpty()) {
                        item { Text("Sin informes adjuntos") }
                    }
                    if (errorAlAbrir) {
                        item {
                            Text(
                                text = "No se pudo abrir el archivo del informe",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(informes, key = { it.id ?: 0 }) { informe ->
                        // Tocar el informe lo abre con la app del sistema; para modificarlo
                        // está el botón de editar de la tarjeta.
                        TarjetaContenidoHoja(
                            titulo = "${informe.tipo} · ${informe.fecha.toFormatoEuropeo()}",
                            subtitulo = informe.descripcion,
                            detalle = informe.nombreArchivo,
                            onAbrir = {
                                errorAlAbrir = !abrirArchivo(informe.rutaArchivo)
                            },
                            onEditar = { onEditarInforme(informe) },
                            onBorrar = informe.id?.let { id -> { onBorrarInforme(id) } }
                        )
                    }
                }

                else -> {
                    item { TituloSeccion("Visitas (${revisiones.size})") }
                    if (revisiones.isEmpty()) {
                        item { Text("Sin visitas registradas") }
                    }
                    items(
                        revisiones.sortedByDescending { it.fecha },
                        key = { it.id ?: 0 }
                    ) { revision ->
                        TarjetaContenidoHoja(
                            titulo = "${revision.fecha.toFormatoEuropeo()} · ${revision.motivo}",
                            subtitulo = revision.diagnostico,
                            detalle = listOfNotNull(revision.veterinario, revision.notas)
                                .joinToString(" · ")
                                .ifBlank { null },
                            onAbrir = { onEditarRevision(revision) },
                            onBorrar = revision.id?.let { id -> { onBorrarRevision(id) } }
                        )
                    }
                }
            }
        }

        // Abajo solo el alta, con el mismo "+" que en el resto de la app
        when (pestana) {
            0 -> FilaAccionHoja(etiquetaAccion = "Añadir tratamiento", onAccion = onAgregar)
            1 -> FilaAccionHoja(etiquetaAccion = "Añadir informe", onAccion = onAgregarInforme)
            else -> FilaAccionHoja(etiquetaAccion = "Añadir visita", onAccion = onAgregarRevision)
        }
    }
}

/**
 * Hoja de Notas: lista los apuntes y permite añadir o borrar sin cambiar de pantalla.
 *
 * @param onAgregar recibe el texto de la nota nueva.
 * @param onBorrar recibe el id de la nota que se quiere eliminar.
 */
@Composable
fun HojaNotas(
    notas: List<Nota>,
    onCerrar: () -> Unit,
    onAgregar: (String) -> Unit,
    onEditar: (Long, String) -> Unit,
    onBorrar: (Long) -> Unit
) {
    var texto by rememberSaveable { mutableStateOf("") }
    var notaEditando by rememberSaveable { mutableStateOf<Long?>(null) }

    HojaFicha(titulo = "A tener en cuenta", onCerrar = onCerrar) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text(if (notaEditando == null) "Nueva nota" else "Editando nota") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (texto.isNotBlank()) {
                        val id = notaEditando
                        if (id == null) {
                            onAgregar(texto.trim())
                        } else {
                            onEditar(id, texto.trim())
                        }
                        texto = ""
                        notaEditando = null
                    }
                },
                enabled = texto.isNotBlank()
            ) {
                Icon(
                    imageVector = if (notaEditando == null) Icons.Default.Add else Icons.Default.Check,
                    contentDescription = if (notaEditando == null) "Añadir nota" else "Guardar cambios"
                )
            }
            if (notaEditando != null) {
                IconButton(onClick = {
                    notaEditando = null
                    texto = ""
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar edición")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notas.isEmpty()) {
                item { Text("Sin notas registradas") }
            }
            items(notas, key = { it.id ?: 0 }) { nota ->
                TarjetaContenidoHoja(
                    titulo = nota.texto,
                    // Tocar la nota la carga arriba para poder modificarla
                    onAbrir = {
                        notaEditando = nota.id
                        texto = nota.texto
                    },
                    onBorrar = nota.id?.let { id -> { onBorrar(id) } }
                )
            }
        }
    }
}

