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
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.rememberSelectorArchivo
import com.example.virtualpetkmp.util.toFormatoEuropeo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    var tipo by rememberSaveable { mutableStateOf(informe?.tipo ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(informe?.descripcion ?: "") }
    var fechaStr by rememberSaveable {
        mutableStateOf(
            informe?.fecha?.toFormatoEuropeo()
                ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toFormatoEuropeo()
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
        title = { Text(if (informe == null) "Añadir informe" else "Editar informe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (ej: analítica, radiografía)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                CampoFecha(
                    valor = fechaStr,
                    onValorCambia = { fechaStr = it },
                    etiqueta = "Fecha (DD/MM/AAAA)",
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
                            text = if (nombreArchivo.isBlank()) "  Seleccionar archivo" else "  Cambiar archivo",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (nombreArchivo.isNotBlank()) {
                        Text(
                            text = "Archivo elegido: $nombreArchivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Archivo: $nombreArchivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fechaParsed = try {
                        parseFormatoEuropeo(fechaStr)
                    } catch (e: Exception) {
                        errorFecha = "La fecha debe tener el formato DD/MM/AAAA"
                        return@TextButton
                    }
                    onGuardar(tipo, descripcion.ifBlank { null }, fechaParsed, nombreArchivo, rutaArchivo)
                },
                enabled = tipo.isNotBlank() && fechaStr.isNotBlank() &&
                    (informe != null || (nombreArchivo.isNotBlank() && rutaArchivo.isNotBlank()))
            ) {
                Text(if (informe == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
