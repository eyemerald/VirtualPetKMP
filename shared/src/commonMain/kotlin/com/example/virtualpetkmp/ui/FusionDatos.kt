package com.example.virtualpetkmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.copia.DecisionConflicto
import com.example.virtualpetkmp.util.copia.ErrorCopiaException
import com.example.virtualpetkmp.util.copia.ExportadorCopia
import com.example.virtualpetkmp.util.copia.ImportadorFusion
import com.example.virtualpetkmp.util.copia.PlanFusion
import com.example.virtualpetkmp.util.copia.ResultadoFusion
import com.example.virtualpetkmp.util.plataformaDeLaApp
import com.example.virtualpetkmp.util.rememberSelectorCopia
import com.example.virtualpetkmp.util.versionDeLaApp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.comun_cancelar
import virtualpetkmp.shared.generated.resources.comun_cerrar
import virtualpetkmp.shared.generated.resources.fusion_aplicando
import virtualpetkmp.shared.generated.resources.fusion_aplicar
import virtualpetkmp.shared.generated.resources.fusion_archivos
import virtualpetkmp.shared.generated.resources.fusion_conflicto_texto
import virtualpetkmp.shared.generated.resources.fusion_conflictos_titulo
import virtualpetkmp.shared.generated.resources.fusion_decision_omitir
import virtualpetkmp.shared.generated.resources.fusion_decision_renombrar
import virtualpetkmp.shared.generated.resources.fusion_decision_sobrescribir
import virtualpetkmp.shared.generated.resources.fusion_explicacion
import virtualpetkmp.shared.generated.resources.fusion_menu
import virtualpetkmp.shared.generated.resources.fusion_ok
import virtualpetkmp.shared.generated.resources.fusion_omitidas
import virtualpetkmp.shared.generated.resources.fusion_plan_detalle
import virtualpetkmp.shared.generated.resources.fusion_plan_titulo
import virtualpetkmp.shared.generated.resources.fusion_sin_cambios
import virtualpetkmp.shared.generated.resources.restaurar_elegir

/**
 * Diálogo "Importar y fusionar": traer un paquete **sin borrar nada** de lo que ya hay.
 *
 * Tiene los mismos tres pasos que restaurar (elegir, ver qué va a cambiar y confirmar), más uno
 * propio: cuando una mascota del paquete se llama igual que una de aquí pero **no es la misma**,
 * hay que decidir qué hacer con ella (renombrar, sobrescribir u omitir). La app no lo adivina
 * porque las dos respuestas razonables llevan a resultados muy distintos.
 */
