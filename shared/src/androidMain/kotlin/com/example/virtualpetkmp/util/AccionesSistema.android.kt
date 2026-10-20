package com.example.virtualpetkmp.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberLlamador(): (String) -> Boolean {
    val context = LocalContext.current
    return { telefono ->
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${telefono.filter { it.isDigit() || it == '+' }}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
actual fun rememberAbridorMapa(): (String) -> Boolean {
    val context = LocalContext.current
    return { query ->
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
actual fun rememberProgramadorNotificaciones(): ProgramadorNotificaciones {
    val context = LocalContext.current
    // La implementación vive fuera de la composición (ver AlarmasAndroid.kt) porque también la
    // usa el receptor de arranque del móvil, que corre sin interfaz.
    return remember(context) { crearProgramadorNotificaciones(context) }
}

/**
 * Pide el permiso de notificaciones de Android 13+ si no está concedido.
 *
 * Devuelve `true` cuando ya se puede notificar; si lanza la petición devuelve `false`, porque la
 * respuesta del usuario llega por su cuenta (y no hay nada que hacer con ella: si dice que no,
 * la app sigue funcionando sin avisos, y el formulario se lo dice).
 */
@Composable
actual fun rememberPermisoNotificaciones(): () -> Boolean {
    val context = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* da igual la respuesta aquí: se comprueba al programar */ }

    return remember(context, lanzador) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
                false
            }
        }
    }
}

@Composable
actual fun rememberComprobadorPermisoExacto(): ComprobadorPermisoExacto {
    val context = LocalContext.current
    return remember(context) {
        object : ComprobadorPermisoExacto {
            override fun puedeProgramarExacto(): Boolean {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                return am.canScheduleExactAlarms()
            }

            override fun pedirPermiso() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        // Nada que hacer
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberAgregadorCalendario(): (String, String, Long) -> Boolean {
    val context = LocalContext.current
    return { titulo, descripcion, fechaMillis ->
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, titulo)
                putExtra(android.provider.CalendarContract.Events.DESCRIPTION, descripcion)
                putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaMillis)
                putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, fechaMillis + 24 * 60 * 60 * 1000L)
                // Recordatorio 1 día antes (en minutos: 24 * 60 = 1440)
                putExtra(android.provider.CalendarContract.Reminders.MINUTES, 1440)
                putExtra(android.provider.CalendarContract.Reminders.METHOD, android.provider.CalendarContract.Reminders.METHOD_ALERT)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
