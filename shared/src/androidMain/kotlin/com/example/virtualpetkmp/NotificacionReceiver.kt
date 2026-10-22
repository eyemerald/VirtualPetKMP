package com.example.virtualpetkmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.virtualpetkmp.util.AvisoTratamiento
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
 * antelación y, cuando saltan, ya han cumplido; su texto ya lo lee el usuario y su edición se
 * hace en la app, así que la notificación solo informa. Los de tratamiento, en cambio, son una
 * **medicina que hay que dar**: por eso su aviso se puede **aceptar** o **posponer** (como una
 * alarma) y, al tocarlo, abre la ficha de esa mascota para ver el tratamiento. Además se repiten
 * **cada día a su hora** mientras dure, y como este receptor corre con la app posiblemente
 * cerrada, es aquí donde se encadena el siguiente.
 */
class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Un aviso de tratamiento llega completo (con su mascota y los textos de los botones);
        // el de una dosis llega solo con título y mensaje.
        val aviso = intent.avisoDeTratamiento()
        val titulo = aviso?.titulo ?: intent.getStringExtra(EXTRA_TITULO) ?: "Recordatorio"
        val mensaje = aviso?.mensaje ?: intent.getStringExtra(EXTRA_MENSAJE).orEmpty()
        val notificacionId = aviso?.notificacionId?.toInt()
            ?: intent.getIntExtra(EXTRA_NOTIFICACION_ID, 0)

        val canal = if (aviso != null) CANAL_TRATAMIENTOS else CANAL_RECORDATORIOS
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        asegurarCanal(gestor, canal)

        val builder = NotificationCompat.Builder(context, canal)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        if (aviso != null) {
            // Tocar el aviso abre la ficha de la mascota a la que pertenece el tratamiento.
            builder.setContentIntent(pendingIntentAbrirFicha(context, aviso))
            if (aviso.textoAceptar.isNotBlank()) {
                builder.addAction(0, aviso.textoAceptar, pendingIntentAccion(context, aviso, ACCION_ACEPTAR))
            }
            if (aviso.textoPosponer.isNotBlank()) {
                builder.addAction(0, aviso.textoPosponer, pendingIntentAccion(context, aviso, ACCION_POSPONER))
            }
        }

        gestor.notify(notificacionId, builder.build())

        aviso?.let { programarSiguiente(context, it) }
    }

    /**
     * Programa el aviso de mañana a la misma hora, si el tratamiento sigue en curso.
     *
     * El cálculo es el mismo que al programarlo la primera vez (`RecordatoriosTratamiento`), así
     * que la cadena no puede desviarse de hora ni avisar después de la fecha de fin.
     *
     * Si el usuario pulsó "Posponer", esa alarma sustituye a la de mañana (comparten id) y al
     * sonar vuelve a entrar aquí, que es lo que restaura la de mañana. Es decir: el encadenado se
     * repara solo y por eso posponer no puede "perder" el aviso del día siguiente.
     */
    private fun programarSiguiente(context: Context, aviso: AvisoTratamiento) {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val proximo = RecordatoriosTratamiento.proximoAviso(
            hora = aviso.hora,
            // Ya estamos dentro del tratamiento: no se avisa antes de hoy.
            inicio = ahora.date,
            fin = aviso.fechaFin,
            desde = ahora
        ) ?: return

        crearProgramadorNotificaciones(context).programarTratamiento(
            aviso.copy(
                fechaDisparoMillis = proximo.toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds()
            )
        )
    }

    /**
     * El `PendingIntent` que abre la app en la ficha de la mascota del aviso.
     *
     * Se usa el intent de arranque del paquete en vez de nombrar la activity a mano porque esa
     * clase vive en el módulo de Android y este receptor en el compartido: nombrarla aquí crearía
     * una dependencia que no existe (y el nombre del paquete sí es el mismo en las dos).
     *
     * El `data` con el id del aviso hace que cada tratamiento tenga su propio intent (si no, dos
     * avisos distintos compartirían `PendingIntent` y abrirían la mascota equivocada).
     */
    private fun pendingIntentAbrirFicha(context: Context, aviso: AvisoTratamiento): PendingIntent {
        val abrir = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        abrir.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data = Uri.parse("virtualpet://aviso/${aviso.notificacionId}")
            putExtra(EXTRA_AVISO_MASCOTA_ID, aviso.mascotaId)
            putExtra(EXTRA_AVISO_TRATAMIENTO_ID, aviso.tratamientoId)
        }
        return PendingIntent.getActivity(
            context,
            aviso.notificacionId.toInt(),
            abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * El `PendingIntent` de un botón del aviso.
     *
     * Lleva el aviso completo dentro para que el receptor de la acción pueda posponer sin tocar la
     * base de datos, y la acción en el `action` del intent para distinguir Aceptar de Posponer.
     */
    private fun pendingIntentAccion(
        context: Context,
        aviso: AvisoTratamiento,
        accion: String
    ): PendingIntent {
        val intent = Intent(context, AccionRecordatorioReceiver::class.java)
            .setAction(accion)
            .conAvisoDeTratamiento(aviso)
        return PendingIntent.getBroadcast(
            context,
            aviso.notificacionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        const val EXTRA_TRATAMIENTO_ID = "extra_tratamiento_id"
        const val EXTRA_MASCOTA_ID = "extra_mascota_id"
        const val EXTRA_TEXTO_ACEPTAR = "extra_texto_aceptar"
        const val EXTRA_TEXTO_POSPONER = "extra_texto_posponer"

        /** Extras que recibe la pantalla al abrirse desde un aviso. */
        const val EXTRA_AVISO_MASCOTA_ID = "aviso_mascota_id"
        const val EXTRA_AVISO_TRATAMIENTO_ID = "aviso_tratamiento_id"

        /** Acciones de los dos botones del aviso. */
        const val ACCION_ACEPTAR = "com.example.virtualpetkmp.ACEPTAR_RECORDATORIO"
        const val ACCION_POSPONER = "com.example.virtualpetkmp.POSPONER_RECORDATORIO"

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

/** Mete el aviso completo en el intent de la alarma: el receptor corre sin la app abierta. */
internal fun Intent.conAvisoDeTratamiento(aviso: AvisoTratamiento): Intent = apply {
    putExtra(NotificacionReceiver.EXTRA_TITULO, aviso.titulo)
    putExtra(NotificacionReceiver.EXTRA_MENSAJE, aviso.mensaje)
    putExtra(NotificacionReceiver.EXTRA_NOTIFICACION_ID, aviso.notificacionId.toInt())
    putExtra(NotificacionReceiver.EXTRA_HORA, aviso.hora)
    putExtra(NotificacionReceiver.EXTRA_TRATAMIENTO_ID, aviso.tratamientoId)
    putExtra(NotificacionReceiver.EXTRA_MASCOTA_ID, aviso.mascotaId)
    putExtra(NotificacionReceiver.EXTRA_TEXTO_ACEPTAR, aviso.textoAceptar)
    putExtra(NotificacionReceiver.EXTRA_TEXTO_POSPONER, aviso.textoPosponer)
    aviso.fechaFin?.let { putExtra(NotificacionReceiver.EXTRA_FECHA_FIN, it.toString()) }
}

/** Recupera el aviso de un intent, o `null` si el aviso no era de un tratamiento. */
internal fun Intent.avisoDeTratamiento(): AvisoTratamiento? {
    val tratamientoId = getLongExtra(NotificacionReceiver.EXTRA_TRATAMIENTO_ID, -1L)
    if (tratamientoId <= 0L) return null
    val hora = getStringExtra(NotificacionReceiver.EXTRA_HORA) ?: return null

    return AvisoTratamiento(
        notificacionId = getIntExtra(NotificacionReceiver.EXTRA_NOTIFICACION_ID, 0).toLong(),
        tratamientoId = tratamientoId,
        mascotaId = getLongExtra(NotificacionReceiver.EXTRA_MASCOTA_ID, -1L),
        titulo = getStringExtra(NotificacionReceiver.EXTRA_TITULO).orEmpty(),
        mensaje = getStringExtra(NotificacionReceiver.EXTRA_MENSAJE).orEmpty(),
        fechaDisparoMillis = 0L,
        hora = hora,
        fechaFin = getStringExtra(NotificacionReceiver.EXTRA_FECHA_FIN)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        textoAceptar = getStringExtra(NotificacionReceiver.EXTRA_TEXTO_ACEPTAR).orEmpty(),
        textoPosponer = getStringExtra(NotificacionReceiver.EXTRA_TEXTO_POSPONER).orEmpty()
    )
}
