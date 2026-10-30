package com.example.virtualpetkmp

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.canal_recordatorios_descripcion
import virtualpetkmp.shared.generated.resources.canal_recordatorios_nombre
import virtualpetkmp.shared.generated.resources.canal_tratamientos_descripcion
import virtualpetkmp.shared.generated.resources.canal_tratamientos_nombre

/**
 * Crea (o **actualiza**) los dos canales de notificación de la app, con su nombre y su descripción
 * leídos de los recursos de idioma.
 *
 * **Por qué se llama siempre, y no solo si no existen.** El nombre y la descripción de un canal los
 * pinta Android en Ajustes, y son texto de cara al usuario: antes estaban escritos en español en el
 * código. Volver a llamar a `createNotificationChannel` con el mismo id **actualiza** nombre y
 * descripción (lo que no se puede cambiar así es la importancia, y eso es a propósito: la configuración
 * del usuario manda). Se llama:
 * 1. al arrancar la app (`MainActivity`), para que un cambio de idioma del sistema se refleje sin
 *    esperar a que salte un aviso;
 * 2. antes de publicar cada notificación (`NotificacionReceiver`), que es el caso de la app cerrada.
 *
 * **Por qué `runBlocking`.** `getString` de los recursos de Compose es `suspend`, y esto se llama
 * desde un `BroadcastReceiver` y desde `onCreate`, que no lo son. Lo que se bloquea es la lectura de
 * una cadena de recursos ya cargada: microsegundos.
 */
fun crearCanalesDeNotificacion(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    crearCanal(
        gestor = gestor,
        id = NotificacionReceiver.CANAL_TRATAMIENTOS,
        nombre = runBlocking { getString(Res.string.canal_tratamientos_nombre) },
        descripcion = runBlocking { getString(Res.string.canal_tratamientos_descripcion) }
    )
    crearCanal(
        gestor = gestor,
        id = NotificacionReceiver.CANAL_RECORDATORIOS,
        nombre = runBlocking { getString(Res.string.canal_recordatorios_nombre) },
        descripcion = runBlocking { getString(Res.string.canal_recordatorios_descripcion) }
    )
}

private fun crearCanal(
    gestor: NotificationManager,
    id: String,
    nombre: String,
    descripcion: String
) {
    val canal = android.app.NotificationChannel(
        id,
        nombre,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        this.description = descripcion
    }
    gestor.createNotificationChannel(canal)
}
