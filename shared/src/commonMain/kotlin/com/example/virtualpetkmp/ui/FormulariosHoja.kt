package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
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
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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

private fun hoyTexto(): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).toFormatoEuropeo()

/**
 * Diálogo de vacuna, para alta o edición. Si [vacuna] no es null, el formulario llega
 * relleno con sus datos y el botón guarda los cambios.
 * Valida las fechas con [parseFormatoEuropeo] antes de guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioVacuna(
    vacuna: com.example.virtualpetkmp.Vacuna? = null,
    onCancelar: () -> Unit,
    onGuardar: (nombre: String, aplicacion: LocalDate, proxima: LocalDate, veterinario: String?, lote: String?) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(vacuna?.nombre ?: "") }
    var aplicacion by rememberSaveable {
        mutableStateOf(vacuna?.fechaAplicacion?.toFormatoEuropeo() ?: hoyTexto())
    }
    var proxima by rememberSaveable {
        mutableStateOf(vacuna?.fechaProximaDosis?.toFormatoEuropeo() ?: hoyTexto())
    }
    var veterinario by rememberSaveable { mutableStateOf(vacuna?.veterinario ?: "") }
    var lote by rememberSaveable { mutableStateOf(vacuna?.lote ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (vacuna == null) "Añadir vacuna" else "Editar vacuna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la vacuna") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = aplicacion,
                    onValorCambia = { aplicacion = it },
                    etiqueta = "Fecha de aplicación (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                CampoFecha(
                    valor = proxima,
                    onValorCambia = { proxima = it },
                    etiqueta = "Próxima dosis (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    label = { Text("Veterinario (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    label = { Text("Lote (opcional)") },
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
                        error = "Indica el nombre de la vacuna"
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            nombre.trim(),
                            parseFormatoEuropeo(aplicacion),
                            parseFormatoEuropeo(proxima),
                            veterinario.ifBlank { null },
                            lote.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        error = "Las fechas deben tener el formato DD/MM/AAAA"
                    }
                }
            ) {
                Text(if (vacuna == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
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
    var tipo by rememberSaveable { mutableStateOf(preventivo?.tipo ?: "Pipeta") }
    var nombre by rememberSaveable { mutableStateOf(preventivo?.nombre ?: "") }
    var aplicacion by rememberSaveable {
        mutableStateOf(preventivo?.fechaAplicacion?.toFormatoEuropeo() ?: hoyTexto())
    }
    var proxima by rememberSaveable {
        mutableStateOf(preventivo?.fechaProximaDosis?.toFormatoEuropeo() ?: hoyTexto())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (preventivo == null) "Añadir preventivo" else "Editar preventivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // El tipo se elige con chips y no con un desplegable: dentro de un diálogo el
                // menú desplegable no se desplegaba de forma fiable, y los chips son además
                // más rápidos (se ve la opción elegida sin abrir nada).
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Pipeta", "Desparasitación", "Otro").forEach { opcion ->
                        androidx.compose.material3.FilterChip(
                            selected = tipo == opcion,
                            onClick = { tipo = opcion },
                            label = { Text(opcion, fontSize = 12.sp, maxLines = 1) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = aplicacion,
                    onValorCambia = { aplicacion = it },
                    etiqueta = "Fecha de aplicación (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                CampoFecha(
                    valor = proxima,
                    onValorCambia = { proxima = it },
                    etiqueta = "Próxima dosis (DD/MM/AAAA)",
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
                        error = "Indica el nombre del producto"
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            tipo,
                            nombre.trim(),
                            parseFormatoEuropeo(aplicacion),
                            parseFormatoEuropeo(proxima)
                        )
                    } catch (e: Exception) {
                        error = "Las fechas deben tener el formato DD/MM/AAAA"
                    }
                }
            ) {
                Text(if (preventivo == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/** Diálogo de tratamiento, para alta o edición. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioTratamiento(
    tratamiento: com.example.virtualpetkmp.Tratamiento? = null,
    onCancelar: () -> Unit,
    onGuardar: (nombre: String, dosis: String?, frecuencia: String?, inicio: LocalDate, fin: LocalDate?) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(tratamiento?.nombreMedicamento ?: "") }
    var dosis by rememberSaveable { mutableStateOf(tratamiento?.dosis ?: "") }
    var frecuencia by rememberSaveable { mutableStateOf(tratamiento?.frecuencia ?: "") }
    var inicio by rememberSaveable {
        mutableStateOf(tratamiento?.fechaInicio?.toFormatoEuropeo() ?: hoyTexto())
    }
    var fin by rememberSaveable {
        mutableStateOf(tratamiento?.fechaFin?.toFormatoEuropeo() ?: "")
    }
    var cronico by rememberSaveable { mutableStateOf(tratamiento?.fechaFin == null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (tratamiento == null) "Añadir tratamiento" else "Editar tratamiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Medicamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text("Dosis (ej: ½ comprimido)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text("Frecuencia (ej: cada 24h)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = inicio,
                    onValorCambia = { inicio = it },
                    etiqueta = "Fecha de inicio (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = cronico,
                        onClick = { cronico = true },
                        label = { Text("Crónico") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = !cronico,
                        onClick = { cronico = false },
                        label = { Text("Con fecha fin") }
                    )
                }
                if (!cronico) {
                    CampoFecha(
                        valor = fin,
                        onValorCambia = { fin = it },
                        etiqueta = "Fecha de fin (DD/MM/AAAA)",
                        modifier = Modifier.fillMaxWidth(),
                        esError = error != null
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
                        error = "Indica el medicamento"
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            nombre.trim(),
                            dosis.ifBlank { null },
                            frecuencia.ifBlank { null },
                            parseFormatoEuropeo(inicio),
                            if (cronico || fin.isBlank()) null else parseFormatoEuropeo(fin)
                        )
                    } catch (e: Exception) {
                        error = "Las fechas deben tener el formato DD/MM/AAAA"
                    }
                }
            ) {
                Text(if (tratamiento == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
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
    var fecha by rememberSaveable {
        mutableStateOf(revision?.fecha?.toFormatoEuropeo() ?: hoyTexto())
    }
    var motivo by rememberSaveable { mutableStateOf(revision?.motivo ?: "") }
    var diagnostico by rememberSaveable { mutableStateOf(revision?.diagnostico ?: "") }
    var veterinario by rememberSaveable { mutableStateOf(revision?.veterinario ?: "") }
    var notas by rememberSaveable { mutableStateOf(revision?.notas ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (revision == null) "Añadir visita" else "Editar visita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoFecha(
                    valor = fecha,
                    onValorCambia = { fecha = it },
                    etiqueta = "Fecha (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo de la consulta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = diagnostico,
                    onValueChange = { diagnostico = it },
                    label = { Text("Diagnóstico (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    label = { Text("Veterinario (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas de la consulta (opcional)") },
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
                        error = "Indica el motivo de la consulta"
                        return@TextButton
                    }
                    try {
                        onGuardar(
                            parseFormatoEuropeo(fecha),
                            motivo.trim(),
                            diagnostico.ifBlank { null },
                            notas.ifBlank { null },
                            veterinario.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        error = "La fecha debe tener el formato DD/MM/AAAA"
                    }
                }
            ) {
                Text(if (revision == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
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
    onGuardar: (fecha: LocalDate, kilos: Double, notas: String?) -> Unit
) {
    var fecha by rememberSaveable {
        mutableStateOf(peso?.fecha?.toFormatoEuropeo() ?: hoyTexto())
    }
    var kilos by rememberSaveable {
        mutableStateOf(peso?.peso?.let { formatearKilos(it) } ?: "")
    }
    var notas by rememberSaveable { mutableStateOf(peso?.notas ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (peso == null) "Añadir pesaje" else "Editar pesaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoFecha(
                    valor = fecha,
                    onValorCambia = { fecha = it },
                    etiqueta = "Fecha (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    esError = error != null
                )
                OutlinedTextField(
                    value = kilos,
                    onValueChange = { kilos = it },
                    label = { Text("Peso en kg (ej: 12,4)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
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
                    val valor = kilos.trim().replace(',', '.').toDoubleOrNull()
                    if (valor == null || valor <= 0.0) {
                        error = "Indica el peso en kilos (por ejemplo 12,4)"
                        return@TextButton
                    }
                    try {
                        onGuardar(parseFormatoEuropeo(fecha), valor, notas.ifBlank { null })
                    } catch (e: Exception) {
                        error = "La fecha debe tener el formato DD/MM/AAAA"
                    }
                }
            ) {
                Text(if (peso == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/** Kilos con dos decimales y coma, como se escriben en español. */
private fun formatearKilos(valor: Double): String {
    val centesimas = kotlin.math.round(valor * 100).toLong()
    val entero = centesimas / 100
    val decimales = centesimas % 100
    return "$entero,${decimales.toString().padStart(2, '0')}"
}
