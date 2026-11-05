package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate

/**
 * Todo lo que necesita el aviso de un tratamiento: lo que se ve, lo que hace falta para volver a
 * programarlo cuando salte y los textos de sus dos botones.
 *
 * Existe por un motivo concreto: la notificación la pinta un receptor de Android que puede correr
 * con la app cerrada y ya no tiene acceso al modelo ni a los repositorios. Así que el aviso
 * **viaja completo dentro de la alarma**; cuando salta, el receptor solo tiene que leer estos
 * campos, sin consultar la base de datos.
 *
 * De ahí que [textoAceptar] y [textoPosponer] estén ya traducidos: quien programa el aviso (que sí
 * corre en una corrutina) los lee de los recursos en el idioma activo, y el receptor los usa tal
 * cual. Si se leyeran al mostrar el aviso, habría que abrir recursos desde un receptor.
 *
 * @param notificacionId el id de la notificación, que es también el de la alarma y el de los
 *   `PendingIntent` (ver `RecordatoriosTratamiento.idNotificacion`).
 * @param tratamientoId el id real de la fila en `tratamientos`. **No sirve para reconocer la fila**
 *   al sonar: al restaurar una copia se reinserta con otro id. Para eso está [tratamientoUuid].
 * @param mascotaId para poder abrir la ficha de esa mascota al tocar el aviso.
 * @param hora "HH:mm" tal y como se guarda, para reprogramar el de mañana a la misma hora.
 * @param fechaFin último día del tratamiento, o `null` si es crónico.
 * @param tratamientoUuid identificador universal de la fila: es lo que comprueba el receptor cuando
 *   la alarma salta. Una alarma de una versión anterior lo trae vacío y se trata como zombi.
 */
data class AvisoTratamiento(
    val notificacionId: Long,
    val tratamientoId: Long,
    val mascotaId: Long,
    val titulo: String,
    val mensaje: String,
    val fechaDisparoMillis: Long,
    val hora: String,
    /** Indice de la toma dentro del tratamiento (0 = la primera). Ver HorasDeToma. */
    val toma: Int = 0,
    val fechaFin: LocalDate? = null,
    val textoAceptar: String = "",
    val textoPosponer: String = "",
    val tratamientoUuid: String = ""
) {

    /**
     * El mismo aviso, pero para dentro de [minutos] contados **desde el momento de posponer** y no
     * desde la hora a la que sonó: el usuario que pulsa "Posponer" a los diez minutos espera
     * treinta más, no veinte.
     */
    fun pospuesto(
        desdeMillis: Long,
        minutos: Int = RecordatoriosTratamiento.POSPOSICION_MINUTOS
    ): AvisoTratamiento = copy(fechaDisparoMillis = desdeMillis + minutos * 60_000L)
}
