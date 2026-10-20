package com.example.virtualpetkmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.virtualpetkmp.util.RecordatoriosTratamiento
import com.example.virtualpetkmp.util.crearProgramadorNotificaciones
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Muestra un recordatorio y, si es de un tratamiento, programa el del día siguiente.
 *
 * Los avisos de vacunas y preventivos son de **una sola vez**: se programan con días de
 * antelación y al saltar ya han cumplido. Los de tratamiento, en cambio, se repiten **cada día a
 * su hora** mientras dure, y como este receptor corre con la app posiblemente cerrada, es aquí
 * donde se encadena el siguiente: por eso el aviso lleva la hora y la fecha de fin dentro.
 */
class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Recordatorio"
        val mensaje = intent.getStringExtra(EXTRA_MENSAJE) ?: ""
        val notificacionId = intent.getIntExtra(EXTRA_NOTIFICACION_ID, 0)
        // La hora solo viene en los avisos de tratamiento: es lo que distingue unos de otros.
        val hora = intent.getStringExtra(EXTRA_HORA)

        val canal = if (hora != null) CANAL_TRATAMIENTOS else CANAL_RECORDATORIOS
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        asegurarCanal(gestor, canal)

        val builder = NotificationCompat.Builder(context, canal)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        gestor.notify(notificacionId, builder.build())

        if (hora != null) {
            programarSiguiente(context, intent, hora, notificacionId)
        }
    }

    /**
     * Programa el aviso de mañana a la misma hora, si el tratamiento sigue en curso.
     *
     * El cálculo es el mismo que al programarlo la primera vez (`RecordatoriosTratamiento`), así
     * que la cadena no puede desviarse de hora ni avisar después de la fecha de fin.
     */
    private fun programarSiguiente(context: Context, intent: Intent, hora: String, id: Int) {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val fin = intent.getStringExtra(EXTRA_FECHA_FIN)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = hora,
            // Ya estamos dentro del tratamiento: no se avisa antes de hoy.
            inicio = ahora.date,
            fin = fin,
            desde = ahora
        ) ?: return

        crearProgramadorNotificaciones(context).programarTratamiento(
            id = id.toLong(),
            titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty(),
            mensaje = intent.getStringExtra(EXTRA_MENSAJE).orEmpty(),
            fechaDisparoMillis = proximo.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            hora = hora,
            fechaFin = intent.getStringExtra(EXTRA_FECHA_FIN)
        )
    }

    companion object {
        /** Canal de vacunas y preventivos. El id no se cambia: el usuario ya lo tiene configurado. */
        const val CANAL_RECORDATORIOS = "recordatorios_preventivos"

        /** Canal propio de los tratamientos: así se pueden silenciar sin silenciar las vacunas. */
        const val CANAL_TRATAMIENTOS = "recordatorios_tratamientos"

        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_MENSAJE = "extra_mensaje"
        const val EXTRA_NOTIFICACION_ID = "extra_notificacion_id"
        const val EXTRA_HORA = "extra_hora"
        const val EXTRA_FECHA_FIN = "extra_fecha_fin"

        /**
         * Crea el canal si no existe.
         *
         * Android 8+ no muestra nada de un canal que no esté creado, y crear uno que ya existe no
         * hace nada (no pisa la configuración del usuario), así que se puede llamar siempre.
         */
        fun asegurarCanal(gestor: NotificationManager, canalId: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            if (gestor.getNotificationChannel(canalId) != null) return

            val canal = NotificationChannel(
                canalId,
                when (canalId) {
                    CANAL_TRATAMIENTOS -> "Recordatorios de tratamientos"
                    else -> "Recordatorios de vacunas y preventivos"
                },
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = when (canalId) {
                    CANAL_TRATAMIENTOS -> "Avisos diarios para dar la medicación a tiempo"
                    else -> "Avisos de las próximas dosis de vacunas y preventivos"
                }
            }
            gestor.createNotificationChannel(canal)
        }
    }
}
