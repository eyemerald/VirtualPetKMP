package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.borrarArchivo
import com.example.virtualpetkmp.util.copia.ErrorCopia
import com.example.virtualpetkmp.util.copia.ErrorCopiaException
import com.example.virtualpetkmp.util.copia.ExportadorCopia
import com.example.virtualpetkmp.util.copia.ImportadorCopia
import com.example.virtualpetkmp.util.copia.PaqueteExportado
import com.example.virtualpetkmp.util.copia.ResultadoRestauracion
import com.example.virtualpetkmp.util.copia.VistaPreviaPaquete
import com.example.virtualpetkmp.util.copiarArchivoA
import com.example.virtualpetkmp.util.plataformaDeLaApp
import com.example.virtualpetkmp.util.rememberSelectorCopia
import com.example.virtualpetkmp.util.rememberSelectorDestinoCopia
import com.example.virtualpetkmp.util.versionDeLaApp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.comun_cancelar
import virtualpetkmp.shared.generated.resources.comun_cerrar
import virtualpetkmp.shared.generated.resources.copia_error
import virtualpetkmp.shared.generated.resources.copia_explicacion
import virtualpetkmp.shared.generated.resources.copia_faltaban
import virtualpetkmp.shared.generated.resources.copia_guardando
import virtualpetkmp.shared.generated.resources.copia_guardar
import virtualpetkmp.shared.generated.resources.copia_menu
import virtualpetkmp.shared.generated.resources.copia_menu_restaurar
import virtualpetkmp.shared.generated.resources.copia_ok
import virtualpetkmp.shared.generated.resources.copia_ok_detalle
import virtualpetkmp.shared.generated.resources.error_desconocido
import virtualpetkmp.shared.generated.resources.restaurar_aviso
import virtualpetkmp.shared.generated.resources.restaurar_confirmar
import virtualpetkmp.shared.generated.resources.restaurar_elegir
import virtualpetkmp.shared.generated.resources.restaurar_en_curso
import virtualpetkmp.shared.generated.resources.restaurar_error_ajeno
import virtualpetkmp.shared.generated.resources.restaurar_error_datos
import virtualpetkmp.shared.generated.resources.restaurar_error_guardando
import virtualpetkmp.shared.generated.resources.restaurar_error_ilegible
import virtualpetkmp.shared.generated.resources.restaurar_error_nuevo
import virtualpetkmp.shared.generated.resources.restaurar_explicacion
import virtualpetkmp.shared.generated.resources.restaurar_ok
import virtualpetkmp.shared.generated.resources.restaurar_ok_copia
import virtualpetkmp.shared.generated.resources.restaurar_vista_detalle
import virtualpetkmp.shared.generated.resources.restaurar_vista_mascotas
import virtualpetkmp.shared.generated.resources.restaurar_vista_titulo

/**
 * Qué diálogo de copia está abierto.
 *
 * Es un enum y no un booleano porque los dos se abren desde el mismo menú y solo puede haber uno:
 * con dos `Boolean` habría estados imposibles (los dos abiertos).
 */
enum class DialogoCopia { GUARDAR, RESTAURAR, IMPORTAR }

/**
 * Motivo de un fallo de copia, ya clasificado para poder traducirlo al pintarlo.
 *
 * No se guarda el texto del error sino el motivo: es lo mismo que hace el resto de la app con
 * `ErrorApp`, porque un mensaje guardado como texto se queda en el idioma que había al fallar.
 */
internal enum class FalloCopia { ILEGIBLE, AJENO, NUEVO, DATOS, GUARDANDO, ESCRITURA, DESCONOCIDO }

internal fun falloDeError(error: ErrorCopia): FalloCopia = when (error) {
    ErrorCopia.PAQUETE_ILEGIBLE -> FalloCopia.ILEGIBLE
    ErrorCopia.NO_ES_DE_VIRTUALPET -> FalloCopia.AJENO
    ErrorCopia.FORMATO_DEMASIADO_NUEVO -> FalloCopia.NUEVO
    ErrorCopia.DATOS_INCOMPLETOS -> FalloCopia.DATOS
    ErrorCopia.ERROR_GUARDANDO -> FalloCopia.GUARDANDO
    ErrorCopia.ERROR_ESCRIBIENDO -> FalloCopia.ESCRITURA
}

