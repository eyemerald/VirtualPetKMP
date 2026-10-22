package com.example.virtualpetkmp.util

import java.io.File

actual fun carpetaDatosApp(): String = try {
    val app = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as? android.app.Application

    (app?.filesDir ?: File("/data/local/tmp")).absolutePath
} catch (e: Exception) {
    "/data/local/tmp"
}
