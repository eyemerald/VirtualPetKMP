package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import com.example.virtualpetkmp.util.rememberSelectorArchivo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.accion_guardar
import virtualpetkmp.shared.generated.resources.form_error_fecha
import virtualpetkmp.shared.generated.resources.form_fecha
import virtualpetkmp.shared.generated.resources.informe_anadir
import virtualpetkmp.shared.generated.resources.informe_archivo
import virtualpetkmp.shared.generated.resources.informe_archivo_elegido
import virtualpetkmp.shared.generated.resources.informe_cambiar_archivo
import virtualpetkmp.shared.generated.resources.informe_campo_descripcion
import virtualpetkmp.shared.generated.resources.informe_campo_tipo
import virtualpetkmp.shared.generated.resources.informe_editar
import virtualpetkmp.shared.generated.resources.informe_guardar_cambios
import virtualpetkmp.shared.generated.resources.informe_seleccionar_archivo

/**
 * Formulario de informe, para alta o edición, con selector de archivo del sistema.
 *
 * Es el mismo diálogo que usa la pantalla de Informes y el que se abre desde la hoja de
 * la ficha, para no tener dos formularios distintos del mismo dato.
 *
 * @param informe si no es null, edita ese informe (el archivo no se puede cambiar; para
 *   eso está el alta de uno nuevo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioInforme(
    informe: Informe? = null,
    onCancelar: () -> Unit,
    onGuardar: (tipo: String, descripcion: String?, fecha: LocalDate, nombreArchivo: String, rutaArchivo: String) -> Unit
) {
    // Orden de fecha y separador decimal del usuario; el error se resuelve fuera del onClick.
    val formato = localizacion
    val pistaFecha = formato.pistaFormatoFecha
    val textoErrorFecha = stringResource(Res.string.form_error_fecha, pistaFecha)

    var tipo by rememberSaveable { mutableStateOf(informe?.tipo ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(informe?.descripcion ?: "") }
    var fechaStr by rememberSaveable {
        mutableStateOf(
            formato.formatearFecha(
                informe?.fecha
                    ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            )
        )
    }
    var nombreArchivo by rememberSaveable { mutableStateOf(informe?.nombreArchivo ?: "") }
    var rutaArchivo by rememberSaveable { mutableStateOf(informe?.rutaArchivo ?: "") }
    // errorFecha NO se migra: es validación efímera del último intento de guardado.
    var errorFecha by remember { mutableStateOf<String?>(null) }

    val seleccionarArchivo = rememberSelectorArchivo { nombre, ruta ->
        nombreArchivo = nombre
        rutaArchivo = ruta
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(stringResource(if (informe == null) Res.string.informe_anadir else Res.string.informe_editar))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text(stringResource(Res.string.informe_campo_tipo)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(Res.string.informe_campo_descripcion)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = fechaStr,
                    onValorCambia = { fechaStr = it },
                    etiqueta = stringResource(Res.string.form_fecha, pistaFecha),
                    modifier = Modifier.fillMaxWidth(),
                    esError = errorFecha != null
                )
                if (errorFecha != null) {
                    Text(
                        text = errorFecha!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (informe == null) {
                    OutlinedButton(
                        onClick = seleccionarArchivo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Text(
                            text = if (nombreArchivo.isBlank()) {
                                "  " + stringResource(Res.string.informe_seleccionar_archivo)
                            } else {
                                "  " + stringResource(Res.string.informe_cambiar_archivo)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (nombreArchivo.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.informe_archivo_elegido, nombreArchivo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.informe_archivo, nombreArchivo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Mismo criterio que en el resto de formularios: se acepta el orden de la
                    // región y, como respaldo, el otro (evita fallar por escribir DD/MM en inglés).
                    val fechaParsed = parseFechaLocalizada(fechaStr, formato.ordenFechaMostrada)
                    if (fechaParsed == null) {
                        errorFecha = textoErrorFecha
                        return@TextButton
                    }
                    onGuardar(tipo, descripcion.ifBlank { null }, fechaParsed, nombreArchivo, rutaArchivo)
                },
                enabled = tipo.isNotBlank() && fechaStr.isNotBlank() &&
                    (informe != null || (nombreArchivo.isNotBlank() && rutaArchivo.isNotBlank()))
            ) {
                Text(
                    stringResource(
                        if (informe == null) Res.string.accion_guardar else Res.string.informe_guardar_cambios
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(Res.string.accion_cancelar))
            }
        }
    )
}
