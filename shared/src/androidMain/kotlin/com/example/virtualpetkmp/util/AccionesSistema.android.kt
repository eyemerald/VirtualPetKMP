package com.example.virtualpetkmp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.virtualpetkmp.NotificacionReceiver

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
    return remember(context) {
        object : ProgramadorNotificaciones {
            override fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificacionReceiver::class.java).apply {
                    putExtra(NotificacionReceiver.EXTRA_TITULO, titulo)
                    putExtra(NotificacionReceiver.EXTRA_MENSAJE, mensaje)
                    putExtra(NotificacionReceiver.EXTRA_NOTIFICACION_ID, id.toInt())
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // En Android 12+, si el usuario no ha concedido SCHEDULE_EXACT_ALARM,
                // usamos set() (flexible) en lugar de setExactAndAllowWhileIdle() (exacto).
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, fechaDisparoMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparoMillis, pendingIntent)
                }
            }

            override fun cancelar(id: Long) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificacionReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id.toInt(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
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
