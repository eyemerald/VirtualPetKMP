package com.example.virtualpetkmp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.AvisoDosis
import com.example.virtualpetkmp.util.AvisoTratamiento
import com.example.virtualpetkmp.util.DecisionAviso
import com.example.virtualpetkmp.util.EntidadDeAviso
import com.example.virtualpetkmp.util.Localizacion
import com.example.virtualpetkmp.util.MotivoZombi
import com.example.virtualpetkmp.util.Posposiciones
import com.example.virtualpetkmp.util.ProgramadorNotificaciones
import com.example.virtualpetkmp.util.RegistroAvisosEnFichero
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Muestra un recordatorio cuando su alarma salta.
 *
 * **Antes de notificar, el aviso se verifica contra la base de datos.** Hasta la 1.14 este receptor
 * se fiaba de lo que llevaba dentro la alarma (el título, el mensaje y las fechas se guardaban ya
 * formateados) y volvía a programar el aviso del día siguiente sin comprobar nada. Eso hacía que una
 * alarma de una mascota borrada —o de un tratamiento que una restauración había sustituido— sonara
 * **todos los días para siempre**: se reprodujo en el emulador con cuatro alarmas de una mascota de
 * prueba que ya no existía.
 *
 * Ahora, al sonar:
 *
 * 1. Se busca la entidad por su **`uuid`** (el id local cambia al restaurar una copia).
 * 2. Si no existe, si la hora de esa toma ya no es una de las horas del tratamiento o si el
 *    tratamiento ya no está vigente, **no se notifica**: la alarma se cancela a sí misma, se quita
 *    del registro y no se reprograma. Es lo que limpia además las alarmas zombi que dejaron las
 *    versiones anteriores, que ni siquiera llevan `uuid`.
 * 3. Si sigue valiendo, el texto se construye **desde la base de datos actual** (no desde los
 *    extras, que pueden ser de una versión anterior o de otro idioma) y se deja el conjunto de
 *    avisos sincronizado, que es lo que programa el de mañana.
 *
 * Los avisos de vacunas y preventivos son de **una sola vez**: cuando saltan ya han cumplido. Los
 * de tratamiento se repiten **cada día a su hora** mientras dure, y su aviso se puede **aceptar** o
 * **posponer** (ver [AccionRecordatorioReceiver]).
 */
