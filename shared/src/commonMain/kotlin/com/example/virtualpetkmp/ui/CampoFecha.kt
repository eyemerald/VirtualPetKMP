package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.virtualpetkmp.util.aDiasEpoch
import com.example.virtualpetkmp.util.localDateDeDiasEpoch
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.util.parseFechaLocalizada
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_elegir_calendario
import virtualpetkmp.shared.generated.resources.accion_aceptar
import virtualpetkmp.shared.generated.resources.accion_cancelar

private const val MILIS_POR_DIA = 86_400_000L

/**
 * Campo de fecha reutilizable: se puede escribir a mano o elegir desde un calendario tocando
 * el icono de la derecha.
 *
 * El formato que se le pide al usuario depende de la región ([pistaFormatoFecha]): en inglés
 * es MM/DD/AAAA y en el resto DD/MM/AAAA. La fecha se guarda y se lee en ISO, así que el
 * formato solo afecta a lo que se ve y a lo que se escribe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esError: Boolean = false
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val formato = localizacion

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        modifier = modifier,
        singleLine = true,
        isError = esError,
        trailingIcon = {
            IconButton(onClick = { mostrarCalendario = true }) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = stringResource(Res.string.a11y_elegir_calendario)
                )
            }
        }
    )

    if (mostrarCalendario) {
        val fechaInicial = runCatching { parseFechaLocalizada(valor, formato.ordenFechaMostrada) }
            .getOrNull()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fechaInicial?.let { it.aDiasEpoch() * MILIS_POR_DIA }
        )

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { milis ->
                        val fecha = localDateDeDiasEpoch(milis / MILIS_POR_DIA)
                        // Al calendario se le devuelve el texto en el orden que espera el
                        // campo, que es el que luego se vuelve a leer al guardar.
                        onValorCambia(formato.formatearFecha(fecha))
                    }
                    mostrarCalendario = false
                }) {
                    Text(stringResource(Res.string.accion_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text(stringResource(Res.string.accion_cancelar))
                }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}
