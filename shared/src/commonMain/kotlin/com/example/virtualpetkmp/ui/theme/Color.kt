package com.example.virtualpetkmp.ui.theme

import androidx.compose.ui.graphics.Color

// Modo claro
val VerdePrimario = Color(0xFF3F6B4A)
val OnVerdePrimario = Color(0xFFFFFFFF)
val VerdePrimarioContenedor = Color(0xFFDCE8DC)
val OnVerdePrimarioContenedor = Color(0xFF002107)

val MarronSecundario = Color(0xFF745B41)
val OnMarronSecundario = Color(0xFFFFFFFF)
val MarronSecundarioContenedor = Color(0xFFECE0D0)
val OnMarronSecundarioContenedor = Color(0xFF291800)

val TealTerciario = Color(0xFF3A6659)
val OnTealTerciario = Color(0xFFFFFFFF)
val TealTerciarioContenedor = Color(0xFFD6E5DF)
val OnTealTerciarioContenedor = Color(0xFF002019)

val ErrorClaro = Color(0xFFBA1A1A)
val OnErrorClaro = Color(0xFFFFFFFF)
val ErrorContenedorClaro = Color(0xFFFFDAD6)
val OnErrorContenedorClaro = Color(0xFF410002)

val FondoClaro = Color(0xFFFBFDF7)
val OnFondoClaro = Color(0xFF1A1C19)
val SuperficieClaro = Color(0xFFFBFDF7)
val OnSuperficieClaro = Color(0xFF1A1C19)
val SuperficieVarianteClaro = Color(0xFFE7E9E3)
val OnSuperficieVarianteClaro = Color(0xFF424940)
val ContornoClaro = Color(0xFF72796F)

// Modo oscuro
val VerdePrimarioOscuro = Color(0xFFA5D3AA)
val OnVerdePrimarioOscuro = Color(0xFF10381D)
val VerdePrimarioContenedorOscuro = Color(0xFF2E4A34)
val OnVerdePrimarioContenedorOscuro = Color(0xFFDCE8DC)

val MarronSecundarioOscuro = Color(0xFFE3C29B)
val OnMarronSecundarioOscuro = Color(0xFF402D17)
val MarronSecundarioContenedorOscuro = Color(0xFF4A3D2C)
val OnMarronSecundarioContenedorOscuro = Color(0xFFECE0D0)

val TealTerciarioOscuro = Color(0xFFA1D0C1)
val OnTealTerciarioOscuro = Color(0xFF05372C)
val TealTerciarioContenedorOscuro = Color(0xFF2C4640)
val OnTealTerciarioContenedorOscuro = Color(0xFFD6E5DF)

val ErrorOscuro = Color(0xFFFFB4AB)
val OnErrorOscuro = Color(0xFF690005)
val ErrorContenedorOscuro = Color(0xFF93000A)
val OnErrorContenedorOscuro = Color(0xFFFFDAD6)

val FondoOscuro = Color(0xFF1A1C19)
val OnFondoOscuro = Color(0xFFE2E3DD)
val SuperficieOscuro = Color(0xFF1A1C19)
val OnSuperficieOscuro = Color(0xFFE2E3DD)
val SuperficieVarianteOscuro = Color(0xFF424940)
val OnSuperficieVarianteOscuro = Color(0xFFC2C9BC)
val ContornoOscuro = Color(0xFF8C9388)

// Colores custom para el estado "próxima a vencer" (naranja)
val ProximaContenedor = Color(0xFFFFB74D)         // naranja más visible
val OnProximaContenedor = Color(0xFF2E1A00)       // texto más oscuro

val ProximaContenedorOscuro = Color(0xFF7A5A1A)   // más visible
val OnProximaContenedorOscuro = Color(0xFFFFE0B2) // texto naranja claro

// Colores suaves para dar identidad a cada tarjeta de la ficha. Son tonos apagados,
// no vivos, para que la pantalla no parezca un semáforo.
val TarjetaVerde = Color(0xFFD6E6D8)          // recordatorio del verde del logo
val IconoTarjetaVerde = Color(0xFF3F6B4A)
val TarjetaNaranja = Color(0xFFF6DFC6)        // cálido, para salud
val IconoTarjetaNaranja = Color(0xFF8F5A22)
val TarjetaAzul = Color(0xFFD8E2F3)           // frío, para notas
val IconoTarjetaAzul = Color(0xFF35528A)

val TarjetaVerdeOscuro = Color(0xFF2E4034)
val IconoTarjetaVerdeOscuro = Color(0xFFA5D3AA)
val TarjetaNaranjaOscuro = Color(0xFF44362A)
val IconoTarjetaNaranjaOscuro = Color(0xFFE3C29B)
val TarjetaAzulOscuro = Color(0xFF2C3648)
val IconoTarjetaAzulOscuro = Color(0xFFAEC2E8)

// Azul de la última barra del gráfico de peso: destaca el dato más reciente sin
// recurrir al verde ni al rojo, que en esta pantalla ya significan estado.
val AzulBarraActual = Color(0xFF3F5C93)
val AzulBarraActualOscuro = Color(0xFFAEC2E8)

// Gris azulado de la línea de PESO IDEAL. Es una referencia, no un dato medido: por eso va
// discontinua y en un tono neutro, para que no compita con la línea real del peso.
val LineaIdeal = Color(0xFF5A6478)
val LineaIdealOscuro = Color(0xFFB6BECD)
