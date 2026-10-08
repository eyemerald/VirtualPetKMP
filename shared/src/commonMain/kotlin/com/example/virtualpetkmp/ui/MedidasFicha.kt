package com.example.virtualpetkmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * Hueco FIJO reservado al fondo del área de scroll de cada pantalla para el banner de
 * publicidad que se añadirá en el futuro.
 *
 * Se implementa como un `Spacer` hermano POSTERIOR al contenedor desplazable (no como
 * `contentPadding` ni como un elemento más de la lista): así el hueco queda siempre
 * anclado al fondo de la pantalla, el contenido se desplaza por encima de él y nunca
 * se mueve con el scroll.
 *
 * 50 dp es el mínimo real de un banner adaptable de AdMob en un móvil en vertical
 * (el SDK lo calcula a partir del ancho del anuncio; sirve un mínimo de 32 dp y en
 * pantallas de ~360 dp de ancho da 50 dp). No conviene bajarlo más: si el banner
 * recibido es más alto que este hueco, se recortaría o taparía contenido.
 * Cuando el banner exista, sustituir este hueco por su altura real.
 */
val ALTO_RESERVA_BANNER = 50.dp

/**
 * Aire mínimo entre las pestañas (Mascotas/Veterinarios) y el contenido de cada
 * pantalla, para que las tres empiecen exactamente a la misma altura.
 */
val ESPACIO_SOBRE_CABECERA = 8.dp

/** Aire entre la cabecera de la ficha (foto/nombre/edad) y el contenido que se desplaza. */
val ESPACIO_BAJO_CABECERA = 12.dp

/**
 * Sitio que ocupan los iconos flotantes por encima del contenido: su alto (44 dp) más un
 * poco de aire, para que la cabecera de la ficha empiece por debajo y no queden pisados.
 */
val ESPACIO_ICONOS_FLOTANTES = 52.dp

/**
 * Altura por debajo de la cual se considera pantalla "baja" (un móvil en horizontal ronda
 * los 390 dp de alto). En ese caso la ficha reduce cabecera y gráficos para que todo quepa.
 */
private val ALTURA_COMPACTA = 480.dp

/**
 * `true` cuando la ventana es baja, es decir, el móvil está en horizontal (o la pantalla es
 * muy pequeña). Se usa para encoger la cabecera y la tarjeta de peso, y que la ficha se vea
 * entera en lugar de cortada.
 */
@Composable
fun alturaCompacta(): Boolean {
    val altoPx = LocalWindowInfo.current.containerSize.height
    val altoDp = with(LocalDensity.current) { altoPx.toDp() }
    return altoDp < ALTURA_COMPACTA
}
