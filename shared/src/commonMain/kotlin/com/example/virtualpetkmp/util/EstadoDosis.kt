package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.estado_al_dia
import virtualpetkmp.shared.generated.resources.estado_vencida
import virtualpetkmp.shared.generated.resources.estado_vence_pronto

/**
 * Estado de una dosis pendiente (vacuna o preventivo), según su fecha.
 *
 * Estaba escrito a mano en dos sitios (la lista de la hoja y las pastillas de la ficha), con
 * los mismos plazos y el mismo texto. Centralizarlo evita que se desincronicen: si mañana se
 * cambia un plazo, se cambia en un solo lugar.
 *
 * Los días de antelación **no** son los mismos para vacunas y preventivos: una vacuna avisa
 * con 15 días de margen y un preventivo con 5, así que el plazo entra como parámetro.
 */
enum class EstadoDosis { VENCIDA, PROXIMA, AL_DIA }

/** Calcula el estado de una dosis a partir de hoy y del margen de aviso. */
fun estadoDosis(fechaProximaDosis: LocalDate, hoy: LocalDate, diasAviso: Int): EstadoDosis {
    val limite = LocalDate.fromEpochDays(hoy.toEpochDays() + diasAviso)
    return when {
        fechaProximaDosis < hoy -> EstadoDosis.VENCIDA
        fechaProximaDosis <= limite -> EstadoDosis.PROXIMA
        else -> EstadoDosis.AL_DIA
    }
}

/** Texto del estado, en el idioma activo. */
@Composable
fun textoEstadoDosis(estado: EstadoDosis): String = stringResource(
    when (estado) {
        EstadoDosis.VENCIDA -> Res.string.estado_vencida
        EstadoDosis.PROXIMA -> Res.string.estado_vence_pronto
        EstadoDosis.AL_DIA -> Res.string.estado_al_dia
    }
)
