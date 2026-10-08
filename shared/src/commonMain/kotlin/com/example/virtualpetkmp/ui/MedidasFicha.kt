package com.example.virtualpetkmp.ui

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
