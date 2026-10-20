package com.example.virtualpetkmp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.virtualpetkmp.NotificacionReceiver

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

        override fun programar(
            id: Long,
            titulo: String,
            mensaje: String,
            fechaDisparoMillis: Long
        ) {
            programarAlarma(context, id, intento(context, id, titulo, mensaje), fechaDisparoMillis)
        }

        override fun programarTratamiento(
            id: Long,
            titulo: String,
            mensaje: String,
            fechaDisparoMillis: Long,
            hora: String,
            fechaFin: String?
        ) {
            // El aviso de un tratamiento lleva dentro la hora y la fecha de fin: cuando salte,
            // la app puede estar cerrada, y con eso el receptor sabe si tiene que programar el
            // de mañana. Sin estos extras, un tratamiento solo avisaría una vez.
            val intent = intento(context, id, titulo, mensaje).apply {
                putExtra(NotificacionReceiver.EXTRA_HORA, hora)
                if (fechaFin != null) putExtra(NotificacionReceiver.EXTRA_FECHA_FIN, fechaFin)
            }
            programarAlarma(context, id, intent, fechaDisparoMillis)
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

/** El intent que recibe [NotificacionReceiver] para mostrar el aviso. */
private fun intento(context: Context, id: Long, titulo: String, mensaje: String): Intent =
    Intent(context, NotificacionReceiver::class.java).apply {
        putExtra(NotificacionReceiver.EXTRA_TITULO, titulo)
        putExtra(NotificacionReceiver.EXTRA_MENSAJE, mensaje)
        putExtra(NotificacionReceiver.EXTRA_NOTIFICACION_ID, id.toInt())
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