@Composable
internal fun textoDeFallo(fallo: FalloCopia, detalle: String): String {
    val detalleFinal = detalle.ifBlank { stringResource(Res.string.error_desconocido) }
    return when (fallo) {
        FalloCopia.ILEGIBLE -> stringResource(Res.string.restaurar_error_ilegible)
        FalloCopia.AJENO -> stringResource(Res.string.restaurar_error_ajeno)
        FalloCopia.NUEVO -> stringResource(Res.string.restaurar_error_nuevo)
        FalloCopia.DATOS -> stringResource(Res.string.restaurar_error_datos)
        FalloCopia.GUARDANDO -> stringResource(Res.string.restaurar_error_guardando)
        FalloCopia.ESCRITURA, FalloCopia.DESCONOCIDO -> stringResource(Res.string.copia_error, detalleFinal)
    }
}

/** Nombre sugerido del paquete: "VirtualPet-copia-2026-10-22.vpet". */
private fun nombreSugeridoDelDia(): String {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    fun dos(valor: Int) = valor.toString().padStart(2, '0')
    return "VirtualPet-copia-${hoy.year}-${dos(hoy.monthNumber)}-${dos(hoy.dayOfMonth)}.vpet"
}

/**
 * Diálogo "Guardar copia de seguridad".
 *
 * Escribe el paquete en la caché y **copia** el resultado a donde haya elegido el usuario. Se hace
 * en dos pasos porque en Android el destino es un `content://`, que no es un fichero: escribir el
 * ZIP directamente ahí obligaría a cargarlo entero en memoria.
 */
@Composable
fun DialogoGuardarCopia(
    db: VirtualPetDatabase,
    carpetas: CarpetasApp,
    onCerrar: () -> Unit
) {
    val alcance = rememberCoroutineScope()

    var trabajando by remember { mutableStateOf(false) }
    var guardada by remember { mutableStateOf<PaqueteExportado?>(null) }
    var fallo by remember { mutableStateOf<Pair<FalloCopia, String>?>(null) }

    val exportador = remember(db, carpetas) {
        ExportadorCopia(db, carpetas, versionApp = versionDeLaApp(), plataforma = plataformaDeLaApp())
    }

    val elegirDestino = rememberSelectorDestinoCopia(
        nombreSugerido = remember { nombreSugeridoDelDia() },
        onElegido = { _, rutaDestino ->
            alcance.launch {
                trabajando = true
                fallo = null
                guardada = null

                val temporal = "${carpetas.copias}/temporal-${ahoraMillis()}.vpet"
                val resultado = exportador.exportar(temporal)
                val paquete = resultado.getOrNull()

                if (paquete == null) {
                    fallo = FalloCopia.ESCRITURA to resultado.exceptionOrNull()?.message.orEmpty()
                } else if (!copiarArchivoA(rutaDestino, temporal)) {
                    fallo = FalloCopia.ESCRITURA to ""
                } else {
                    guardada = paquete
                }

                // El paquete intermedio sobra en los dos casos: si salió bien, ya está copiado.
                borrarArchivo(temporal)
                trabajando = false
            }
        }
    )

    AlertDialog(
        onDismissRequest = { if (!trabajando) onCerrar() },
        title = { Text(stringResource(Res.string.copia_menu)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.copia_explicacion))
                ResultadoDeGuardar(trabajando, guardada, fallo)
            }
        },
        confirmButton = {
            if (guardada == null) {
                TextButton(onClick = { elegirDestino() }, enabled = !trabajando) {
                    Text(
                        if (trabajando) {
                            stringResource(Res.string.copia_guardando)
                        } else {
                            stringResource(Res.string.copia_guardar)
                        }
                    )
                }
            } else {
                TextButton(onClick = onCerrar) { Text(stringResource(Res.string.comun_cerrar)) }
            }
        },
        dismissButton = {
            if (guardada == null) {
                TextButton(onClick = onCerrar, enabled = !trabajando) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            }
        }
    )
}

