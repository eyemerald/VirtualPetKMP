package com.example.virtualpetkmp.util

/**
 * Android: se pregunta al sistema por el `versionName` instalado, que es el que el usuario ve en
 * Ajustes. Así no hay que repetir el número en dos sitios que se puedan desincronizar.
 */
actual fun versionDeLaApp(): String = try {
    val app = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as? android.app.Application

    app?.packageManager?.getPackageInfo(app.packageName, 0)?.versionName ?: ""
} catch (e: Exception) {
    ""
}

actual fun plataformaDeLaApp(): String = "android"
