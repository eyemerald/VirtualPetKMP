package com.example.virtualpetkmp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.util.AvisoTratamiento
import com.example.virtualpetkmp.util.DecisionAviso
import com.example.virtualpetkmp.util.EntidadDeAviso
import com.example.virtualpetkmp.util.Posposiciones
import com.example.virtualpetkmp.util.RecordatoriosTratamiento
import com.example.virtualpetkmp.util.RegistroAvisosEnFichero
import com.example.virtualpetkmp.util.TipoAviso
import com.example.virtualpetkmp.util.crearProgramadorNotificaciones
import com.example.virtualpetkmp.util.decidirAviso
import com.example.virtualpetkmp.util.entidadDeAviso
import com.example.virtualpetkmp.util.localizacionFueraDeComposicion
import com.example.virtualpetkmp.util.sincronizarAvisos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Los dos botones del aviso de un tratamiento: **Aceptar** y **Posponer**.
 *
 * Va aparte de [NotificacionReceiver] porque no hace lo mismo: aquel pinta el aviso y deja el de
 * mañana programado, y este solo responde a lo que pulsa el usuario. Se separan para que un fallo al
 * posponer no pueda impedir que se programe el aviso del día siguiente.
 *
 * **También comprueba el aviso**, igual que el otro receptor: cuando el usuario pulsa "Posponer"
 * pueden haber pasado horas y la mascota o el tratamiento pueden ya no existir. Si el aviso es
 * zombi, no se pospone nada: se cancela, se quita del registro y se deja el resto sincronizado.
 *
 * **Aceptar** cierra el aviso y nada más: la dosis se ha dado. El de mañana ya está programado desde
 * que sonó este.
 *
 * **Posponer** cierra el aviso y lo vuelve a programar para dentro de
 * [RecordatoriosTratamiento.POSPOSICION_MINUTOS]. No hay límite de veces: si el usuario no acepta,
 * el aviso vuelve una vez tras otra hasta que lo acepte o hasta que el tratamiento se acabe.
 */
class AccionRecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendiente = goAsync()
        val aplicacion = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                atender(aplicacion, intent)
            } catch (e: Exception) {
                android.util.Log.e(NotificacionReceiver.ETIQUETA, "no se pudo atender la acción", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private suspend fun atender(context: Context, intent: Intent) {
        val aviso = intent.avisoDeTratamiento() ?: return

        // En los dos casos el aviso deja de estar en la barra: el usuario ya ha decidido.
        quitarNotificacion(context, aviso.notificacionId)

        val programador = crearProgramadorNotificaciones(context)
        val localizacion = localizacionFueraDeComposicion()
        val db = DatabaseFactory(context).createDatabase()
        val ahora = Clock.System.now()
        val hoy = ahora.toLocalDateTime(TimeZone.currentSystemDefault()).date

        val entidad = entidadDeAviso(
            db = db,
            tipo = TipoAviso.TRATAMIENTO,
            uuid = aviso.tratamientoUuid,
            diaDelAvisoMillis = aviso.fechaDisparoMillis,
            hoy = hoy
        )

        if (decidirAviso(
                tipo = TipoAviso.TRATAMIENTO,
                uuidDeLaAlarma = aviso.tratamientoUuid,
                horaDeLaAlarma = aviso.hora,
                estado = entidad?.estado
            ) is DecisionAviso.Zombi
        ) {
            // El aviso ya no vale: no se pospone nada y se limpia lo que hubiera.
            programador.cancelar(aviso.notificacionId)
            Posposiciones.borrar(aviso.notificacionId)
            RegistroAvisosEnFichero.quitar(aviso.notificacionId)
            sincronizarAvisos(programador, db, localizacion)
            return
        }

        if (intent.action == NotificacionReceiver.ACCION_POSPONER) {
            posponer(context, aviso, (entidad as? EntidadDeAviso.DeTratamiento)?.tratamiento?.fechaFin)
        } else {
            // Aceptar: no hay nada pendiente de este tratamiento.
            Posposiciones.borrar(aviso.notificacionId)
        }
    }

    /**
     * Vuelve a programar el aviso dentro de un rato, salvo que el tratamiento ya se haya acabado.
     *
     * La comprobación con la fecha de fin es lo que evita el aviso fantasma: si el último día del
     * tratamiento era hoy y el usuario pospone a las 23:50, el aviso saltaría mañana, cuando ya no
     * hay nada que dar. La fecha de fin sale de la base de datos, no de los extras de la alarma.
     *
     * **La posposición se guarda en disco** ([Posposiciones]) además de en la alarma: Android borra
     * las alarmas al reiniciar, y con el aviso solo dentro de `AlarmManager` el recordatorio
     * pospuesto se perdía hasta el día siguiente.
     */
    private fun posponer(context: Context, aviso: AvisoTratamiento, fechaFin: kotlinx.datetime.LocalDate?) {
        val ahoraMillis = Clock.System.now().toEpochMilliseconds()
        val nuevo = aviso.pospuesto(desdeMillis = ahoraMillis)

        val destino = Instant.fromEpochMilliseconds(nuevo.fechaDisparoMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        if (!RecordatoriosTratamiento.dentroDelTratamiento(destino.date, fechaFin)) {
            // Se acabó el tratamiento: se limpia lo que hubiera y no se programa nada.
            Posposiciones.borrar(aviso.notificacionId)
            return
        }

        Posposiciones.guardar(aviso.notificacionId, nuevo.fechaDisparoMillis)
        crearProgramadorNotificaciones(context).programarTratamiento(nuevo)
        // La alarma sigue siendo del mismo tratamiento y de la misma toma: el registro no cambia,
        // solo su hora, y esa hora vive en la posposición.
    }

    private fun quitarNotificacion(context: Context, notificacionId: Long) {
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestor.cancel(notificacionId.toInt())
    }
}