@Composable
fun DialogoImportarFusion(
    db: VirtualPetDatabase,
    carpetas: CarpetasApp,
    alFusionar: suspend () -> Unit,
    onCerrar: () -> Unit
) {
    val alcance = rememberCoroutineScope()

    var paquete by remember { mutableStateOf<String?>(null) }
    var plan by remember { mutableStateOf<PlanFusion?>(null) }
    var leyendo by remember { mutableStateOf(false) }
    var aplicando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ResultadoFusion?>(null) }
    var fallo by remember { mutableStateOf<Pair<FalloCopia, String>?>(null) }
    var decisiones by remember { mutableStateOf<Map<String, DecisionConflicto>>(emptyMap()) }

    val exportador = remember(db, carpetas) {
        ExportadorCopia(db, carpetas, versionApp = versionDeLaApp(), plataforma = plataformaDeLaApp())
    }
    val fusionador = remember(db, carpetas, exportador) { ImportadorFusion(db, carpetas, exportador) }

    val elegirPaquete = rememberSelectorCopia(
        onElegida = { _, ruta ->
            alcance.launch {
                leyendo = true
                fallo = null
                resultado = null
                plan = null

                val analisis = fusionador.analizar(ruta)
                if (analisis.isSuccess) {
                    paquete = ruta
                    plan = analisis.getOrThrow()
                    // Por defecto, renombrar: es la opción que no pierde nada de ninguno de los dos
                    // lados, y la que el usuario entiende mejor ("son dos mascotas distintas").
                    decisiones = analisis.getOrThrow().conflictos.associate {
                        it.uuidPaquete to DecisionConflicto.RENOMBRAR
                    }
                } else {
                    fallo = (
                        (analisis.exceptionOrNull() as? ErrorCopiaException)?.let { falloDeError(it.error) }
                            ?: FalloCopia.DESCONOCIDO
                        ) to analisis.exceptionOrNull()?.message.orEmpty()
                }
                leyendo = false
            }
        }
    )

    AlertDialog(
        onDismissRequest = { if (!leyendo && !aplicando) onCerrar() },
        title = { Text(stringResource(Res.string.fusion_menu)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            ) {
                when {
                    aplicando -> Text(stringResource(Res.string.fusion_aplicando))

                    leyendo -> Unit

                    resultado != null -> {
                        val r = resultado!!
                        if (r.filasNuevas + r.filasActualizadas == 0) {
                            Text(stringResource(Res.string.fusion_sin_cambios))
                        } else {
                            Text(
                                stringResource(Res.string.fusion_ok, r.filasNuevas, r.filasActualizadas)
                            )
                            if (r.archivosCopiados > 0) {
                                Text(
                                    stringResource(Res.string.fusion_archivos, r.archivosCopiados),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (r.conflictosOmitidos > 0) {
                            Text(
                                stringResource(Res.string.fusion_omitidas, r.conflictosOmitidos),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    plan != null -> {
                        val p = plan!!
                        Text(stringResource(Res.string.fusion_plan_titulo), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(
                                Res.string.fusion_plan_detalle,
                                p.filasNuevas,
                                p.filasAModificar,
                                p.filasIguales
                            )
                        )

                        if (p.conflictos.isNotEmpty()) {
                            Text(
                                stringResource(Res.string.fusion_conflictos_titulo),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            p.conflictos.forEach { conflicto ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(
                                            Res.string.fusion_conflicto_texto,
                                            conflicto.nombrePaquete
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OpcionConflicto(
                                            texto = stringResource(Res.string.fusion_decision_renombrar),
                                            elegida = decisiones[conflicto.uuidPaquete] == DecisionConflicto.RENOMBRAR
                                        ) {
                                            decisiones = decisiones + (conflicto.uuidPaquete to DecisionConflicto.RENOMBRAR)
                                        }
                                        OpcionConflicto(
                                            texto = stringResource(Res.string.fusion_decision_sobrescribir),
                                            elegida = decisiones[conflicto.uuidPaquete] == DecisionConflicto.SOBRESCRIBIR
                                        ) {
                                            decisiones = decisiones + (conflicto.uuidPaquete to DecisionConflicto.SOBRESCRIBIR)
                                        }
                                        OpcionConflicto(
                                            texto = stringResource(Res.string.fusion_decision_omitir),
                                            elegida = decisiones[conflicto.uuidPaquete] == DecisionConflicto.OMITIR
                                        ) {
                                            decisiones = decisiones + (conflicto.uuidPaquete to DecisionConflicto.OMITIR)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    fallo != null -> Text(
                        textoDeFallo(fallo!!.first, fallo!!.second),
                        color = MaterialTheme.colorScheme.error
                    )

                    else -> Text(stringResource(Res.string.fusion_explicacion))
                }
            }
        },
        confirmButton = {
            when {
                resultado != null -> TextButton(onClick = onCerrar) {
                    Text(stringResource(Res.string.comun_cerrar))
                }

                plan != null -> TextButton(
                    onClick = {
                        val ruta = paquete
                        if (ruta != null) {
                            alcance.launch {
                                aplicando = true
                                val fusion = fusionador.aplicar(ruta, decisiones)
                                aplicando = false

                                if (fusion.isSuccess) {
                                    resultado = fusion.getOrThrow()
                                    plan = null
                                    // Los datos cambian: hay que reprogramar los avisos.
                                    alFusionar()
                                } else {
                                    fallo = (
                                        (fusion.exceptionOrNull() as? ErrorCopiaException)
                                            ?.let { falloDeError(it.error) }
                                            ?: FalloCopia.DESCONOCIDO
                                        ) to fusion.exceptionOrNull()?.message.orEmpty()
                                }
                            }
                        }
                    },
                    enabled = !aplicando
                ) {
                    Text(stringResource(Res.string.fusion_aplicar))
                }

                else -> TextButton(onClick = { elegirPaquete() }, enabled = !leyendo) {
                    Text(stringResource(Res.string.restaurar_elegir))
                }
            }
        },
        dismissButton = {
            if (resultado == null) {
                TextButton(onClick = onCerrar, enabled = !leyendo && !aplicando) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            }
        }
    )
}

/**
 * Una de las tres salidas de un conflicto de nombre.
 *
 * Se marca la elegida con el color de acento y en negrita: sin eso, con tres botones iguales al
 * lado, no se sabe qué va a pasar al final.
 */
@Composable
private fun OpcionConflicto(
    texto: String,
    elegida: Boolean,
    onElegir: () -> Unit
) {
    TextButton(onClick = onElegir) {
        Text(
            text = texto,
            style = if (elegida) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
