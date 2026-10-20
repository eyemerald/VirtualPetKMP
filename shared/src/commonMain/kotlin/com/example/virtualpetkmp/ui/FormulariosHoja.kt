package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.util.Localizacion
import com.example.virtualpetkmp.util.RecordatoriosTratamiento
import com.example.virtualpetkmp.util.TraduccionesDatos
import com.example.virtualpetkmp.util.UnidadPeso
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import com.example.virtualpetkmp.util.parseKilos
import com.example.virtualpetkmp.util.rememberPermisoNotificaciones
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.accion_eliminar
import virtualpetkmp.shared.generated.resources.accion_guardar
import virtualpetkmp.shared.generated.resources.form_anadir_pesaje
import virtualpetkmp.shared.generated.resources.form_anadir_preventivo
import virtualpetkmp.shared.generated.resources.form_anadir_tratamiento
import virtualpetkmp.shared.generated.resources.form_anadir_vacuna
import virtualpetkmp.shared.generated.resources.form_anadir_visita
import virtualpetkmp.shared.generated.resources.form_campo_tipo
import virtualpetkmp.shared.generated.resources.form_con_fecha_fin
import virtualpetkmp.shared.generated.resources.form_cronico
import virtualpetkmp.shared.generated.resources.form_diagnostico_opcional
import virtualpetkmp.shared.generated.resources.form_dosis
import virtualpetkmp.shared.generated.resources.form_editar_pesaje
import virtualpetkmp.shared.generated.resources.form_editar_preventivo
import virtualpetkmp.shared.generated.resources.form_editar_tratamiento
import virtualpetkmp.shared.generated.resources.form_editar_vacuna
import virtualpetkmp.shared.generated.resources.form_editar_visita
import virtualpetkmp.shared.generated.resources.form_ejemplo_peso
import virtualpetkmp.shared.generated.resources.form_eliminar_pesaje
import virtualpetkmp.shared.generated.resources.form_eliminar_pesaje_mensaje
import virtualpetkmp.shared.generated.resources.form_error_fecha
import virtualpetkmp.shared.generated.resources.form_error_fechas
import virtualpetkmp.shared.generated.resources.form_error_medicamento
import virtualpetkmp.shared.generated.resources.form_error_motivo
import virtualpetkmp.shared.generated.resources.form_error_nombre_producto
import virtualpetkmp.shared.generated.resources.form_error_nombre_vacuna
import virtualpetkmp.shared.generated.resources.form_error_peso
import virtualpetkmp.shared.generated.resources.form_fecha
import virtualpetkmp.shared.generated.resources.form_fecha_aplicacion
import virtualpetkmp.shared.generated.resources.form_fecha_fin
import virtualpetkmp.shared.generated.resources.form_fecha_inicio
import virtualpetkmp.shared.generated.resources.form_frecuencia
import virtualpetkmp.shared.generated.resources.form_hora
import virtualpetkmp.shared.generated.resources.form_hora_anadir
import virtualpetkmp.shared.generated.resources.form_recordarme
import virtualpetkmp.shared.generated.resources.form_recordarme_ayuda
import virtualpetkmp.shared.generated.resources.form_recordarme_sin_permiso
import virtualpetkmp.shared.generated.resources.form_lote_opcional
import virtualpetkmp.shared.generated.resources.form_medicamento
import virtualpetkmp.shared.generated.resources.form_motivo_consulta
import virtualpetkmp.shared.generated.resources.form_nombre_producto
import virtualpetkmp.shared.generated.resources.form_nombre_vacuna
import virtualpetkmp.shared.generated.resources.form_notas_consulta_opcional
import virtualpetkmp.shared.generated.resources.form_notas_opcional
import virtualpetkmp.shared.generated.resources.form_peso_kg
import virtualpetkmp.shared.generated.resources.form_proxima_dosis
import virtualpetkmp.shared.generated.resources.form_veterinario_opcional
import virtualpetkmp.shared.generated.resources.hoja_guardar_cambios

/** Formulario que se está mostrando dentro de una hoja modal de la ficha. */
sealed interface FormularioHoja {
    data object Ninguno : FormularioHoja
    data object Vacuna : FormularioHoja
    data object Preventivo : FormularioHoja
    data object Tratamiento : FormularioHoja
    data object Revision : FormularioHoja
    data object Peso : FormularioHoja
}

