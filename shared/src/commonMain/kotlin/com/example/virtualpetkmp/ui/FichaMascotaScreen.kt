package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.Mascota
import kotlinx.coroutines.launch
import com.example.virtualpetkmp.util.ExportadorPdf
import com.example.virtualpetkmp.util.VersionPdf
import com.example.virtualpetkmp.util.calcularEdad
import com.example.virtualpetkmp.util.guardarArchivo
import com.example.virtualpetkmp.util.rememberAbridorArchivo
import com.example.virtualpetkmp.util.rememberFotoMascota
import com.example.virtualpetkmp.util.rememberSelectorDestino
import com.example.virtualpetkmp.util.toFormatoEuropeo
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import com.example.virtualpetkmp.viewmodel.ResumenFicha
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Hoja modal activa del dashboard. None = no hay ninguna abierta. */
sealed class ActiveSheet {
    data object None : ActiveSheet()
    data object Vaccines : ActiveSheet()
    data object HealthAndTracking : ActiveSheet()
    data object Notes : ActiveSheet()
    data object Weight : ActiveSheet()
}

/** "1 nota" / "2 notas": elige singular o plural según la cantidad. */
private fun plural(cantidad: Int, singular: String, plural: String): String =
    "$cantidad ${if (cantidad == 1) singular else plural}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaMascotaScreen(
    mascota: Mascota,
    viewModel: FichaMascotaViewModel,
    mascotaViewModel: MascotaViewModel,
    onModificar: () -> Unit,
    mostrarDialogoExportar: Boolean,
    onCambiarDialogoExportar: (Boolean) -> Unit
) {
    val resumen by viewModel.resumen.collectAsState()
    // En horizontal la ventana es muy baja: se encogen cabecera y gráficos para que la
    // ficha se vea entera en lugar de cortada.
    val compacta = alturaCompacta()
    // Listas completas para las hojas modales del dashboard
    val vacunas by viewModel.vacunas.collectAsState()
    val preventivos by viewModel.preventivos.collectAsState()
    val tratamientos by viewModel.tratamientos.collectAsState()
    val informes by viewModel.informes.collectAsState()
    val revisiones by viewModel.revisiones.collectAsState()
    val notas by viewModel.notas.collectAsState()
    val pesos by viewModel.pesos.collectAsState()
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Hoja modal activa. Sustituye la navegación profunda: el contenido se abre encima
    // de la ficha en lugar de cambiar de pantalla.
    var activeSheet by remember { mutableStateOf<ActiveSheet>(ActiveSheet.None) }
    // Formulario de alta abierto dentro de una hoja (vacuna, preventivo o tratamiento)
    var formularioActivo by remember { mutableStateOf<FormularioHoja>(FormularioHoja.Ninguno) }
    // Registro existente que se está viendo/editando al tocar una tarjeta de la hoja
    var edicionActiva by remember { mutableStateOf<EdicionHoja>(EdicionHoja.Ninguna) }

    // El diálogo de exportar lo controla la barra superior global (MainScreen), así que su
    // estado llega desde fuera; el resto de estados de la exportación son efímeros.
    var exportando by remember { mutableStateOf(false) }
    var mensajeExportar by remember { mutableStateOf<String?>(null) }
    var versionSeleccionada by rememberSaveable { mutableStateOf(VersionPdf.COMPLETA) }

    // Foto mostrada en el círculo. Se mantiene en estado local para que el cambio se vea
    // al instante, y se sincroniza con la mascota cuando llega una versión nueva desde la BD.
    var rutaFoto by rememberSaveable(mascota.id) { mutableStateOf(mascota.foto) }
    var mostrarOpcionesFoto by rememberSaveable { mutableStateOf(false) }
    var errorFoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mascota.foto) {
        rutaFoto = mascota.foto
    }

    val scope = rememberCoroutineScope()

    val accionesFoto = rememberFotoMascota(
        onFotoSeleccionada = { foto ->
            errorFoto = null
            rutaFoto = foto.ruta
            mostrarOpcionesFoto = false
            mascota.id?.let { id ->
                scope.launch { mascotaViewModel.updateFoto(id, foto.ruta) }
            }
        },
        onError = { mensaje ->
            errorFoto = mensaje
            mostrarOpcionesFoto = false
        }
    )

    val exportador = remember { ExportadorPdf() }
    val abrirArchivo = rememberAbridorArchivo()
    val seleccionarDestino = rememberSelectorDestino(nombreSugerido = "ficha-${mascota.nombre}.pdf") { _, rutaArchivo ->
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
                    onCambiarDialogoExportar(false)
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
                onCambiarDialogoExportar(false)
            }
        }
    }

    // Tarjetas hub de la ficha. El peso va aparte porque tiene su propio diseño.
    // Cada tarjeta tiene su color suave (verde, naranja, azul) para que se distingan de un
    // vistazo, y muestran UNA sola métrica: el aviso que importa, no un recuento de todo.
    val proximas = resumen.vacunasProximas + resumen.preventivosProximos
    val vencidas = resumen.vacunasVencidas + resumen.preventivosVencidos
    val colores = LocalExtrasColors.current

    val tarjetas = listOf(
        ItemFicha(
            icono = Icons.Default.MedicalServices,
            titulo = "Vacunas y preventivos",
            colorFondo = colores.tarjetaVerde,
            colorIcono = colores.iconoTarjetaVerde,
            // Solo el aviso: cuántas están vencidas. El resto del detalle está dentro.
            badges = buildList {
                if (vencidas > 0) {
                    add(BadgeEstado(plural(vencidas, "vencida", "vencidas"), EstadoBadge.AVISO))
                }
                if (proximas > 0) {
                    add(BadgeEstado(plural(proximas, "próxima", "próximas"), EstadoBadge.PROXIMO))
                }
            },
            hoja = "vacunasPreventivos"
        ),
        ItemFicha(
            icono = Icons.Default.MedicalInformation,
            titulo = "Salud y seguimiento",
            colorFondo = colores.tarjetaNaranja,
            colorIcono = colores.iconoTarjetaNaranja,
            // Con saber cuántos tratamientos hay activos basta; los informes y las
            // revisiones se ven al entrar.
            badges = if (resumen.tratamientosActivos > 0) {
                listOf(
                    BadgeEstado(
                        plural(resumen.tratamientosActivos, "activo", "activos"),
                        EstadoBadge.CORRECTO
                    )
                )
            } else {
                emptyList()
            },
            hoja = "saludYSeguimiento"
        ),
        ItemFicha(
            icono = Icons.Default.Note,
            titulo = "A tener en cuenta",
            colorFondo = colores.tarjetaAzul,
            colorIcono = colores.iconoTarjetaAzul,
            // Sin recuento: aquí interesa saber que dentro hay apuntes importantes.
            detalle = "Información importante de la mascota",
            hoja = "notas"
        )
    )

    // Contenido que se desplaza: la tarjeta de peso y las tarjetas hub. Es el mismo tanto en
    // la lista única de vertical como en la columna derecha de apaisado.
    //
    // @param conPeso la tarjeta de peso se omite en la columna izquierda de apaisado, donde
    //   ya tiene su propio sitio fijo: así no aparece dos veces en la misma pantalla.
    fun LazyListScope.contenidoHubFicha(conPeso: Boolean = true) {
        if (conPeso) {
            item(key = "peso") {
                TarjetaPesoFicha(
                    resumen = resumen,
                    compacta = compacta,
                    altura = if (compacta) 88.dp else 130.dp,
                    onClick = { activeSheet = ActiveSheet.Weight }
                )
            }
        }

        if (errorFoto != null) {
            item(key = "error-foto") {
                ErrorFotoTexto(errorFoto!!)
            }
        }

        // Tarjetas hub apiladas: abren su hoja modal
        listaTarjetasFicha(
            items = tarjetas,
            compacta = compacta,
            onAbrirHoja = { clave ->
                activeSheet = when (clave) {
                    "vacunasPreventivos" -> ActiveSheet.Vaccines
                    "saludYSeguimiento" -> ActiveSheet.HealthAndTracking
                    "notas" -> ActiveSheet.Notes
                    else -> ActiveSheet.None
                }
            }
        )

        if (mensajeExportar != null) {
            item(key = "mensaje-exportar") {
                Text(
                    text = mensajeExportar!!,
                    color = if (mensajeExportar!!.contains("Error", ignoreCase = true) || mensajeExportar!!.contains("No se pudo", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // En apaisado la ficha se parte en dos columnas: a la izquierda la identidad de la
    // mascota y su peso, a la derecha el resto de tarjetas con espacio de sobra. Así la
    // tarjeta de peso y las tarjetas hub se ven a la vez y cada una con su tamaño normal.
    if (compacta) {
        // El contenido ocupa el alto que deja libre el hueco del banner, que va anclado al
        // fondo de la pantalla (no arriba), igual que en vertical.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = ESPACIO_ICONOS_FLOTANTES)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .weight(0.42f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CabeceraMascota(
                        nombre = mascota.nombre,
                        rutaFoto = rutaFoto,
                        edad = calcularEdad(mascota.fechaNacimiento, hoy),
                        tamanoAvatar = 84.dp,
                        apilada = true,
                        onCambiarFoto = { mostrarOpcionesFoto = true },
                        menuAcciones = { MenuAccionesFicha(onModificar = onModificar) }
                    )

                    errorFoto?.let { ErrorFotoTexto(it) }
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // La tarjeta de peso abre la columna derecha: es el dato principal.
                    contenidoHubFicha(conPeso = true)
                }
            }

            // Hueco FIJO del banner, anclado abajo y fuera del contenido que se desplaza.
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(ALTO_RESERVA_BANNER)
            )
        }
        return
    }

    // Sin Scaffold: esta pantalla se dibuja DENTRO del Scaffold de MainScreen. Arriba deja
    // una banda libre para los iconos flotantes de volver y compartir, que van superpuestos.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = ESPACIO_ICONOS_FLOTANTES)
    ) {
        CabeceraMascota(
            nombre = mascota.nombre,
            rutaFoto = rutaFoto,
            edad = calcularEdad(mascota.fechaNacimiento, hoy),
            tamanoAvatar = 80.dp,
            apilada = false,
            onCambiarFoto = { mostrarOpcionesFoto = true },
            menuAcciones = { MenuAccionesFicha(onModificar = onModificar) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            // Aire entre el último elemento y el hueco del banner
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            contenidoHubFicha()
        }

        // Hueco FIJO del banner de publicidad: al ser hermano posterior del LazyColumn
        // (y no parte de su contenido) queda siempre anclado al fondo y no se desplaza.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(ALTO_RESERVA_BANNER)
        )
    }

    if (mostrarDialogoExportar) {
        AlertDialog(
            onDismissRequest = { if (!exportando) onCambiarDialogoExportar(false) },
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
                TextButton(onClick = { if (!exportando) onCambiarDialogoExportar(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarOpcionesFoto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarOpcionesFoto = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Foto de ${mascota.nombre}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                if (accionesFoto.soportaCamara) {
                    OpcionFoto(
                        icono = Icons.Default.PhotoCamera,
                        texto = "Hacer una foto",
                        onClick = { accionesFoto.hacerFoto?.invoke() }
                    )
                }

                OpcionFoto(
                    icono = Icons.Default.PhotoLibrary,
                    texto = "Elegir de mis archivos",
                    onClick = accionesFoto.elegirDeArchivos
                )

                if (!rutaFoto.isNullOrBlank()) {
                    OpcionFoto(
                        icono = Icons.Default.Delete,
                        texto = "Quitar la foto",
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            mostrarOpcionesFoto = false
                            errorFoto = null
                            rutaFoto = null
                            mascota.id?.let { id ->
                                scope.launch { mascotaViewModel.updateFoto(id, null) }
                            }
                        }
                    )
                }
            }
        }
    }

    // Hojas modales del dashboard: todo se ve, se modifica y se da de alta aquí dentro, sin
    // cambiar de pantalla (ya no quedan pantallas de detalle a las que navegar).
    when (activeSheet) {
        is ActiveSheet.Vaccines -> HojaVacunasPreventivos(
            vacunas = vacunas,
            preventivos = preventivos,
            nombreMascota = mascota.nombre,
            onCerrar = { activeSheet = ActiveSheet.None },
            onAgregar = { esVacuna ->
                formularioActivo = if (esVacuna) FormularioHoja.Vacuna else FormularioHoja.Preventivo
            },
            onEditarVacuna = { vacuna -> edicionActiva = EdicionHoja.Vacuna(vacuna) },
            onEditarPreventivo = { preventivo -> edicionActiva = EdicionHoja.Preventivo(preventivo) },
            onBorrarVacuna = { id -> viewModel.borrarVacuna(id) },
            onBorrarPreventivo = { id -> viewModel.borrarPreventivo(id) }
        )

        is ActiveSheet.HealthAndTracking -> HojaSaludYSeguimiento(
            tratamientos = tratamientos,
            informes = informes,
            revisiones = revisiones,
            onCerrar = { activeSheet = ActiveSheet.None },
            onAgregar = { formularioActivo = FormularioHoja.Tratamiento },
            onEditarTratamiento = { tratamiento -> edicionActiva = EdicionHoja.Tratamiento(tratamiento) },
            onBorrarTratamiento = { id -> viewModel.borrarTratamiento(id) },
            onEditarRevision = { revision -> edicionActiva = EdicionHoja.Revision(revision) },
            onAgregarRevision = { formularioActivo = FormularioHoja.Revision },
            onBorrarRevision = { id -> viewModel.borrarRevision(id) },
            onEditarInforme = { informe -> edicionActiva = EdicionHoja.Informe(informe) },
            onAgregarInforme = { edicionActiva = EdicionHoja.InformeNuevo },
            onBorrarInforme = { id -> viewModel.borrarInforme(id) }
        )

        is ActiveSheet.Notes -> HojaNotas(
            notas = notas,
            onCerrar = { activeSheet = ActiveSheet.None },
            onAgregar = { texto -> viewModel.agregarNota(texto) },
            onEditar = { id, texto -> viewModel.actualizarNota(id, texto) },
            onBorrar = { id -> viewModel.borrarNota(id) }
        )

        is ActiveSheet.Weight -> HojaPesoDetallada(
            pesos = pesos,
            pesoIdeal = mascota.pesoIdeal,
            onCambiarPesoIdeal = { nuevo ->
                mascota.id?.let { id -> mascotaViewModel.updatePesoIdeal(id, nuevo) }
            },
            onCerrar = { activeSheet = ActiveSheet.None },
            onAgregar = { formularioActivo = FormularioHoja.Peso },
            onEditar = { registro -> edicionActiva = EdicionHoja.Peso(registro) },
            onBorrar = { id -> viewModel.borrarPeso(id) }
        )

        is ActiveSheet.None -> Unit
    }

    // Formularios de alta dentro de las hojas.
    // Si la hoja de vacunas está en la pestaña de preventivos, el mismo botón "+" abre el
    // formulario de preventivo: se decide por el estado de la hoja.
    when (formularioActivo) {
        is FormularioHoja.Vacuna -> FormularioVacuna(
            onCancelar = { formularioActivo = FormularioHoja.Ninguno },
            onGuardar = { nombre, aplicacion, proxima, veterinario, lote ->
                viewModel.agregarVacuna(nombre, aplicacion, proxima, veterinario, lote)
                formularioActivo = FormularioHoja.Ninguno
            }
        )

        is FormularioHoja.Preventivo -> FormularioPreventivo(
            onCancelar = { formularioActivo = FormularioHoja.Ninguno },
            onGuardar = { tipo, nombre, aplicacion, proxima ->
                viewModel.agregarPreventivo(tipo, nombre, aplicacion, proxima)
                formularioActivo = FormularioHoja.Ninguno
            }
        )

        is FormularioHoja.Tratamiento -> FormularioTratamiento(
            onCancelar = { formularioActivo = FormularioHoja.Ninguno },
            onGuardar = { nombre, dosis, frecuencia, inicio, fin ->
                viewModel.agregarTratamiento(nombre, dosis, frecuencia, inicio, fin)
                formularioActivo = FormularioHoja.Ninguno
            }
        )

        is FormularioHoja.Revision -> FormularioRevision(
            onCancelar = { formularioActivo = FormularioHoja.Ninguno },
            onGuardar = { fecha, motivo, diagnostico, notas, veterinario ->
                viewModel.agregarRevision(fecha, motivo, diagnostico, notas, veterinario)
                formularioActivo = FormularioHoja.Ninguno
            }
        )

        is FormularioHoja.Peso -> FormularioPeso(
            onCancelar = { formularioActivo = FormularioHoja.Ninguno },
            onGuardar = { fecha, kilos, notas ->
                viewModel.agregarPeso(fecha, kilos, notas)
                formularioActivo = FormularioHoja.Ninguno
            }
        )

        is FormularioHoja.Ninguno -> Unit
    }

    // Ver y modificar: al tocar una tarjeta de una hoja se abre su formulario relleno.
    when (val edicion = edicionActiva) {
        is EdicionHoja.Vacuna -> FormularioVacuna(
            vacuna = edicion.vacuna,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { nombre, aplicacion, proxima, veterinario, lote ->
                edicion.vacuna.id?.let { id ->
                    viewModel.actualizarVacuna(id, nombre, aplicacion, proxima, veterinario, lote)
                }
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        is EdicionHoja.Preventivo -> FormularioPreventivo(
            preventivo = edicion.preventivo,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { tipo, nombre, aplicacion, proxima ->
                edicion.preventivo.id?.let { id ->
                    viewModel.actualizarPreventivo(id, tipo, nombre, aplicacion, proxima)
                }
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        is EdicionHoja.Tratamiento -> FormularioTratamiento(
            tratamiento = edicion.tratamiento,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { nombre, dosis, frecuencia, inicio, fin ->
                edicion.tratamiento.id?.let { id ->
                    viewModel.actualizarTratamiento(id, nombre, dosis, frecuencia, inicio, fin)
                }
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        is EdicionHoja.Revision -> FormularioRevision(
            revision = edicion.revision,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { fecha, motivo, diagnostico, notas, veterinario ->
                edicion.revision.id?.let { id ->
                    viewModel.actualizarRevision(id, fecha, motivo, diagnostico, notas, veterinario)
                }
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        // Alta de informe: abre el selector de archivo del sistema
        is EdicionHoja.InformeNuevo -> FormularioInforme(
            informe = null,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { tipo, descripcion, fecha, nombreArchivo, rutaArchivo ->
                viewModel.agregarInforme(tipo, descripcion, fecha, nombreArchivo, rutaArchivo)
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        is EdicionHoja.Informe -> FormularioInforme(
            informe = edicion.informe,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { tipo, descripcion, fecha, _, _ ->
                edicion.informe.id?.let { id ->
                    viewModel.actualizarInforme(id, tipo, descripcion, fecha)
                }
                edicionActiva = EdicionHoja.Ninguna
            }
        )

        is EdicionHoja.Peso -> FormularioPeso(
            peso = edicion.peso,
            onCancelar = { edicionActiva = EdicionHoja.Ninguna },
            onGuardar = { fecha, kilos, notas ->
                edicion.peso.id?.let { id ->
                    viewModel.actualizarPeso(id, fecha, kilos, notas)
                }
                edicionActiva = EdicionHoja.Ninguna
            },
            onBorrar = edicion.peso.id?.let { id ->
                {
                    viewModel.borrarPeso(id)
                    edicionActiva = EdicionHoja.Ninguna
                }
            }
        )

        is EdicionHoja.Ninguna -> Unit
    }
}

@Composable
private fun OpcionFoto(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
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

/**
 * Mensaje de error al cargar la foto de la mascota, para no repetirlo en las dos
 * distribuciones de la ficha.
 */
@Composable
private fun ErrorFotoTexto(mensaje: String) {
    Text(
        text = mensaje,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Menú de tres puntos con las acciones de la ficha que no son datos (por ahora, modificar los
 * datos de la mascota). Va en la fila del nombre, alineado arriba, para no gastar una fila
 * entera de botón como antes.
 */
@Composable
private fun MenuAccionesFicha(
    onModificar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { abierto = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Más acciones"
            )
        }

        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            DropdownMenuItem(
                text = { Text("Modificar datos") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                },
                onClick = {
                    abierto = false
                    onModificar()
                }
            )
        }
    }
}

/**
 * Identidad de la mascota: avatar pulsable (abre las opciones de foto), nombre y edad.
 *
 * @param apilada true en la columna izquierda de apaisado: el avatar va encima del nombre y
 *   todo queda centrado, para aprovechar el ancho estrecho de esa columna. En vertical va en
 *   una sola línea, foto a la izquierda y nombre a la derecha, que es lo que menos alto gasta.
 */
@Composable
private fun CabeceraMascota(
    nombre: String,
    rutaFoto: String?,
    edad: String,
    tamanoAvatar: Dp,
    apilada: Boolean,
    onCambiarFoto: () -> Unit,
    menuAcciones: @Composable () -> Unit = {}
) {
    if (apilada) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarMascota(
                nombre = nombre,
                rutaFoto = rutaFoto,
                tamano = tamanoAvatar,
                onCambiarFoto = onCambiarFoto
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = nombre,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = edad,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            menuAcciones()
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = ESPACIO_SOBRE_CABECERA,
                bottom = ESPACIO_BAJO_CABECERA
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarMascota(
            nombre = nombre,
            rutaFoto = rutaFoto,
            tamano = tamanoAvatar,
            onCambiarFoto = onCambiarFoto
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = edad,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Las acciones van arriba de la fila, a la derecha del nombre.
        Box(
            modifier = Modifier
                .align(Alignment.Top)
                .offset(y = (-8).dp)
        ) {
            menuAcciones()
        }
    }
}

/** Tarjeta de peso de la ficha, con los datos del resumen ya extraídos. */
@Composable
private fun TarjetaPesoFicha(
    resumen: ResumenFicha,
    compacta: Boolean,
    altura: Dp,
    onClick: () -> Unit
) {
    TarjetaPesoHero(
        ultimoPeso = resumen.ultimoPeso,
        pesoAnterior = resumen.pesoAnterior,
        pesos = resumen.pesosRecientes,
        altura = altura,
        compacta = compacta,
        onClick = onClick
    )
}

@Composable
private fun TarjetaPesoHero(
    ultimoPeso: Double?,
    pesoAnterior: Double?,
    pesos: List<com.example.virtualpetkmp.Peso>,
    onClick: () -> Unit,
    altura: Dp = 130.dp,
    compacta: Boolean = false
) {
    val extras = LocalExtrasColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            // Filo sutil en lugar de bloque de color: la tarjeta se lee como el dato
            // destacado por su tamaño y su borde, no por un fondo pastel.
            .border(
                width = 1.dp,
                color = extras.tarjetaPesoBorde,
                shape = CardDefaults.shape
            ),
        colors = CardDefaults.cardColors(
            containerColor = extras.tarjetaPesoFondo
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compacta) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // El peso va dentro de su propio contenedor redondeado: da aspecto de
                // "dato destacado" sin necesidad de un icono ni de colorear la tarjeta.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(extras.barraActual.copy(alpha = 0.12f))
                        .padding(
                            horizontal = if (compacta) 10.dp else 14.dp,
                            vertical = if (compacta) 4.dp else 10.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ultimoPeso?.let { "${formatearPesoDosDecimales(it)} kg" } ?: "Sin datos",
                        fontSize = if (compacta) 20.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        // Azul de acento para el dato del peso, que es el color que ya
                        // identifica esta pantalla.
                        color = extras.barraActual,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(if (compacta) 2.dp else 6.dp))

                // En horizontal no cabe la etiqueta bajo el número: se omite y el dato se
                // entiende igual porque la tarjeta es la del peso.
                if (!compacta) {
                    Text(
                        text = "Peso actual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pesos.size >= 2) {
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    SparklinePeso(
                        pesos = pesos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    // Las fechas van fuera del lienzo: así el gráfico nunca les roba sitio
                    // ni se recortan cuando la tarjeta es baja (apaisado).
                    val etiquetas = fechasSparkline(pesos)
                    if (etiquetas != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = etiquetas.first,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = etiquetas.second,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sparkline del peso: una línea suave y minimalista con las últimas pesadas, tres líneas
 * horizontales muy tenues de referencia y un punto destacado en el último dato.
 *
 * A la derecha se rotulan el peso máximo y el mínimo del tramo, y abajo las fechas del
 * primer y último pesaje (con el mes si son del mismo año, o solo el año si no lo son), de
 * modo que se entiende el gráfico sin tener que abrir la hoja detallada.
 *
 * La línea es de izquierda a derecha en orden cronológico, y los puntos se reparten a
 * intervalos iguales (no por fecha), para que no queden huecos grandes si hay pesadas de
 * años distintos.
 */
@Composable
private fun SparklinePeso(
    pesos: List<com.example.virtualpetkmp.Peso>,
    modifier: Modifier = Modifier
) {
    val puntos = remember(pesos) {
        pesos.sortedBy { it.fecha }.takeLast(14)
    }
    // La línea del gráfico es el acento azul de esta pantalla; las referencias y el texto,
    // neutros, para que solo el dato medido tenga color.
    val colorLinea = LocalExtrasColors.current.barraActual.copy(alpha = 0.75f)
    val colorPunto = LocalExtrasColors.current.barraActual
    val colorReferencia = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val estiloEtiqueta = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = colorTexto)
    val densidad = androidx.compose.ui.platform.LocalDensity.current
    // Tope a cada lado del gráfico: el primero y el último punto quedan con aire respecto a
    // su borde, así la línea no arranca pegada a la esquina de la tarjeta.
    val topeIzquierdoPx = with(densidad) { 8.dp.toPx() }
    // Banda reservada a la derecha para rotular el peso máximo y el mínimo, con un hueco de
    // separación respecto al último punto para que el número no se monte encima.
    val bandaDerechaPx = with(densidad) { 32.dp.toPx() }
    val separacionEtiquetaPx = with(densidad) { 6.dp.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (puntos.size < 2) return@Canvas

        val minPeso = puntos.minOf { it.peso }
        val maxPeso = puntos.maxOf { it.peso }
        val rango = (maxPeso - minPeso).toFloat()
        val padV = 4.dp.toPx()
        // El eje X va del tope izquierdo al arranque de la banda de etiquetas: el primer
        // punto queda con aire a su izquierda y el último con la banda a su derecha.
        val inicioGrafico = topeIzquierdoPx.coerceAtMost(size.width / 4f)
        val anchoGrafico = (size.width - bandaDerechaPx - inicioGrafico).coerceAtLeast(1f)
        val altoUtil = (size.height - padV * 2).coerceAtLeast(1f)
        val grosorLinea = 1.dp.toPx()

        // Tres referencias horizontales muy sutiles, para dar sensación de escala. Ocupan
        // todo el ancho para que se lean como la escala del gráfico.
        listOf(0f, 0.5f, 1f).forEach { fraccionY ->
            val y = padV + altoUtil * fraccionY
            drawLine(
                color = colorReferencia,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = grosorLinea
            )
        }

        fun x(indice: Int): Float =
            inicioGrafico + (indice.toFloat() / (puntos.size - 1)) * anchoGrafico

        fun y(peso: Double): Float {
            val normalizado = if (rango <= 0.0001f) 0.5f else ((peso - minPeso) / rango).toFloat()
            // Se invierte porque en pantalla la Y crece hacia abajo.
            return padV + altoUtil * (1f - normalizado)
        }

        val coordenadas = puntos.indices.map { Offset(x(it), y(puntos[it].peso)) }

        // Índices del peso máximo y mínimo: sus etiquetas se alinean con su punto real.
        val indiceMax = puntos.indices.maxBy { puntos[it].peso }
        val indiceMin = puntos.indices.minBy { puntos[it].peso }

        // Curva suave entre los puntos, con los controles acotados al alto del gráfico.
        val path = Path()
        path.moveTo(coordenadas[0].x, coordenadas[0].y)
        val factor = 0.18f
        for (i in 0 until coordenadas.size - 1) {
            val p0 = if (i == 0) coordenadas[i] else coordenadas[i - 1]
            val p1 = coordenadas[i]
            val p2 = coordenadas[i + 1]
            val p3 = if (i + 2 < coordenadas.size) coordenadas[i + 2] else coordenadas[i + 1]
            val c1x = p1.x + (p2.x - p0.x) * factor
            val c1y = (p1.y + (p2.y - p0.y) * factor).coerceIn(padV, padV + altoUtil)
            val c2x = p2.x - (p3.x - p1.x) * factor
            val c2y = (p2.y - (p3.y - p1.y) * factor).coerceIn(padV, padV + altoUtil)
            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }
        drawPath(path = path, color = colorLinea, style = Stroke(width = 2.dp.toPx()))

        // Pesos máximo y mínimo en la banda de la derecha, a la altura de su propio punto
        // pero sin pisarlo: van a partir del final del eje más un huequito.
        val etiquetaMax = formatearPesoDosDecimales(maxPeso)
        val etiquetaMin = formatearPesoDosDecimales(minPeso)
        val medidaMax = textMeasurer.measure(etiquetaMax, style = estiloEtiqueta)
        val medidaMin = textMeasurer.measure(etiquetaMin, style = estiloEtiqueta)
        val xEtiquetas = inicioGrafico + anchoGrafico + separacionEtiquetaPx
        val yMaxEtiqueta = (coordenadas[indiceMax].y - medidaMax.size.height / 2f)
            .coerceIn(0f, (size.height - medidaMax.size.height).coerceAtLeast(0f))
        val yMinEtiqueta = (coordenadas[indiceMin].y - medidaMin.size.height / 2f)
            .coerceIn(0f, (size.height - medidaMin.size.height).coerceAtLeast(0f))
        drawText(
            textMeasurer = textMeasurer,
            text = etiquetaMax,
            topLeft = Offset(xEtiquetas, yMaxEtiqueta),
            style = estiloEtiqueta
        )
        drawText(
            textMeasurer = textMeasurer,
            text = etiquetaMin,
            topLeft = Offset(xEtiquetas, yMinEtiqueta),
            style = estiloEtiqueta
        )

        // Punto destacado en el último peso
        val ultimo = coordenadas.last()
        drawCircle(color = colorPunto, radius = 4.5.dp.toPx(), center = ultimo)
    }
}

/**
 * Formatea un peso con dos decimales (1.7 -> "1.70"). Se hace a mano para no depender
 * de `String.format`, que no existe en commonMain de Kotlin Multiplatform.
 */
private fun formatearPesoDosDecimales(peso: Double): String {
    val centesimas = kotlin.math.round(peso * 100).toLong()
    val signo = if (centesimas < 0) "-" else ""
    val absoluto = kotlin.math.abs(centesimas)
    val entero = absoluto / 100
    val decimales = absoluto % 100
    return "$signo$entero.${decimales.toString().padStart(2, '0')}"
}

private val MESES_CORTOS = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
)

/**
 * Etiquetas de fecha de los extremos del gráfico: la del primer y la del último pesaje del
 * tramo. Devuelve null si no hay pesajes suficientes para dibujar el gráfico.
 */
private fun fechasSparkline(pesos: List<com.example.virtualpetkmp.Peso>): Pair<String, String>? {
    if (pesos.size < 2) return null
    val ordenados = pesos.sortedBy { it.fecha }.takeLast(14)
    val primera = ordenados.first().fecha
    val ultima = ordenados.last().fecha
    return etiquetaFechaCompacta(primera, ultima) to etiquetaFechaCompacta(ultima, ultima)
}

/**
 * Etiqueta de fecha compacta para el gráfico: "sep 26" si el otro extremo del tramo es del
 * mismo año, y solo el año ("2026") si no lo es, para que no se repita en las dos puntas.
 */
private fun etiquetaFechaCompacta(fecha: kotlinx.datetime.LocalDate, otra: kotlinx.datetime.LocalDate): String {
    val mes = MESES_CORTOS[fecha.monthNumber - 1]
    val anioCorto = (fecha.year % 100).toString().padStart(2, '0')
    return if (fecha.year == otra.year) "$mes $anioCorto" else "${fecha.year}"
}