class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Un receptor tiene unos segundos de vida: `goAsync()` mantiene el proceso despierto
        // mientras se consulta la base de datos, y hay que cerrarlo siempre.
        val pendiente = goAsync()
        val aplicacion = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                atender(aplicacion, intent)
            } catch (e: Exception) {
                android.util.Log.e(ETIQUETA, "no se pudo atender el aviso", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    /** Todo lo que hace el receptor, ya con corrutina y con la base de datos abierta. */
    private suspend fun atender(context: Context, intent: Intent) {
        val notificacionId = intent.getIntExtra(EXTRA_NOTIFICACION_ID, 0).toLong()
        val tipo = TipoAviso.desdeNombre(intent.getStringExtra(EXTRA_TIPO))
        val uuid = intent.getStringExtra(EXTRA_UUID)
        val hora = intent.getStringExtra(EXTRA_HORA)
        val diaDelAviso = intent.getLongExtra(EXTRA_DISPARO_MILLIS, 0L)

        val programador = crearProgramadorNotificaciones(context)
        val localizacion = localizacionFueraDeComposicion()
        val db = abrirBaseDeDatos(context)

        val ahora = Clock.System.now()
        val hoy = ahora.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 1. ¿Sigue teniendo sentido este aviso? La respuesta se calcula con los datos de AHORA.
        val entidad = if (tipo != null && !uuid.isNullOrBlank()) {
            entidadDeAviso(db, tipo, uuid, diaDelAviso, hoy)
        } else {
            null
        }

        when (val decision = decidirAviso(tipo, uuid, hora, entidad?.estado)) {
            is DecisionAviso.Zombi -> limpiarZombi(
                context = context,
                db = db,
                notificacionId = notificacionId,
                motivo = decision.motivo,
                programador = programador,
                localizacion = localizacion
            )

            DecisionAviso.Sonar -> {
                android.util.Log.i(
                    ETIQUETA,
                    "aviso vigente ($tipo $uuid a las $hora): se notifica el $notificacionId"
                )
                mostrar(
                    context = context,
                    entidad = entidad,
                    horaDeLaToma = hora,
                    momentoDelAviso = ahora.toEpochMilliseconds(),
                    notificacionId = notificacionId,
                    localizacion = localizacion
                )
                // El aviso ha vuelto a salir: la posposición ya está consumida. Se limpia ANTES de
                // sincronizar, porque la sincronización reprograma esa misma alarma y no debe
                // encontrarse viva la posposición.
                Posposiciones.borrar(notificacionId)
                // Deja el aviso de mañana programado (si toca) y el registro al día.
                sincronizarAvisos(programador, db, localizacion, ahora = ahora.toLocalDateTime(
                    TimeZone.currentSystemDefault()
                ))
            }
        }
    }

    /**
     * Cancela una alarma que ya no debería existir y deja el registro correcto.
     *
     * No se notifica nada y **no se reprograma**: si se reprogramara, el aviso zombi volvería
     * mañana y el problema no se arreglaría nunca. Después se llama a `sincronizarAvisos()`, que
     * vuelve a programar lo que sí hay que avisar con el formato nuevo (con `uuid`), así que un
     * móvil que venía de la versión anterior queda arreglado solo.
     */
    private suspend fun limpiarZombi(
        context: Context,
        db: VirtualPetDatabase,
        notificacionId: Long,
        motivo: MotivoZombi,
        programador: ProgramadorNotificaciones,
        localizacion: Localizacion
    ) {
        android.util.Log.i(ETIQUETA, "aviso zombi ($motivo): se cancela el $notificacionId")
        programador.cancelar(notificacionId)
        quitarNotificacion(context, notificacionId)
        Posposiciones.borrar(notificacionId)
        RegistroAvisosEnFichero.quitar(notificacionId)

        try {
            sincronizarAvisos(
                programador = programador,
                db = db,
                localizacion = localizacion,
                registro = RegistroAvisosEnFichero
            )
        } catch (e: Exception) {
            android.util.Log.w(ETIQUETA, "no se pudo sincronizar tras limpiar el aviso", e)
        }
    }

    /** Pinta la notificación con el texto leído de los datos actuales. */
    private suspend fun mostrar(
        context: Context,
        entidad: EntidadDeAviso?,
        horaDeLaToma: String?,
        momentoDelAviso: Long,
        notificacionId: Long,
        localizacion: Localizacion
    ) {
        if (entidad == null) return

        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Nombre y descripción de los canales salen de los recursos de idioma. Se reescriben aquí
        // para que un cambio de idioma del sistema se vea aunque la app lleve días cerrada.
        crearCanalesDeNotificacion(context)

        val avisoTratamiento = avisoDelTratamiento(entidad, horaDeLaToma, momentoDelAviso)
        val (titulo, mensaje) = when (entidad) {
            is EntidadDeAviso.DeTratamiento ->
                // Sin aviso no hay nada que enseñar: el tratamiento ya no tiene esa toma.
                if (avisoTratamiento == null) return else
                    avisoTratamiento.titulo to avisoTratamiento.mensaje
            is EntidadDeAviso.DeVacuna ->
                ReprogramadorNotificaciones.textoVacuna(entidad.vacuna, localizacion)
            is EntidadDeAviso.DePreventivo ->
                ReprogramadorNotificaciones.textoPreventivo(entidad.preventivo, localizacion)
        }

        val canal = if (avisoTratamiento != null) CANAL_TRATAMIENTOS else CANAL_RECORDATORIOS
        val builder = NotificationCompat.Builder(context, canal)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        if (avisoTratamiento != null) {
            // Tocar el aviso abre la ficha de la mascota a la que pertenece el tratamiento.
            builder.setContentIntent(pendingIntentAbrirFicha(context, avisoTratamiento))
            if (avisoTratamiento.textoAceptar.isNotBlank()) {
                builder.addAction(
                    0,
                    avisoTratamiento.textoAceptar,
                    pendingIntentAccion(context, avisoTratamiento, ACCION_ACEPTAR)
                )
            }
            if (avisoTratamiento.textoPosponer.isNotBlank()) {
                builder.addAction(
                    0,
                    avisoTratamiento.textoPosponer,
                    pendingIntentAccion(context, avisoTratamiento, ACCION_POSPONER)
                )
            }
        }

        gestor.notify(notificacionId.toInt(), builder.build())
    }

    /**
     * El aviso de tratamiento, reconstruido con los datos actuales (texto incluido) y posicionado
     * en la toma que está sonando.
     */
    private suspend fun avisoDelTratamiento(
        entidad: EntidadDeAviso?,
        horaDeLaToma: String?,
        momentoDelAviso: Long
    ): AvisoTratamiento? {
        if (entidad !is EntidadDeAviso.DeTratamiento) return null
        // La toma que suena es la que la alarma lleva escrita; si esa hora ya no está en el
        // tratamiento, el aviso se habría descartado antes de llegar aquí.
        val toma = entidad.estado.horasDeAviso.indexOf(horaDeLaToma ?: return null)
        if (toma < 0) return null
        return ReprogramadorNotificaciones.avisoDeToma(
            tratamiento = entidad.tratamiento,
            nombreMascota = entidad.nombreMascota,
            toma = toma,
            // La hora real del aviso viaja en los botones: el de Posponer calcula desde el momento
            // en que se pulsa, pero el intent tiene que llevar la identidad completa del aviso.
            fechaDisparoMillis = momentoDelAviso
        )
    }

    private fun quitarNotificacion(context: Context, notificacionId: Long) {
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestor.cancel(notificacionId.toInt())
    }

    /** La base de datos de la app, abierta desde un receptor. */
    private fun abrirBaseDeDatos(context: Context): VirtualPetDatabase =
        DatabaseFactory(context).createDatabase()

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

        /**
         * Identidad de la entidad del aviso.
         *
         * [EXTRA_UUID] es el identificador universal de la fila (tratamiento, vacuna o preventivo) y
         * [EXTRA_TIPO] dice en qué tabla buscarla. Sin los dos, el aviso no se puede verificar y se
         * trata como zombi — que es justo lo que pasa con las alarmas de la versión anterior.
         */
        const val EXTRA_TIPO = "extra_tipo"
        const val EXTRA_UUID = "extra_uuid"
        const val EXTRA_DISPARO_MILLIS = "extra_disparo_millis"

        /** Extras que recibe la pantalla al abrirse desde un aviso. */
        const val EXTRA_AVISO_MASCOTA_ID = "aviso_mascota_id"
        const val EXTRA_AVISO_TRATAMIENTO_ID = "aviso_tratamiento_id"

        /**
         * Acciones de los dos botones del aviso.
         *
         * Llevan un prefijo propio y no el del paquete: son intents **explicitos** (van a nuestro
         * AccionRecordatorioReceiver), así que no necesitan ser únicas en el sistema, y así no
         * queda el identificador antiguo de la app escrito en el código.
         */
        const val ACCION_ACEPTAR = "virtualpet.ACEPTAR_RECORDATORIO"
        const val ACCION_POSPONER = "virtualpet.POSPONER_RECORDATORIO"

        /**
         * Etiqueta de `logcat` para lo que hace este receptor sin la app abierta.
         *
         * Se escribe con `android.util.Log` y no con `println` porque este código corre en un
         * proceso que Android puede arrancar solo para atender la alarma: aquí sí hay logcat, y
         * los mensajes aparecen con `adb logcat -s VirtualPet`.
         */
        const val ETIQUETA = "VirtualPet"
    }
}