/** Qué se está editando dentro de una hoja: nada, un registro nuevo o uno existente. */
sealed interface EdicionHoja {
    data object Ninguna : EdicionHoja
    data class Vacuna(val vacuna: com.example.virtualpetkmp.Vacuna) : EdicionHoja
    data class Preventivo(val preventivo: com.example.virtualpetkmp.Preventivo) : EdicionHoja
    data class Tratamiento(val tratamiento: com.example.virtualpetkmp.Tratamiento) : EdicionHoja
    data class Revision(val revision: com.example.virtualpetkmp.Revision) : EdicionHoja
    data class Informe(val informe: com.example.virtualpetkmp.Informe) : EdicionHoja
    data class Peso(val peso: com.example.virtualpetkmp.Peso) : EdicionHoja
    data object InformeNuevo : EdicionHoja
}

/**
 * Tipos de preventivo que ofrece el formulario.
 *
 * Son las **claves estables que se guardan en la base de datos**: en el desplegable se muestran
 * traducidas (con [TraduccionesDatos]) pero lo que se persiste es esto, así que no se pueden
 * traducir aquí ni cambiar sin migrar los registros que ya existen.
 */
private val TIPOS_PREVENTIVO = listOf("Pipeta", "Desparasitación", "Otro")

/**
 * Fecha de hoy en el formato que espera el campo, que es el de la región.
 * Se resuelve en cada composición para que siga al idioma activo.
 */
/** Fecha de hoy ya formateada, calculada una vez por formulario. */
private fun hoyTexto(formato: Localizacion): String =
    formato.formatearFecha(Clock.System.todayIn(TimeZone.currentSystemDefault()))

/**
 * Textos de error y pistas de formato que usan los formularios de esta hoja.
 *
 * Se agrupan aquí porque los comparten los cinco formularios: cada uno los pide una vez y los
 * usa dentro de sus `onClick`, donde no se pueden leer recursos.
 */
private data class TextosFormulario(
    val pistaFecha: String,
    val errorNombreVacuna: String,
    val errorNombreProducto: String,
    val errorMedicamento: String,
    val errorMotivo: String,
    val errorPeso: String,
    val errorFecha: String,
    val errorFechas: String
)

@Composable
private fun textosDelFormulario(): TextosFormulario {
    val pista = localizacion.pistaFormatoFecha
    return TextosFormulario(
        pistaFecha = pista,
        errorNombreVacuna = stringResource(Res.string.form_error_nombre_vacuna),
        errorNombreProducto = stringResource(Res.string.form_error_nombre_producto),
        errorMedicamento = stringResource(Res.string.form_error_medicamento),
        errorMotivo = stringResource(Res.string.form_error_motivo),
        // La unidad y el ejemplo salen de la región: en Estados Unidos el campo va en libras.
        errorPeso = stringResource(
            Res.string.form_error_peso,
            localizacion.unidadPesoSufijo,
            stringResource(Res.string.form_ejemplo_peso)
        ),
        errorFecha = stringResource(Res.string.form_error_fecha, pista),
        errorFechas = stringResource(Res.string.form_error_fechas, pista)
    )
}

