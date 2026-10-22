package com.example.virtualpetkmp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.virtualpetkmp.util.reprogramarRecordatorios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recupera los recordatorios cuando el móvil se reinicia o la app se actualiza.
 *
 * Android **borra todas las alarmas programadas al apagar el dispositivo**, así que sin esto un
 * tratamiento con recordatorio dejaría de avisar en cuanto el teléfono se reiniciara, y el usuario
 * no tendría forma de saberlo. Al recibir `BOOT_COMPLETED` se leen de la base de datos los
 * tratamientos que piden aviso **y las próximas dosis de vacunas y preventivos**, y se vuelven a
 * programar todos (ver `reprogramarRecordatorios`).
 *
 * También escucha `MY_PACKAGE_REPLACED` (cuando se instala una versión nueva), que es el otro
 * momento en el que Android limpia las alarmas.
 */
class ReinicioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action
        if (accion != Intent.ACTION_BOOT_COMPLETED && accion != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        // Un receptor tiene unos segundos de vida: `goAsync()` mantiene el proceso despierto
        // mientras se lee la base de datos y se programan las alarmas, y hay que cerrarlo siempre.
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reprogramarRecordatorios(context.applicationContext)
            } catch (e: Exception) {
                println("No se pudieron reprogramar los recordatorios al arrancar: ${e.message}")
            } finally {
                pendiente.finish()
            }
        }
    }
}
