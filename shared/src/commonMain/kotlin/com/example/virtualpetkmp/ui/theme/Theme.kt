package com.example.virtualpetkmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = OnVerdePrimario,
    primaryContainer = VerdePrimarioContenedor,
    onPrimaryContainer = OnVerdePrimarioContenedor,
    secondary = MarronSecundario,
    onSecondary = OnMarronSecundario,
    secondaryContainer = MarronSecundarioContenedor,
    onSecondaryContainer = OnMarronSecundarioContenedor,
    tertiary = TealTerciario,
    onTertiary = OnTealTerciario,
    tertiaryContainer = TealTerciarioContenedor,
    onTertiaryContainer = OnTealTerciarioContenedor,
    error = ErrorClaro,
    onError = OnErrorClaro,
    errorContainer = ErrorContenedorClaro,
    onErrorContainer = OnErrorContenedorClaro,
    background = FondoClaro,
    onBackground = OnFondoClaro,
    surface = SuperficieClaro,
    onSurface = OnSuperficieClaro,
    surfaceVariant = SuperficieVarianteClaro,
    onSurfaceVariant = OnSuperficieVarianteClaro,
    outline = ContornoClaro
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdePrimarioOscuro,
    onPrimary = OnVerdePrimarioOscuro,
    primaryContainer = VerdePrimarioContenedorOscuro,
    onPrimaryContainer = OnVerdePrimarioContenedorOscuro,
    secondary = MarronSecundarioOscuro,
    onSecondary = OnMarronSecundarioOscuro,
    secondaryContainer = MarronSecundarioContenedorOscuro,
    onSecondaryContainer = OnMarronSecundarioContenedorOscuro,
    tertiary = TealTerciarioOscuro,
    onTertiary = OnTealTerciarioOscuro,
    tertiaryContainer = TealTerciarioContenedorOscuro,
    onTertiaryContainer = OnTealTerciarioContenedorOscuro,
    error = ErrorOscuro,
    onError = OnErrorOscuro,
    errorContainer = ErrorContenedorOscuro,
    onErrorContainer = OnErrorContenedorOscuro,
    background = FondoOscuro,
    onBackground = OnFondoOscuro,
    surface = SuperficieOscuro,
    onSurface = OnSuperficieOscuro,
    surfaceVariant = SuperficieVarianteOscuro,
    onSurfaceVariant = OnSuperficieVarianteOscuro,
    outline = ContornoOscuro
)

@Immutable
data class ExtrasColors(
    val proximaContenedor: Color,
    val onProximaContenedor: Color
)

val LocalExtrasColors = staticCompositionLocalOf {
    ExtrasColors(
        proximaContenedor = ProximaContenedor,
        onProximaContenedor = OnProximaContenedor
    )
}

@Composable
fun VirtualPetTheme(content: @Composable () -> Unit) {
    val esOscuro = isSystemInDarkTheme()
    val esquemaColor = if (esOscuro) EsquemaOscuro else EsquemaClaro

    val extrasColors = if (esOscuro) {
        ExtrasColors(
            proximaContenedor = ProximaContenedorOscuro,
            onProximaContenedor = OnProximaContenedorOscuro
        )
    } else {
        ExtrasColors(
            proximaContenedor = ProximaContenedor,
            onProximaContenedor = OnProximaContenedor
        )
    }

    CompositionLocalProvider(LocalExtrasColors provides extrasColors) {
        MaterialTheme(
            colorScheme = esquemaColor,
            content = content
        )
    }
}
