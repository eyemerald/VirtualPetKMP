package com.example.virtualpetkmp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.virtualpetkmp.NotificacionReceiver
import com.example.virtualpetkmp.conAvisoDeDosis
import com.example.virtualpetkmp.conAvisoDeTratamiento

/**
 * Alarmas de Android para los recordatorios locales.
 *
 * Es una **función normal y no un `@Composable`** a propósito: no solo la usa la interfaz. El
 * receptor de arranque del móvil también tiene que programar alarmas, y ahí no hay composición
 * ninguna (la app puede estar cerrada).
 *
 * Las alarmas se piden exactas (`setExactAndAllowWhileIdle`) para que el aviso de una medicina
 * llegue a su hora y no "en algún momento de la mañana"; si el usuario no ha concedido
 * `SCHEDULE_EXACT_ALARM` en Android 12+, se cae a `set()`, que es inexacto pero funciona. Es
 * mejor un aviso con unos minutos de retraso que ningún aviso.
 */
fun crearProgramadorNotificaciones(context: Context): ProgramadorNotificaciones =
    object : ProgramadorNotificaciones {

        override fun programar(aviso: AvisoDosis) {
            // El aviso de una dosis lleva su tipo y su uuid: cuando salte, el receptor comprobará
            // que esa vacuna o ese preventivo siguen existiendo antes de notificar.
            val intent = Intent(context, NotificacionReceiver::class.java)
                .conAvisoDeDosis(aviso)
            programarAlarma(context, aviso.notificacionId, intent, aviso.fechaDisparoMillis)
        }

        override fun programarTratamiento(aviso: AvisoTratamiento) {
            // El aviso viaja completo dentro de la alarma: cuando salte, la app puede estar
            // cerrada, y con esos extras el receptor sabe a qué mascota pertenece, puede abrir su
            // ficha, pintar los botones Aceptar/Posponer y reprogramar el aviso de mañana.
            val intent = Intent(context, NotificacionReceiver::class.java)
                .conAvisoDeTratamiento(aviso)
            programarAlarma(context, aviso.notificacionId, intent, aviso.fechaDisparoMillis)
        }

        override fun cancelar(id: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Para cancelar hay que encontrar el PendingIntent ya creado: los extras no cuentan
            // al compararlos, así que basta con el mismo componente y el mismo id.
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                Intent(context, NotificacionReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

private fun programarAlarma(context: Context, id: Long, intent: Intent, fechaDisparoMillis: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, fechaDisparoMillis, pendingIntent)
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparoMillis, pendingIntent)
    }
}
