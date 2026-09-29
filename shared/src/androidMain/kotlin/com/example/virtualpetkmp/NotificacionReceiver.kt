package com.example.virtualpetkmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Recordatorio"
        val mensaje = intent.getStringExtra(EXTRA_MENSAJE) ?: ""
        val notificacionId = intent.getIntExtra(EXTRA_NOTIFICACION_ID, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CANAL_RECORDATORIOS,
                "Recordatorios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de vacunas y preventivos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CANAL_RECORDATORIOS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificacionId, builder.build())
    }

    companion object {
        const val CANAL_RECORDATORIOS = "recordatorios_preventivos"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_MENSAJE = "extra_mensaje"
        const val EXTRA_NOTIFICACION_ID = "extra_notificacion_id"
    }
}
