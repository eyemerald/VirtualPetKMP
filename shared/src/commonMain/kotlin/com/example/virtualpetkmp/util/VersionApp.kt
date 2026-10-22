package com.example.virtualpetkmp.util

/**
 * Versión de la app que está corriendo ("1.5"), para dejarla escrita dentro del paquete de copia.
 *
 * El módulo compartido no puede leer el `versionName` del `build.gradle.kts` de Android, así que
 * cada plataforma lo pregunta donde sí lo tiene. Es un dato **informativo**: sirve para saber con
 * qué versión se hizo una copia, no para decidir si se puede restaurar (de eso se encarga
 * `versionFormato`).
 */
expect fun versionDeLaApp(): String

/**
 * "android" o "escritorio". Se guarda en el paquete para saber de dónde salió una copia.
 *
 * Es informativo: restaurar una copia hecha en el ordenador en un móvil es exactamente lo que se
 * quiere poder hacer, así que esto **no** se usa para aceptar o rechazar nada.
 */
expect fun plataformaDeLaApp(): String