/** Lo que se cuenta mientras guarda, cuando termina y cuando falla. */
@Composable
private fun ResultadoDeGuardar(
    trabajando: Boolean,
    guardada: PaqueteExportado?,
    fallo: Pair<FalloCopia, String>?
) {
    when {
        trabajando -> Text(stringResource(Res.string.copia_guardando))

        guardada != null -> {
            Text(stringResource(Res.string.copia_ok), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.copia_ok_detalle, guardada.filas, guardada.archivos))
            if (guardada.archivosQueFaltaban > 0) {
                Text(
                    stringResource(Res.string.copia_faltaban, guardada.archivosQueFaltaban),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        fallo != null -> Text(
            textoDeFallo(fallo.first, fallo.second),
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Diálogo "Restaurar copia".
 *
 * Tiene tres pasos a propósito: elegir el archivo, **enseñar qué trae** y solo entonces confirmar.
 * Restaurar reemplaza todo lo que hay, así que el usuario tiene que ver antes qué va a entrar; la
 * app, por su parte, guarda sola una copia de lo actual antes de tocar nada.
 */
@Composable
fun DialogoRestaurarCopia(
    db: VirtualPetDatabase,
    carpetas: CarpetasApp,
    alRestaurar: suspend () -> Unit,
    onCerrar: () -> Unit
) {
    val alcance = rememberCoroutineScope()

    var paquete by remember { mutableStateOf<String?>(null) }
    var vista by remember { mutableStateOf<VistaPreviaPaquete?>(null) }
    var leyendo by remember { mutableStateOf(false) }
    var restaurando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ResultadoRestauracion?>(null) }
    var fallo by remember { mutableStateOf<Pair<FalloCopia, String>?>(null) }

    val exportador = remember(db, carpetas) {
        ExportadorCopia(db, carpetas, versionApp = versionDeLaApp(), plataforma = plataformaDeLaApp())
    }
    val importador = remember(db, carpetas, exportador) { ImportadorCopia(db, carpetas, exportador) }

    val elegirPaquete = rememberSelectorCopia(
        onElegida = { _, ruta ->
            alcance.launch {
                leyendo = true
                fallo = null
                resultado = null
                vista = null

                val lectura = importador.vistaPrevia(ruta)
                if (lectura.isSuccess) {
                    paquete = ruta
                    vista = lectura.getOrThrow()
                } else {
                    fallo = (
                        (lectura.exceptionOrNull() as? ErrorCopiaException)?.let { falloDeError(it.error) }
                            ?: FalloCopia.DESCONOCIDO
                        ) to lectura.exceptionOrNull()?.message.orEmpty()
                }
                leyendo = false
            }
        }
    )

    AlertDialog(
        onDismissRequest = { if (!leyendo && !restaurando) onCerrar() },
        title = { Text(stringResource(Res.string.copia_menu_restaurar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    restaurando -> Text(stringResource(Res.string.restaurar_en_curso))

                    leyendo -> Unit

                    resultado != null -> {
                        Text(
                            stringResource(
                                Res.string.restaurar_ok,
                                resultado!!.mascotas,
                                resultado!!.filas
                            )
                        )
                        resultado!!.copiaPrevia?.let { ruta ->
                            Text(
                                stringResource(Res.string.restaurar_ok_copia, ruta),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    vista != null -> {
                        Text(
                            stringResource(Res.string.restaurar_vista_titulo),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            stringResource(
                                Res.string.restaurar_vista_detalle,
                                vista!!.mascotas,
                                vista!!.totalFilas,
                                vista!!.archivos
                            )
                        )
                        if (vista!!.nombresMascotas.isNotEmpty()) {
                            Text(
                                stringResource(
                                    Res.string.restaurar_vista_mascotas,
                                    vista!!.nombresMascotas.joinToString(", ")
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            stringResource(Res.string.restaurar_aviso),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    fallo != null -> Text(
                        textoDeFallo(fallo!!.first, fallo!!.second),
                        color = MaterialTheme.colorScheme.error
                    )

                    else -> Text(stringResource(Res.string.restaurar_explicacion))
                }
            }
        },
        confirmButton = {
            when {
                resultado != null -> TextButton(onClick = onCerrar) {
                    Text(stringResource(Res.string.comun_cerrar))
                }

                vista != null -> TextButton(
                    onClick = {
                        val ruta = paquete
                        if (ruta != null) {
                            alcance.launch {
                                restaurando = true
                                val restauracion = importador.restaurar(ruta)
                                restaurando = false

                                if (restauracion.isSuccess) {
                                    resultado = restauracion.getOrThrow()
                                    vista = null
                                    // Los avisos dependen de los datos: hay que reprogramarlos.
                                    alRestaurar()
                                } else {
                                    fallo = (
                                        (restauracion.exceptionOrNull() as? ErrorCopiaException)
                                            ?.let { falloDeError(it.error) }
                                            ?: FalloCopia.DESCONOCIDO
                                        ) to restauracion.exceptionOrNull()?.message.orEmpty()
                                }
                            }
                        }
                    },
                    enabled = !restaurando
                ) {
                    Text(
                        stringResource(Res.string.restaurar_confirmar),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> TextButton(onClick = { elegirPaquete() }, enabled = !leyendo) {
                    Text(stringResource(Res.string.restaurar_elegir))
                }
            }
        },
        dismissButton = {
            if (resultado == null) {
                TextButton(onClick = onCerrar, enabled = !leyendo && !restaurando) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            }
        }
    )
}
