package com.example.virtualpetkmp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.virtualpetkmp.util.AvisoTratamiento
import com.example.virtualpetkmp.util.RecordatoriosTratamiento
import com.example.virtualpetkmp.util.crearProgramadorNotificaciones
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Los dos botones del aviso de un tratamiento: **Aceptar** y **Posponer**.
 *
 * Va aparte de [NotificacionReceiver] porque no hace lo mismo: aquel pinta el aviso y encadena el
 * de mañana, y este solo responde a lo que pulsa el usuario. Se separan para que un fallo al
 * posponer no pueda impedir que se programe el aviso del día siguiente.
 *
 * **Aceptar** cierra el aviso y nada más: la dosis se ha dado, no hay nada que recordar. El de
 * mañana ya está programado desde que sonó este.
 *
 * **Posponer** cierra el aviso y lo vuelve a programar para dentro de
 * [RecordatoriosTratamiento.POSPOSICION_MINUTOS]. No hay límite de veces: si el usuario no acepta,
 * el aviso vuelve una vez tras otra hasta que lo acepte o hasta que el tratamiento se acabe.
 */
class AccionRecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val aviso = intent.avisoDeTratamiento() ?: return

        // En los dos casos el aviso deja de estar en la barra: el usuario ya ha decidido.
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestor.cancel(aviso.notificacionId.toInt())

        if (intent.action == NotificacionReceiver.ACCION_POSPONER) {
            posponer(context, aviso)
        }
    }

    /**
     * Vuelve a programar el aviso dentro de un rato, salvo que el tratamiento ya se haya acabado.
     *
     * La comprobación con la fecha de fin es lo que evita el aviso fantasma: si el último día del
     * tratamiento era hoy y el usuario pospone a las 23:50, el aviso saltaría mañana, cuando ya no
     * hay nada que dar.
     */
    private fun posponer(context: Context, aviso: AvisoTratamiento) {
        val ahoraMillis = Clock.System.now().toEpochMilliseconds()
        val nuevo = aviso.pospuesto(desdeMillis = ahoraMillis)

        val destino = Instant.fromEpochMilliseconds(nuevo.fechaDisparoMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        if (!RecordatoriosTratamiento.dentroDelTratamiento(destino.date, aviso.fechaFin)) return

        crearProgramadorNotificaciones(context).programarTratamiento(nuevo)
    }
}
