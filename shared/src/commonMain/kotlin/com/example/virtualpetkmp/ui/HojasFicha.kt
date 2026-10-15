package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.virtualpetkmp.util.TraduccionesDatos
import com.example.virtualpetkmp.util.DIAS_AVISO_PREVENTIVO
import com.example.virtualpetkmp.util.DIAS_AVISO_VACUNA
import com.example.virtualpetkmp.util.estadoDosis
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.rememberAbridorArchivo
import com.example.virtualpetkmp.util.rememberAgregadorCalendario
import com.example.virtualpetkmp.util.textoEstadoDosis
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.accion_abrir
import virtualpetkmp.shared.generated.resources.calendario_preventivo_descripcion
import virtualpetkmp.shared.generated.resources.calendario_preventivo_descripcion_fin
import virtualpetkmp.shared.generated.resources.calendario_preventivo_titulo
import virtualpetkmp.shared.generated.resources.calendario_vacuna_descripcion
import virtualpetkmp.shared.generated.resources.calendario_vacuna_descripcion_fin
import virtualpetkmp.shared.generated.resources.calendario_vacuna_titulo
import virtualpetkmp.shared.generated.resources.hoja_activos
import virtualpetkmp.shared.generated.resources.hoja_anadir_calendario
import virtualpetkmp.shared.generated.resources.hoja_anadir_informe
import virtualpetkmp.shared.generated.resources.hoja_anadir_nota
import virtualpetkmp.shared.generated.resources.hoja_anadir_preventivo
import virtualpetkmp.shared.generated.resources.hoja_anadir_tratamiento
import virtualpetkmp.shared.generated.resources.hoja_anadir_vacuna
import virtualpetkmp.shared.generated.resources.hoja_anadir_visita
import virtualpetkmp.shared.generated.resources.hoja_cancelar_edicion
import virtualpetkmp.shared.generated.resources.hoja_guardar_cambios
import virtualpetkmp.shared.generated.resources.hoja_historial_ocultar
import virtualpetkmp.shared.generated.resources.hoja_historial_ver
import virtualpetkmp.shared.generated.resources.hoja_informe_error_abrir
import virtualpetkmp.shared.generated.resources.hoja_informes_titulo
import virtualpetkmp.shared.generated.resources.hoja_nota_editando
import virtualpetkmp.shared.generated.resources.hoja_nota_nueva
import virtualpetkmp.shared.generated.resources.hoja_notas_titulo
import virtualpetkmp.shared.generated.resources.hoja_preventivos_pestana
import virtualpetkmp.shared.generated.resources.hoja_proxima_dosis
import virtualpetkmp.shared.generated.resources.hoja_salud_informes
import virtualpetkmp.shared.generated.resources.hoja_salud_titulo
import virtualpetkmp.shared.generated.resources.hoja_salud_tratamientos
import virtualpetkmp.shared.generated.resources.hoja_salud_visitas
import virtualpetkmp.shared.generated.resources.hoja_sin_fecha
import virtualpetkmp.shared.generated.resources.hoja_sin_informes
import virtualpetkmp.shared.generated.resources.hoja_sin_notas
import virtualpetkmp.shared.generated.resources.hoja_sin_preventivos
import virtualpetkmp.shared.generated.resources.hoja_sin_tratamientos
import virtualpetkmp.shared.generated.resources.hoja_sin_vacunas
import virtualpetkmp.shared.generated.resources.hoja_sin_visitas
import virtualpetkmp.shared.generated.resources.hoja_tratamiento_cronico
import virtualpetkmp.shared.generated.resources.hoja_tratamiento_finalizado
import virtualpetkmp.shared.generated.resources.hoja_tratamiento_hasta
import virtualpetkmp.shared.generated.resources.hoja_vacunas_pestana
import virtualpetkmp.shared.generated.resources.hoja_vacunas_titulo
import virtualpetkmp.shared.generated.resources.hoja_visitas_titulo
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

