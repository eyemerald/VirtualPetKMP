package com.example.virtualpetkmp.util

/**
 * Lo que el aviso de una dosis (vacuna o preventivo) lleva dentro de la alarma.
 *
 * Antes el aviso de una dosis viajaba como título y mensaje sueltos: cuando sonaba, el receptor no
 * tenía forma de saber **qué fila** lo había pedido, así que aunque la vacuna se hubiera borrado (o
 * la mascota entera) el aviso salía igual. Con el `uuid` el receptor puede comprobarlo y, si la
 * entidad ya no existe, cancelarse sin notificar.
 *
 * El texto se sigue llevando dentro porque el receptor corre con la app cerrada, pero **ya no es la
 * única fuente**: al sonar se vuelve a construir desde la base de datos (ver `NotificacionReceiver`).
 */
data class AvisoDosis(
    val notificacionId: Long,
    val tipo: TipoAviso,
    /** Identificador universal de la fila en `vacunas` o `preventivos`. */
    val uuid: String,
    val titulo: String,
    val mensaje: String,
    val fechaDisparoMillis: Long
)