/** Mete el aviso completo en el intent de la alarma: el receptor corre sin la app abierta. */
internal fun Intent.conAvisoDeTratamiento(aviso: AvisoTratamiento): Intent = apply {
    putExtra(NotificacionReceiver.EXTRA_TIPO, TipoAviso.TRATAMIENTO.name)
    putExtra(NotificacionReceiver.EXTRA_UUID, aviso.tratamientoUuid)
    putExtra(NotificacionReceiver.EXTRA_DISPARO_MILLIS, aviso.fechaDisparoMillis)
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

/** Mete el aviso de una dosis en el intent de la alarma, con su tipo y su `uuid`. */
internal fun Intent.conAvisoDeDosis(aviso: AvisoDosis): Intent = apply {
    putExtra(NotificacionReceiver.EXTRA_TIPO, aviso.tipo.name)
    putExtra(NotificacionReceiver.EXTRA_UUID, aviso.uuid)
    putExtra(NotificacionReceiver.EXTRA_DISPARO_MILLIS, aviso.fechaDisparoMillis)
    putExtra(NotificacionReceiver.EXTRA_TITULO, aviso.titulo)
    putExtra(NotificacionReceiver.EXTRA_MENSAJE, aviso.mensaje)
    putExtra(NotificacionReceiver.EXTRA_NOTIFICACION_ID, aviso.notificacionId.toInt())
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
        fechaDisparoMillis = getLongExtra(NotificacionReceiver.EXTRA_DISPARO_MILLIS, 0L),
        hora = hora,
        fechaFin = getStringExtra(NotificacionReceiver.EXTRA_FECHA_FIN)
            ?.let { runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull() },
        textoAceptar = getStringExtra(NotificacionReceiver.EXTRA_TEXTO_ACEPTAR).orEmpty(),
        textoPosponer = getStringExtra(NotificacionReceiver.EXTRA_TEXTO_POSPONER).orEmpty(),
        tratamientoUuid = getStringExtra(NotificacionReceiver.EXTRA_UUID).orEmpty()
    )
}
