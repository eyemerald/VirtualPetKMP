package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    // Sin Scaffold: esta pantalla se dibuja DENTRO del Scaffold de MainScreen, que ya
    // aplica los insets del sistema y aporta la barra superior (volver y compartir).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Identidad de la mascota: foto pulsable, nombre y edad. La foto sigue siendo el
        // acceso al selector de foto; el volver y el compartir viven en la barra global.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ESPACIO_SOBRE_CABECERA, bottom = ESPACIO_BAJO_CABECERA),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarMascota(
                nombre = mascota.nombre,
                rutaFoto = rutaFoto,
                tamano = 100.dp,
                onCambiarFoto = { mostrarOpcionesFoto = true }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mascota.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = calcularEdad(mascota.fechaNacimiento, hoy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            // Aire entre el último elemento y el hueco del banner
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // La tarjeta de peso es el primer elemento que se desplaza
            item(key = "peso") {
                TarjetaPesoHero(
                    ultimoPeso = resumen.ultimoPeso,
                    pesoAnterior = resumen.pesoAnterior,
                    pesos = resumen.pesosRecientes,
                    onClick = { activeSheet = ActiveSheet.Weight }
                )
            }

            if (errorFoto != null) {
                item(key = "error-foto") {
                    Text(
                        text = errorFoto!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tarjetas hub apiladas: abren su hoja modal
            listaTarjetasFicha(
                items = tarjetas,
                onAbrirHoja = { clave ->
                    activeSheet = when (clave) {
                        "vacunasPreventivos" -> ActiveSheet.Vaccines
                        "saludYSeguimiento" -> ActiveSheet.HealthAndTracking
                        "notas" -> ActiveSheet.Notes
                        else -> ActiveSheet.None
                    }
                }
            )

            item(key = "modificar") {
                Button(
                    onClick = onModificar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modificar datos")
                }
            }

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

@Composable
private fun TarjetaPesoHero(
    ultimoPeso: Double?,
    pesoAnterior: Double?,
    pesos: List<com.example.virtualpetkmp.Peso>,
    onClick: () -> Unit
) {
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
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // El peso va dentro de su propio contenedor redondeado: da aspecto de
                // "dato destacado" sin necesidad de un icono ni de colorear la tarjeta.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ultimoPeso?.let { "${formatearPesoDosDecimales(it)} kg" } ?: "Sin datos",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Peso actual",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }

            if (pesos.size >= 2) {
                BarrasPeso(
                    pesos = pesos,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Vista compacta del peso en barras: una barra por pesaje, en orden cronológico y pegadas
 * unas a otras (sin separar por fechas), para ver de un vistazo si ha habido una bajada o
 * una subida importante. La barra más reciente va en azul y el resto en oscuro.
 */
@Composable
private fun BarrasPeso(
    pesos: List<com.example.virtualpetkmp.Peso>,
    modifier: Modifier = Modifier
) {
    val recientes = remember(pesos) {
        pesos.sortedBy { it.fecha }.takeLast(12)
    }
    val colorBarra = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.45f)
    val colorActual = LocalExtrasColors.current.barraActual
    val radioEsquina = with(androidx.compose.ui.platform.LocalDensity.current) { 2.dp.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (recientes.isEmpty()) return@Canvas

        val minPeso = recientes.minOf { it.peso }
        val maxPeso = recientes.maxOf { it.peso }
        val rango = (maxPeso - minPeso).toFloat()

        // La barra más baja nunca desaparece: mantiene un 25% de la altura útil.
        fun fraccion(peso: Double): Float {
            val normalizado = if (rango <= 0.0001f) 1f else ((peso - minPeso) / rango).toFloat()
            return 0.25f + normalizado * 0.75f
        }

        val hueco = 3.dp.toPx()
        val anchoTotal = size.width
        val anchoBarra = ((anchoTotal - hueco * (recientes.size - 1)) / recientes.size)
            .coerceAtLeast(1f)
        val altoUtil = size.height

        recientes.forEachIndexed { indice, registro ->
            val altoBarra = (altoUtil * fraccion(registro.peso)).coerceAtLeast(2.dp.toPx())
            val x = indice * (anchoBarra + hueco)
            val esUltima = indice == recientes.lastIndex
            drawRoundRect(
                color = if (esUltima) colorActual else colorBarra,
                topLeft = Offset(x, altoUtil - altoBarra),
                size = androidx.compose.ui.geometry.Size(anchoBarra, altoBarra),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radioEsquina, radioEsquina)
            )
        }
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

