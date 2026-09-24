package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.virtualpetkmp.util.aDiasEpoch
import com.example.virtualpetkmp.util.localDateDeDiasEpoch
import com.example.virtualpetkmp.util.parseFormatoEuropeo
import com.example.virtualpetkmp.util.toFormatoEuropeo

private const val MILIS_POR_DIA = 86_400_000L

/**
 * Campo de fecha reutilizable: se puede escribir a mano (DD/MM/AAAA) o
 * elegir desde un calendario tocando el icono de la derecha.
 * Pensado para sustituir cualquier campo de fecha de texto simple en la app.
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

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        modifier = modifier,
        singleLine = true,
        isError = esError,
        trailingIcon = {
            IconButton(onClick = { mostrarCalendario = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir del calendario")
            }
        }
    )

    if (mostrarCalendario) {
        val fechaInicial = runCatching { parseFormatoEuropeo(valor) }.getOrNull()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fechaInicial?.let { it.aDiasEpoch() * MILIS_POR_DIA }
        )

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { milis ->
                        val fecha = localDateDeDiasEpoch(milis / MILIS_POR_DIA)
                        onValorCambia(fecha.toFormatoEuropeo())
                    }
                    mostrarCalendario = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}
