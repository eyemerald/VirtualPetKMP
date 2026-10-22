package com.example.virtualpetkmp

/**
 * Petición de navegación que llega desde fuera de la app: hoy, tocar el aviso de un tratamiento.
 *
 * Es una **clase normal y no un `data class`** a propósito: quien la recibe la usa como clave de
 * `LaunchedEffect`, y eso compara con `equals`. Con un `data class`, tocar dos veces la misma
 * notificación crearía dos objetos iguales y el efecto no se volvería a ejecutar (la pantalla se
 * quedaría donde estuviera). Con identidad, cada petición es distinta y siempre navega.
 */
class SolicitudNavegacion(
    val mascotaId: Long,
    /** Abrir directamente la hoja de Salud y seguimiento, que es donde viven los tratamientos. */
    val abrirTratamientos: Boolean = false
)
