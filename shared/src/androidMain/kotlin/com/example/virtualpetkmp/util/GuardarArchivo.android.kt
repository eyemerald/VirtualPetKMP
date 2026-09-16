package com.example.virtualpetkmp.util

import java.io.File

actual fun guardarArchivo(rutaArchivo: String, contenido: ByteArray): Boolean {
    return try {
        // If a content URI was returned by the selector, write via ContentResolver
        if (rutaArchivo.startsWith("content://")) {
            try {
                val uri = android.net.Uri.parse(rutaArchivo)
                val activityThread = Class.forName("android.app.ActivityThread")
                val app = activityThread.getMethod("currentApplication").invoke(null) as android.app.Application
                app.contentResolver.openOutputStream(uri)?.use { it.write(contenido) }
                println("[guardarArchivo.android] wroteToContentUri size=${contenido.size}, uri=$rutaArchivo")
                // Assume success if no exception
                return true
            } catch (e: Exception) {
                println("[guardarArchivo.android] error writing to contentUri: ${e.message}")
                return false
            }
        }

        val destino = File(rutaArchivo)
        val parent = destino.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${destino.name}.tmp")
        tmp.outputStream().use { it.write(contenido) }
        val ok = tmp.length() > 0
        if (ok) {
            if (tmp.renameTo(destino)) {
                println("[guardarArchivo.android] wrote=${contenido.size}, finalSize=${destino.length()}, path=${destino.absolutePath}")
                true
            } else {
                // fallback: try overwrite
                destino.outputStream().use { it.write(contenido) }
                val finalOk = destino.length() > 0
                println("[guardarArchivo.android] rename failed, wrote fallback finalSize=${destino.length()}, ok=$finalOk")
                finalOk
            }
        } else {
            tmp.delete()
            println("[guardarArchivo.android] tmp write produced 0 bytes")
            false
        }
    } catch (e: Exception) {
        println("[guardarArchivo.android] error: ${e.message}")
        false
    }
}
