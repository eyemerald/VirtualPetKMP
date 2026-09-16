package com.example.virtualpetkmp.util

import java.io.File

actual fun guardarArchivo(rutaArchivo: String, contenido: ByteArray): Boolean {
    return try {
        val destino = File(rutaArchivo)
        val tmp = File(destino.parentFile, "${destino.name}.tmp")
        tmp.outputStream().use { it.write(contenido) }
        val ok = tmp.length() > 0
        if (ok) {
            tmp.renameTo(destino)
        } else {
            tmp.delete()
        }
        println("[guardarArchivo.jvm] wrote=${contenido.size}, tmpSize=${tmp.length()}, ok=$ok, path=${destino.absolutePath}")
        ok
    } catch (e: Exception) {
        println("[guardarArchivo.jvm] error: ${e.message}")
        false
    }
}
