package com.example.virtualpetkmp.util

/**
 * Carpeta de datos de la app, sin composición.
 *
 * Hace falta porque los avisos pospuestos se guardan en un fichero y los escribe y lee un
 * `BroadcastReceiver`, donde no hay ni actividad ni `LocalContext`. En Android es `filesDir`; en
 * escritorio, `~/.virtualpet`, junto a la base de datos.
 */
expect fun carpetaDatosApp(): String