/**
 * Diálogo de vacuna, para alta o edición. Si [vacuna] no es null, el formulario llega
 * relleno con sus datos y el botón guarda los cambios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioVacuna(
    vacuna: com.example.virtualpetkmp.Vacuna? = null,
    onCancelar: () -> Unit,
    onGuardar: (nombre: String, aplicacion: LocalDate, proxima: LocalDate, veterinario: String?, lote: String?) -> Unit
) {
    val formato = localizacion
    val fechaHoy = hoyTexto(formato)
    val textos = textosDelFormulario()
    val pistaFecha = textos.pistaFecha
    val textoErrorNombreVacuna = textos.errorNombreVacuna
    val textoErrorFechas = textos.errorFechas

    var nombre by rememberSaveable { mutableStateOf(vacuna?.nombre ?: "") }
    var aplicacion by rememberSaveable {
        mutableStateOf(vacuna?.fechaAplicacion?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var proxima by rememberSaveable {
        mutableStateOf(vacuna?.fechaProximaDosis?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var veterinario by rememberSaveable { mutableStateOf(vacuna?.veterinario ?: "") }
    var lote by rememberSaveable { mutableStateOf(vacuna?.lote ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (vacuna == null) Res.string.form_anadir_vacuna else Res.string.form_editar_vacuna)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(Res.string.form_nombre_vacuna)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = aplicacion,
                    onValorCambia = { aplicacion = it },
                    etiqueta = stringResource(Res.string.form_fecha_aplicacion, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                CampoFecha(
                    valor = proxima,
                    onValorCambia = { proxima = it },
                    etiqueta = stringResource(Res.string.form_proxima_dosis, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    label = { Text(stringResource(Res.string.form_veterinario_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    label = { Text(stringResource(Res.string.form_lote_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isBlank()) {
                        error = textoErrorNombreVacuna
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            nombre.trim(),
                            parseFechaLocalizada(aplicacion, formato.ordenFechaMostrada)!!,
                            parseFechaLocalizada(proxima, formato.ordenFechaMostrada)!!,
                            veterinario.ifBlank { null },
                            lote.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        error = textoErrorFechas
                    }
                }
            ) {
                Text(stringResource(if (vacuna == null) Res.string.accion_guardar else Res.string.hoja_guardar_cambios))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
        }
    )
}

/** Diálogo de preventivo, para alta o edición. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPreventivo(
    preventivo: com.example.virtualpetkmp.Preventivo? = null,
    onCancelar: () -> Unit,
    onGuardar: (tipo: String, nombre: String, aplicacion: LocalDate, proxima: LocalDate) -> Unit
) {
    val formato = localizacion
    val fechaHoy = hoyTexto(formato)
    val textos = textosDelFormulario()
    val pistaFecha = textos.pistaFecha
    val textoErrorNombreProducto = textos.errorNombreProducto
    val textoErrorMedicamento = textos.errorMedicamento
    val textoErrorMotivo = textos.errorMotivo
    val textoErrorPeso = textos.errorPeso
    val textoErrorFecha = textos.errorFecha
    val textoErrorFechas = textos.errorFechas
    var tipo by rememberSaveable { mutableStateOf(preventivo?.tipo ?: "Pipeta") }
    var nombre by rememberSaveable { mutableStateOf(preventivo?.nombre ?: "") }
    var aplicacion by rememberSaveable {
        mutableStateOf(preventivo?.fechaAplicacion?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var proxima by rememberSaveable {
        mutableStateOf(preventivo?.fechaProximaDosis?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (preventivo == null) Res.string.form_anadir_preventivo else Res.string.form_editar_preventivo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // El tipo se elige con chips y no con un desplegable: dentro de un diálogo el
                // menú desplegable no se desplegaba de forma fiable, y los chips son además
                // más rápidos (se ve la opción elegida sin abrir nada).
                Text(
                    text = stringResource(Res.string.form_campo_tipo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TIPOS_PREVENTIVO.forEach { opcion ->
                        androidx.compose.material3.FilterChip(
                            selected = tipo == opcion,
                            onClick = { tipo = opcion },
                            label = { Text(TraduccionesDatos.etiqueta(opcion, formato.idioma), fontSize = 12.sp, maxLines = 1) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(Res.string.form_nombre_producto)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = aplicacion,
                    onValorCambia = { aplicacion = it },
                    etiqueta = stringResource(Res.string.form_fecha_aplicacion, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                CampoFecha(
                    valor = proxima,
                    onValorCambia = { proxima = it },
                    etiqueta = stringResource(Res.string.form_proxima_dosis, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isBlank()) {
                        error = textoErrorNombreProducto
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            tipo,
                            nombre.trim(),
                            parseFechaLocalizada(aplicacion, formato.ordenFechaMostrada)!!,
                            parseFechaLocalizada(proxima, formato.ordenFechaMostrada)!!
                        )
                    } catch (e: Exception) {
                        error = textoErrorFechas
                    }
                }
            ) {
                Text(stringResource(if (preventivo == null) Res.string.accion_guardar else Res.string.hoja_guardar_cambios))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
        }
    )
}

/** Diálogo de tratamiento, para alta o edición. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioTratamiento(
    tratamiento: com.example.virtualpetkmp.Tratamiento? = null,
    onCancelar: () -> Unit,
    onGuardar: (
        nombre: String,
        dosis: String?,
        frecuencia: String?,
        inicio: LocalDate,
        fin: LocalDate?,
        hora: String?,
        notificar: Boolean
    ) -> Unit
) {
    val formato = localizacion
    val fechaHoy = hoyTexto(formato)
    val textos = textosDelFormulario()
    val pistaFecha = textos.pistaFecha
    val textoErrorNombreProducto = textos.errorNombreProducto
    val textoErrorMedicamento = textos.errorMedicamento
    val textoErrorMotivo = textos.errorMotivo
    val textoErrorPeso = textos.errorPeso
    val textoErrorFecha = textos.errorFecha
    val textoErrorFechas = textos.errorFechas
    // Se pide el permiso de notificaciones al encender el interruptor: es cuando el usuario
    // pide avisos, no al abrir la app sin venir a cuento.
    val pedirPermisoNotificaciones = rememberPermisoNotificaciones()
    var sinPermiso by remember { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf(tratamiento?.nombreMedicamento ?: "") }
    var dosis by rememberSaveable { mutableStateOf(tratamiento?.dosis ?: "") }
    var frecuencia by rememberSaveable { mutableStateOf(tratamiento?.frecuencia ?: "") }
    var hora by rememberSaveable { mutableStateOf(tratamiento?.hora ?: "") }
    var notificar by rememberSaveable { mutableStateOf(tratamiento?.notificar ?: false) }
    var inicio by rememberSaveable {
        mutableStateOf(tratamiento?.fechaInicio?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var fin by rememberSaveable {
        mutableStateOf(tratamiento?.fechaFin?.let { formato.formatearFecha(it) } ?: "")
    }
    var cronico by rememberSaveable { mutableStateOf(tratamiento?.fechaFin == null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (tratamiento == null) Res.string.form_anadir_tratamiento else Res.string.form_editar_tratamiento)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(Res.string.form_medicamento)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text(stringResource(Res.string.form_dosis)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text(stringResource(Res.string.form_frecuencia)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = inicio,
                    onValorCambia = { inicio = it },
                    etiqueta = stringResource(Res.string.form_fecha_inicio, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = cronico,
                        onClick = { cronico = true },
                        label = { Text(stringResource(Res.string.form_cronico)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = !cronico,
                        onClick = { cronico = false },
                        label = { Text(stringResource(Res.string.form_con_fecha_fin)) }
                    )
                }
                if (!cronico) {
                    CampoFecha(
                        valor = fin,
                        onValorCambia = { fin = it },
                        etiqueta = stringResource(Res.string.form_fecha_fin, pistaFecha),
                        modifier = Modifier.fillMaxWidth(),
                        esError = error != null
                    )
                }
                CampoHora(
                    valor = hora,
                    // Si se borra la hora, el recordatorio se apaga solo: sin hora no hay aviso.
                    onValorCambia = {
                        hora = it
                        if (it.isBlank()) notificar = false
                    },
                    etiqueta = stringResource(Res.string.form_hora),
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.form_recordarme),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(Res.string.form_recordarme_ayuda),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = notificar,
                        onCheckedChange = { activo ->
                            if (activo) {
                                // Sin hora no se puede avisar: se propone una y el usuario la
                                // cambia si quiere.
                                if (hora.isBlank()) hora = HORA_RECORDATORIO_POR_DEFECTO
                                sinPermiso = !pedirPermisoNotificaciones()
                            } else {
                                sinPermiso = false
                            }
                            notificar = activo
                        }
                    )
                }
                if (sinPermiso) {
                    Text(
                        text = stringResource(Res.string.form_recordarme_sin_permiso),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isBlank()) {
                        error = textoErrorMedicamento
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            nombre.trim(),
                            dosis.ifBlank { null },
                            frecuencia.ifBlank { null },
                            parseFechaLocalizada(inicio, formato.ordenFechaMostrada)!!,
                            if (cronico || fin.isBlank()) null else parseFechaLocalizada(fin, formato.ordenFechaMostrada)!!,
                            // Se guarda normalizada ("8:30" → "08:30"); si no es una hora válida,
                            // no se guarda hora y por tanto no hay recordatorio.
                            RecordatoriosTratamiento.leerHora(hora)?.let {
                                RecordatoriosTratamiento.formatearHora(it.first, it.second)
                            },
                            notificar && RecordatoriosTratamiento.leerHora(hora) != null
                        )
                    } catch (e: Exception) {
                        error = textoErrorFechas
                    }
                }
            ) {
                Text(stringResource(if (tratamiento == null) Res.string.accion_guardar else Res.string.hoja_guardar_cambios))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
        }
    )
}

/**
 * Diálogo de visita veterinaria, para alta o edición. Muestra y permite modificar el
 * motivo, la fecha, el diagnóstico, el veterinario y las notas de la consulta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioRevision(
    revision: com.example.virtualpetkmp.Revision? = null,
    onCancelar: () -> Unit,
    onGuardar: (fecha: LocalDate, motivo: String, diagnostico: String?, notas: String?, veterinario: String?) -> Unit
) {
    val formato = localizacion
    val fechaHoy = hoyTexto(formato)
    val textos = textosDelFormulario()
    val pistaFecha = textos.pistaFecha
    val textoErrorNombreProducto = textos.errorNombreProducto
    val textoErrorMedicamento = textos.errorMedicamento
    val textoErrorMotivo = textos.errorMotivo
    val textoErrorPeso = textos.errorPeso
    val textoErrorFecha = textos.errorFecha
    val textoErrorFechas = textos.errorFechas
    var fecha by rememberSaveable {
        mutableStateOf(revision?.fecha?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var motivo by rememberSaveable { mutableStateOf(revision?.motivo ?: "") }
    var diagnostico by rememberSaveable { mutableStateOf(revision?.diagnostico ?: "") }
    var veterinario by rememberSaveable { mutableStateOf(revision?.veterinario ?: "") }
    var notas by rememberSaveable { mutableStateOf(revision?.notas ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (revision == null) Res.string.form_anadir_visita else Res.string.form_editar_visita)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoFecha(
                    valor = fecha,
                    onValorCambia = { fecha = it },
                    etiqueta = stringResource(Res.string.form_fecha, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text(stringResource(Res.string.form_motivo_consulta)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = diagnostico,
                    onValueChange = { diagnostico = it },
                    label = { Text(stringResource(Res.string.form_diagnostico_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    label = { Text(stringResource(Res.string.form_veterinario_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text(stringResource(Res.string.form_notas_consulta_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (motivo.isBlank()) {
                        error = textoErrorMotivo
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            parseFechaLocalizada(fecha, formato.ordenFechaMostrada)!!,
                            motivo.trim(),
                            diagnostico.ifBlank { null },
                            notas.ifBlank { null },
                            veterinario.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        error = textoErrorFecha
                    }
                }
            ) {
                Text(stringResource(if (revision == null) Res.string.accion_guardar else Res.string.hoja_guardar_cambios))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
        }
    )
}

/**
 * Diálogo de pesaje, para alta o edición. Acepta la coma decimal, porque en español se
 * escribe "12,4 kg".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPeso(
    peso: com.example.virtualpetkmp.Peso? = null,
    onCancelar: () -> Unit,
    onGuardar: (fecha: LocalDate, kilos: Double, notas: String?) -> Unit,
    /**
     * Solo se usa al EDITAR un pesaje ya guardado: permite borrarlo desde este mismo modal,
     * que es donde el usuario espera encontrarlo al pulsar el registro en el histórico.
     */
    onBorrar: (() -> Unit)? = null
) {
    val formato = localizacion
    val fechaHoy = hoyTexto(formato)
    val textos = textosDelFormulario()
    val pistaFecha = textos.pistaFecha
    val textoErrorNombreProducto = textos.errorNombreProducto
    val textoErrorMedicamento = textos.errorMedicamento
    val textoErrorMotivo = textos.errorMotivo
    val textoErrorPeso = textos.errorPeso
    val textoErrorFecha = textos.errorFecha
    val textoErrorFechas = textos.errorFechas
    var fecha by rememberSaveable {
        mutableStateOf(peso?.fecha?.let { formato.formatearFecha(it) } ?: fechaHoy)
    }
    var kilos by rememberSaveable {
        mutableStateOf(peso?.peso?.let { formato.formatearPeso(it) } ?: "")
    }
    var notas by rememberSaveable { mutableStateOf(peso?.notas ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmandoBorrado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (peso == null) Res.string.form_anadir_pesaje else Res.string.form_editar_pesaje)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoFecha(
                    valor = fecha,
                    onValorCambia = { fecha = it },
                    etiqueta = stringResource(Res.string.form_fecha, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = kilos,
                    onValueChange = { kilos = it },
                    label = { Text(stringResource(Res.string.form_peso_kg, formato.unidadPesoSufijo, stringResource(Res.string.form_ejemplo_peso))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text(stringResource(Res.string.form_notas_opcional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valorMostrado = parseKilos(kilos)
                    if (valorMostrado == null || valorMostrado <= 0.0) {
                        error = textoErrorPeso
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            parseFechaLocalizada(fecha, formato.ordenFechaMostrada)!!,
                            UnidadPeso.aKilos(valorMostrado, formato.unidadPeso),
                            notas.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        error = textoErrorFecha
                    }
                }
            ) {
                Text(stringResource(if (peso == null) Res.string.accion_guardar else Res.string.hoja_guardar_cambios))
            }
        },
        dismissButton = {
            Row {
                if (onBorrar != null) {
                    TextButton(
                        onClick = { confirmandoBorrado = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(Res.string.accion_eliminar))
                    }
                }
                TextButton(onClick = onCancelar) { Text(stringResource(Res.string.accion_cancelar)) }
            }
        }
    )

    if (confirmandoBorrado && onBorrar != null) {
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text(stringResource(Res.string.form_eliminar_pesaje)) },
            text = { Text(stringResource(Res.string.form_eliminar_pesaje_mensaje)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoBorrado = false
                        onBorrar()
                    }
                ) {
                    Text(stringResource(Res.string.accion_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text(stringResource(Res.string.accion_cancelar)) }
            }
        )
    }
}

