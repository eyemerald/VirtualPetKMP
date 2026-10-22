package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable

/**
 * Las tres carpetas privadas de la app que importan para la copia de seguridad.
 *
 * Se pasan como datos y no se consultan desde dentro de la exportación por dos motivos: así la
 * lógica de copia se puede probar en un directorio temporal (que es lo que hacen los tests) y el
 * código compartido no necesita saber nada de `Context` ni de `user.home`.
 */
data class CarpetasApp(
    /** Fotos de las mascotas. En Android es la que expone el `FileProvider`. */
    val fotos: String,
    /** Informes veterinarios adjuntos (los PDFs). */
    val informes: String,
    /** Copias de seguridad automáticas que la app guarda por su cuenta. */
    val copias: String
)

/**
 * Rutas de esas carpetas en el dispositivo, creadas si no existen.
 *
 * Es `@Composable` porque en Android necesita el contexto de la aplicación, y esa es la forma en
 * la que este proyecto ya resuelve las cosas que dependen de la plataforma.
 */
@Composable
expect fun rememberCarpetasApp(): CarpetasApp
