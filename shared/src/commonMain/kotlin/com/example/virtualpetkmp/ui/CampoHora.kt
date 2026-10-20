package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.virtualpetkmp.util.RecordatoriosTratamiento
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_elegir_hora
import virtualpetkmp.shared.generated.resources.accion_aceptar
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.form_hora_anadir

/** Hora por defecto del recordatorio cuando el usuario enciende el aviso sin haber elegido una. */
const val HORA_RECORDATORIO_POR_DEFECTO = "09:00"

/**
 * Campo de hora reutilizable, en el mismo estilo que `CampoFecha`: se puede escribir a mano
 * ("08:30") o elegir la hora en un selector tocando el reloj de la derecha.
 *
 * La hora se guarda siempre como **"HH:mm"**, con dos cifras, para que la base de datos y el
 * `AlarmManager` hablen el mismo idioma. El usuario puede escribirla con una cifra ("8:30") y se
 * normaliza al guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    var mostrarSelector by remember { mutableStateOf(false) }
    val (horaInicial, minutoInicial) = RecordatoriosTratamiento.leerHora(valor) ?: (9 to 0)

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        placeholder = { Text(stringResource(Res.string.form_hora_anadir)) },
        modifier = modifier,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { mostrarSelector = true }) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = stringResource(Res.string.a11y_elegir_hora)
                )
            }
        }
    )

    if (mostrarSelector) {
        val estado = rememberTimePickerState(
            initialHour = horaInicial,
            initialMinute = minutoInicial,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { mostrarSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    onValorCambia(RecordatoriosTratamiento.formatearHora(estado.hour, estado.minute))
                    mostrarSelector = false
                }) {
                    Text(stringResource(Res.string.accion_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelector = false }) {
                    Text(stringResource(Res.string.accion_cancelar))
                }
            },
            text = {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = estado)
                }
            }
        )
    }
}
