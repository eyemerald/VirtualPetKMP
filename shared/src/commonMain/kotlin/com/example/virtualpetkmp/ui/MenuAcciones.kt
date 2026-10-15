package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_acciones_de
import virtualpetkmp.shared.generated.resources.accion_editar
import virtualpetkmp.shared.generated.resources.accion_eliminar

/**
 * Menú de tres puntos con **Editar** y **Eliminar**, el mismo en toda la app.
 *
 * Antes cada lista tenía sus propios iconos sueltos (un lápiz y una papelera, o solo el lápiz),
 * y la misma acción se veía distinta según la pantalla: en la lista de mascotas era un menú, en
 * las hojas de la ficha eran dos iconos, y en las tarjetas de veterinario otros dos. Además, el
 * icono de borrar a la vista invita a pulsarlo de refilón, y borrar es lo que menos se hace.
 *
 * Aquí se centraliza para que no vuelva a divergir: una fila con acciones de edición y borrado
 * usa esto y punto.
 *
 * @param nombre de qué es el menú ("Rabia", "Clínica Norte"...), que va en la descripción para
 *   los lectores de pantalla: sin él, un lector diría "acciones" y no "acciones de Rabia".
 * @param onEditar si es `null`, la opción no se dibuja (hay listas sin borrado, o sin edición).
 * @param onBorrar si es `null`, tampoco. Cuando está, se pinta en rojo: es la única acción
 *   destructiva del menú.
 */
@Composable
fun MenuAccionesItem(
    nombre: String,
    onEditar: (() -> Unit)? = null,
    onBorrar: (() -> Unit)? = null
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.a11y_acciones_de, nombre)
            )
        }

        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (onEditar != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.accion_editar)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    },
                    onClick = {
                        abierto = false
                        onEditar()
                    }
                )
            }

            if (onBorrar != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.accion_eliminar),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        abierto = false
                        onBorrar()
                    }
                )
            }
        }
    }
}
