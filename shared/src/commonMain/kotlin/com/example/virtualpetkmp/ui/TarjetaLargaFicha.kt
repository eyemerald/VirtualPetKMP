package com.example.virtualpetkmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors

/**
 * Colores de estado de un badge/pastilla: verde si está al día, ámbar si vence pronto,
 * rojo si está vencido y neutro si es solo informativo.
 */
enum class EstadoBadge { CORRECTO, PROXIMO, AVISO, NEUTRO }

/**
 * Badge de estado "de un vistazo", dibujado como pastilla con su propio contenedor:
 * fondo del color semántico y texto en negrita. El contenedor (y no solo el color del
 * texto) es lo que hace que el estado se distinga sin depender de percibir el color.
 */
data class BadgeEstado(
    val texto: String,
    val estado: EstadoBadge = EstadoBadge.NEUTRO
)

/** Un número con su pie de texto dentro de una tarjeta larga. */
data class ContadorTarjeta(
    val valor: Int,
    val pie: String,
    val alerta: Boolean = false
)

/**
 * Contenido de una tarjeta hub de la ficha.
 *
 * @param detalle línea informativa extra (por ejemplo el apunte o la última nota).
 * @param badges pastillas de estado; se muestran ordenadas por severidad, máximo 3.
 * @param colorFondo color suave propio de la tarjeta, para darle identidad.
 * @param colorIcono color del icono de la tarjeta.
 * @param hoja clave de la hoja modal que abre la tarjeta al pulsarla.
 */
data class ItemFicha(
    val icono: ImageVector,
    val titulo: String,
    val subtitulo: String? = null,
    val detalle: String? = null,
    val badges: List<BadgeEstado> = emptyList(),
    val contadores: List<ContadorTarjeta> = emptyList(),
    val colorFondo: Color,
    val colorIcono: Color,
    val hoja: String? = null
)

/**
 * Pastilla de estado: fondo del color semántico y texto en negrita.
 *
 * El contenedor (y no solo el color del texto) es lo que hace legible el estado sin
 * depender de distinguir colores, así que se usa el token del tema en lugar de un hex
 * fijo: así funciona igual en tema claro y oscuro.
 */
@Composable
private fun PastillaEstado(badge: BadgeEstado) {
    val esquema = MaterialTheme.colorScheme
    val extras = LocalExtrasColors.current

    val colorFondo = when (badge.estado) {
        EstadoBadge.CORRECTO -> esquema.primaryContainer
        EstadoBadge.PROXIMO -> extras.proximaContenedor
        EstadoBadge.AVISO -> esquema.errorContainer
        // Los datos informativos no llevan contenedor: si lo llevaran parecerían botones.
        EstadoBadge.NEUTRO -> Color.Transparent
    }
    val colorTexto = when (badge.estado) {
        EstadoBadge.CORRECTO -> esquema.onPrimaryContainer
        EstadoBadge.PROXIMO -> extras.onProximaContenedor
        EstadoBadge.AVISO -> esquema.onErrorContainer
        EstadoBadge.NEUTRO -> esquema.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colorFondo)
            .padding(
                horizontal = if (badge.estado == EstadoBadge.NEUTRO) 0.dp else 10.dp,
                vertical = if (badge.estado == EstadoBadge.NEUTRO) 0.dp else 3.dp
            )
    ) {
        Text(
            text = badge.texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorTexto
        )
    }
}

@Composable
fun TarjetaLargaFicha(
    icono: ImageVector,
    titulo: String,
    subtitulo: String?,
    detalle: String?,
    badges: List<BadgeEstado>,
    contadores: List<ContadorTarjeta>,
    colorFondo: Color,
    colorIcono: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compacta: Boolean = false
) {
    val colorNeutro = MaterialTheme.colorScheme.onSurface
    val colorSuave = MaterialTheme.colorScheme.onSurfaceVariant
    val colorAviso = MaterialTheme.colorScheme.error

    Card(
        modifier = modifier
            .fillMaxWidth()
            // Filo muy sutil: con el fondo casi blanco, sin él las tarjetas se
            // desdibujarían sobre el fondo de la pantalla.
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = CardDefaults.shape
            ),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (compacta) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compacta) 32.dp else 40.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (titulo.isNotBlank()) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorSuave,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!detalle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorNeutro,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Las pastillas se ordenan por severidad (vencidas, próximas, resto)
                    // y se limitan a tres para que no rompan línea en móviles estrechos.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        badges
                            .sortedBy { severidadDeBadge(it.estado) }
                            .take(3)
                            .forEach { badge -> PastillaEstado(badge) }
                    }
                }
            }

            if (contadores.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    contadores.forEachIndexed { indice, contador ->
                        if (indice > 0) Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${contador.valor}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (contador.alerta && contador.valor > 0) colorAviso else colorNeutro
                            )
                            if (contador.pie.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = contador.pie,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorSuave
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Orden de severidad para colocar las pastillas: primero lo que reclama atención. */
private fun severidadDeBadge(estado: EstadoBadge): Int = when (estado) {
    EstadoBadge.AVISO -> 0
    EstadoBadge.PROXIMO -> 1
    EstadoBadge.CORRECTO -> 2
    EstadoBadge.NEUTRO -> 3
}

/**
 * Añade al `LazyColumn` una tarjeta larga por cada item, con el mismo hueco entre todas.
 * Es una extensión de [LazyListScope] (y no un composable) porque se invoca desde dentro
 * del bloque de contenido del `LazyColumn`.
 */
fun LazyListScope.listaTarjetasFicha(
    items: List<ItemFicha>,
    onAbrirHoja: (String) -> Unit,
    compacta: Boolean = false
) {
    items.forEach { item ->
        item(key = "tarjeta-${item.hoja ?: item.titulo}") {
            TarjetaLargaFicha(
                icono = item.icono,
                titulo = item.titulo,
                subtitulo = item.subtitulo,
                detalle = item.detalle,
                badges = item.badges,
                contadores = item.contadores,
                colorFondo = item.colorFondo,
                colorIcono = item.colorIcono,
                compacta = compacta,
                onClick = {
                    val hoja = item.hoja
                    if (hoja != null) onAbrirHoja(hoja)
                }
            )
        }
    }
}

/** Lista vertical de tarjetas largas separadas por el mismo hueco entre todas. */
@Composable
fun ListaTarjetasFicha(
    items: List<ItemFicha>,
    onAbrirHoja: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            TarjetaLargaFicha(
                icono = item.icono,
                titulo = item.titulo,
                subtitulo = item.subtitulo,
                detalle = item.detalle,
                badges = item.badges,
                contadores = item.contadores,
                colorFondo = item.colorFondo,
                colorIcono = item.colorIcono,
                onClick = {
                    val hoja = item.hoja
                    if (hoja != null) onAbrirHoja(hoja)
                }
            )
        }
    }
}