/**
 * Tarjeta simple de contenido dentro de una hoja (título, subtítulo y detalle).
 *
 * Dos formas de hacerla pulsable, y la diferencia importa:
 *
 * @param onAbrir **abre** el contenido en otra app (un informe adjunto, por ejemplo): hace la
 *   tarjeta pulsable y dibuja el icono de "abrir fuera".
 * @param onPulsar acción de la tarjeta **sin icono**, para todo lo que no es abrir: cargar una
 *   nota arriba para editarla, abrir el formulario de un tratamiento o de una visita. Ahí el
 *   icono de "abrir" engañaba: parecía que iba a sacar el contenido de la app, cuando en
 *   realidad abría el formulario para editarlo. Sin icono, la tarjeta se toca y ya está.
 */
@Composable
fun TarjetaContenidoHoja(
    titulo: String,
    subtitulo: String? = null,
    detalle: String? = null,
    onAbrir: (() -> Unit)? = null,
    onPulsar: (() -> Unit)? = null,
    onCalendario: (() -> Unit)? = null,
    onEditar: (() -> Unit)? = null,
    onBorrar: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LocalExtrasColors.current.tarjetaFondo),
        onClick = onAbrir ?: onPulsar ?: {}
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
                    contentDescription = stringResource(Res.string.accion_abrir),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onCalendario != null) {
                IconButton(onClick = onCalendario) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = stringResource(Res.string.hoja_anadir_calendario),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Editar y borrar van en el menú de tres puntos, igual que en el resto de la app:
            // antes eran un lápiz y una papelera sueltos, y la papelera a la vista se pulsa sin
            // querer.
            if (onEditar != null || onBorrar != null) {
                MenuAccionesItem(
                    nombre = titulo,
                    onEditar = onEditar,
                    onBorrar = onBorrar
                )
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
    // Formato de fecha del usuario para las fechas que se muestran aquí.
    val formato = localizacion
    // Los textos que se añaden al calendario del teléfono se resuelven AQUÍ, antes del
    // LazyColumn: dentro de su bloque (y de los lambdas de los botones) no hay contexto
    // composable y `stringResource` no se puede llamar.
    val tituloEventoVacuna = stringResource(Res.string.calendario_vacuna_titulo)
    val textoEventoVacuna = stringResource(Res.string.calendario_vacuna_descripcion)
    val textoEventoFin = stringResource(Res.string.calendario_vacuna_descripcion_fin)
    val tituloEventoPreventivo = stringResource(Res.string.calendario_preventivo_titulo)
    val textoEventoPreventivo = stringResource(Res.string.calendario_preventivo_descripcion)
    val textoEventoPreventivoFin = stringResource(Res.string.calendario_preventivo_descripcion_fin)

    HojaFicha(titulo = stringResource(Res.string.hoja_vacunas_titulo), onCerrar = onCerrar) {
        TabRow(selectedTabIndex = pestana) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text(stringResource(Res.string.hoja_vacunas_pestana, vacunas.size)) }
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text(stringResource(Res.string.hoja_preventivos_pestana, preventivos.size)) }
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
                    item { Text(stringResource(Res.string.hoja_sin_vacunas)) }
                }
                items(vacunas, key = { it.id ?: 0 }) { vacuna ->
                    val estado = estadoDosis(vacuna.fechaProximaDosis, hoy, DIAS_AVISO_VACUNA)
                    TarjetaContenidoHoja(
                        titulo = vacuna.nombre,
                        subtitulo = stringResource(
                            Res.string.hoja_proxima_dosis,
                            formato.formatearFecha(vacuna.fechaProximaDosis)
                        ),
                        detalle = textoEstadoDosis(estado),
                        // El botón de calendario es el que añade el recordatorio; para
                        // modificar los datos está el lápiz.
                        onCalendario = {
                            agregarAlCalendario(
                                tituloEventoVacuna + vacuna.nombre,
                                textoEventoVacuna + vacuna.nombre + textoEventoFin + nombreMascota,
                                vacuna.fechaProximaDosis.aMedianocheMillis()
                            )
                        },
                        onEditar = { onEditarVacuna(vacuna) },
                        onBorrar = vacuna.id?.let { id -> { onBorrarVacuna(id) } }
                    )
                }
            } else {
                if (preventivos.isEmpty()) {
                    item { Text(stringResource(Res.string.hoja_sin_preventivos)) }
                }
                items(preventivos, key = { it.id ?: 0 }) { preventivo ->
                    val estado = estadoDosis(preventivo.fechaProximaDosis, hoy, DIAS_AVISO_PREVENTIVO)
                    TarjetaContenidoHoja(
                        titulo = preventivo.nombre,
                        // El tipo se guarda en español en la base de datos: se traduce al mostrarlo.
                        subtitulo = "${TraduccionesDatos.etiqueta(preventivo.tipo, formato.idioma)}" +
                            " · ${formato.formatearFecha(preventivo.fechaProximaDosis)}",
                        detalle = textoEstadoDosis(estado),
                        onCalendario = {
                            agregarAlCalendario(
                                tituloEventoPreventivo + preventivo.nombre,
                                textoEventoPreventivo + preventivo.nombre +
                                    textoEventoPreventivoFin + nombreMascota,
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
            etiquetaAccion = stringResource(
                if (pestana == 0) Res.string.hoja_anadir_vacuna else Res.string.hoja_anadir_preventivo
            ),
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
    // Formato de fecha del usuario para las fechas que se muestran aquí.
    val formato = localizacion

    val activos = remember(tratamientos, hoy) {
        tratamientos.filter { it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy) }
    }
    val finalizados = remember(tratamientos, hoy) {
        tratamientos.filterNot { it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy) }
    }

    HojaFicha(titulo = stringResource(Res.string.hoja_salud_titulo), onCerrar = onCerrar) {
        TabRow(selectedTabIndex = pestana) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text(stringResource(Res.string.hoja_salud_tratamientos)) }
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text(stringResource(Res.string.hoja_salud_informes)) }
            )
            Tab(
                selected = pestana == 2,
                onClick = { pestana = 2 },
                text = { Text(stringResource(Res.string.hoja_salud_visitas)) }
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
                    item { TituloSeccion(stringResource(Res.string.hoja_activos, activos.size)) }
                    if (activos.isEmpty()) {
                        item { Text(stringResource(Res.string.hoja_sin_tratamientos)) }
                    }
                    items(activos, key = { it.id ?: 0 }) { tratamiento ->
                        TarjetaContenidoHoja(
                            titulo = tratamiento.nombreMedicamento,
                            subtitulo = listOfNotNull(tratamiento.dosis, tratamiento.frecuencia)
                                .joinToString(" · ")
                                .ifBlank { null },
                            detalle = if (tratamiento.fechaFin == null) {
                                stringResource(
                                    Res.string.hoja_tratamiento_cronico,
                                    formato.formatearFecha(tratamiento.fechaInicio)
                                )
                            } else {
                                stringResource(
                                    Res.string.hoja_tratamiento_hasta,
                                    formato.formatearFecha(tratamiento.fechaFin)
                                )
                            },
                            // Tocar la tarjeta abre el formulario para editarlo (la acción está
                            // en el propio toque, sin icono).
                            onPulsar = { onEditarTratamiento(tratamiento) },
                            onBorrar = tratamiento.id?.let { id -> { onBorrarTratamiento(id) } }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { mostrarHistorial = !mostrarHistorial },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(
                                    if (mostrarHistorial) {
                                        Res.string.hoja_historial_ocultar
                                    } else {
                                        Res.string.hoja_historial_ver
                                    },
                                    finalizados.size
                                )
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
                                detalle = stringResource(
                                    Res.string.hoja_tratamiento_finalizado,
                                    tratamiento.fechaFin?.let { formato.formatearFecha(it) }
                                        ?: stringResource(Res.string.hoja_sin_fecha)
                                ),
                                onPulsar = { onEditarTratamiento(tratamiento) },
                                onBorrar = tratamiento.id?.let { id -> { onBorrarTratamiento(id) } }
                            )
                        }
                    }
                }

                1 -> {
                    item { TituloSeccion(stringResource(Res.string.hoja_informes_titulo, informes.size)) }
                    if (informes.isEmpty()) {
                        item { Text(stringResource(Res.string.hoja_sin_informes)) }
                    }
                    if (errorAlAbrir) {
                        item {
                            Text(
                                text = stringResource(Res.string.hoja_informe_error_abrir),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(informes, key = { it.id ?: 0 }) { informe ->
                        // El informe SÍ se abre fuera, con la app del sistema, así que aquí el
                        // icono de abrir tiene sentido y se queda. Para modificarlo, el menú ⋮.
                        TarjetaContenidoHoja(
                            // El tipo de informe lo escribe el usuario, así que se muestra tal cual.
                            titulo = "${informe.tipo} · ${formato.formatearFecha(informe.fecha)}",
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
                    item { TituloSeccion(stringResource(Res.string.hoja_visitas_titulo, revisiones.size)) }
                    if (revisiones.isEmpty()) {
                        item { Text(stringResource(Res.string.hoja_sin_visitas)) }
                    }
                    items(
                        revisiones.sortedByDescending { it.fecha },
                        key = { it.id ?: 0 }
                    ) { revision ->
                        TarjetaContenidoHoja(
                            titulo = "${formato.formatearFecha(revision.fecha)} · ${revision.motivo}",
                            subtitulo = revision.diagnostico,
                            detalle = listOfNotNull(revision.veterinario, revision.notas)
                                .joinToString(" · ")
                                .ifBlank { null },
                            onPulsar = { onEditarRevision(revision) },
                            onBorrar = revision.id?.let { id -> { onBorrarRevision(id) } }
                        )
                    }
                }
            }
        }

        // Abajo solo el alta, con el mismo "+" que en el resto de la app
        when (pestana) {
            0 -> FilaAccionHoja(
                etiquetaAccion = stringResource(Res.string.hoja_anadir_tratamiento),
                onAccion = onAgregar
            )
            1 -> FilaAccionHoja(
                etiquetaAccion = stringResource(Res.string.hoja_anadir_informe),
                onAccion = onAgregarInforme
            )
            else -> FilaAccionHoja(
                etiquetaAccion = stringResource(Res.string.hoja_anadir_visita),
                onAccion = onAgregarRevision
            )
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

    HojaFicha(titulo = stringResource(Res.string.hoja_notas_titulo), onCerrar = onCerrar) {
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
                label = {
                    Text(
                        stringResource(
                            if (notaEditando == null) {
                                Res.string.hoja_nota_nueva
                            } else {
                                Res.string.hoja_nota_editando
                            }
                        )
                    )
                },
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
                    contentDescription = stringResource(
                        if (notaEditando == null) {
                            Res.string.hoja_anadir_nota
                        } else {
                            Res.string.hoja_guardar_cambios
                        }
                    )
                )
            }
            if (notaEditando != null) {
                IconButton(onClick = {
                    notaEditando = null
                    texto = ""
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.hoja_cancelar_edicion)
                    )
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
                item { Text(stringResource(Res.string.hoja_sin_notas)) }
            }
            items(notas, key = { it.id ?: 0 }) { nota ->
                TarjetaContenidoHoja(
                    titulo = nota.texto,
                    // Tocar la nota la carga arriba para modificarla. Va en `onPulsar` y no en
                    // `onAbrir`: no se abre nada fuera, así que no lleva icono.
                    onPulsar = {
                        notaEditando = nota.id
                        texto = nota.texto
                    },
                    onBorrar = nota.id?.let { id -> { onBorrar(id) } }
                )
            }
        }
    }
}

