package com.example.virtualpetkmp.util

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

actual fun leerBytes(ruta: String): ByteArray? = try {
    File(ruta).takeIf { it.isFile }?.readBytes()
} catch (e: Exception) {
    println("[Archivos] no se pudo leer $ruta: ${e.message}")
    null
}

actual fun escribirBytes(ruta: String, contenido: ByteArray): Boolean = try {
    val destino = File(ruta)
    destino.parentFile?.mkdirs()
    destino.writeBytes(contenido)
    true
} catch (e: Exception) {
    println("[Archivos] no se pudo escribir $ruta: ${e.message}")
    false
}

actual fun existeArchivo(ruta: String): Boolean = File(ruta).isFile

actual fun borrarArchivo(ruta: String): Boolean = try {
    File(ruta).delete()
} catch (e: Exception) {
    false
}

actual fun borrarCarpeta(ruta: String): Boolean = try {
    File(ruta).deleteRecursively()
} catch (e: Exception) {
    false
}

actual fun listarArchivos(carpeta: String): List<String> =
    File(carpeta).listFiles()?.filter { it.isFile }?.map { it.absolutePath } ?: emptyList()

actual fun crearCarpeta(ruta: String): Boolean = try {
    File(ruta).mkdirs()
} catch (e: Exception) {
    false
}

actual fun nombreDeArchivo(ruta: String): String = ruta.substringAfterLast('/')

actual fun tamanoArchivo(ruta: String): Long = try {
    File(ruta).length()
} catch (e: Exception) {
    0L
}

actual fun sha256(contenido: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(contenido)
        .joinToString("") { byte -> ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

/**
 * Android: si el destino es una ruta `content://` (lo que devuelve el selector del sistema), se
 * escribe a través del `ContentResolver`; si es una ruta normal, se copia el fichero.
 *
 * El `Application` se obtiene de `ActivityThread` porque este código no tiene contexto: la
 * alternativa sería arrastrar un `Context` por toda la lógica de copia, que también se ejecuta en
 * los tests de escritorio.
 */
actual fun copiarArchivoA(destino: String, origen: String): Boolean = try {
    fun app(): android.app.Application? = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as? android.app.Application

    // Origen: fichero o content://
    fun abrirEntrada(): java.io.InputStream =
        if (origen.startsWith("content://")) {
            app()!!.contentResolver.openInputStream(android.net.Uri.parse(origen))!!
        } else {
            File(origen).inputStream()
        }

    // Destino: fichero o content://
    if (destino.startsWith("content://")) {
        val aplicacion = app() ?: return false
        aplicacion.contentResolver.openOutputStream(android.net.Uri.parse(destino))?.use { salida ->
            abrirEntrada().buffered().use { entrada -> entrada.copyTo(salida) }
        } ?: return false
        true
    } else {
        val salida = File(destino)
        salida.parentFile?.mkdirs()
        salida.outputStream().buffered().use { salidaStream ->
            abrirEntrada().buffered().use { entrada -> entrada.copyTo(salidaStream) }
        }
        true
    }
} catch (e: Exception) {
    println("[Archivos] no se pudo copiar a $destino: ${e.message}")
    false
}
actual fun escribirZip(
    destino: String,
    textos: Map<String, String>,
    archivos: Map<String, String>
): Boolean = try {
    val archivoDestino = File(destino)
    archivoDestino.parentFile?.mkdirs()

    ZipOutputStream(archivoDestino.outputStream().buffered()).use { zip ->
        textos.forEach { (nombre, contenido) ->
            zip.putNextEntry(ZipEntry(nombre))
            zip.write(contenido.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        archivos.forEach { (nombre, rutaLocal) ->
            zip.putNextEntry(ZipEntry(nombre))
            File(rutaLocal).inputStream().buffered().use { entrada ->
                entrada.copyTo(zip, DEFAULT_BUFFER_SIZE)
            }
            zip.closeEntry()
        }
    }
    true
} catch (e: Exception) {
    println("[Archivos] no se pudo escribir el ZIP $destino: ${e.message}")
    false
}

actual fun entradasDeZip(paquete: String): List<String> = try {
    ZipFile(File(paquete)).use { zip -> zip.entries().toList().map { it.name } }
} catch (e: Exception) {
    println("[Archivos] ZIP ilegible $paquete: ${e.message}")
    emptyList()
}

actual fun leerTextoDeZip(paquete: String, entrada: String): String? = try {
    ZipFile(File(paquete)).use { zip ->
        val e = zip.getEntry(entrada) ?: return null
        zip.getInputStream(e).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
} catch (e: Exception) {
    println("[Archivos] no se pudo leer '$entrada' de $paquete: ${e.message}")
    null
}

actual fun extraerDeZip(paquete: String, entrada: String, destino: String): Boolean = try {
    ZipFile(File(paquete)).use { zip ->
        val e = zip.getEntry(entrada) ?: return false
        val archivoDestino = File(destino)
        archivoDestino.parentFile?.mkdirs()
        zip.getInputStream(e).use { input ->
            archivoDestino.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        archivoDestino.length() >= 0
    }
} catch (e: Exception) {
    println("[Archivos] no se pudo extraer '$entrada': ${e.message}")
    false
}

actual fun tamanoDeEntradaZip(paquete: String, entrada: String): Long = try {
    ZipFile(File(paquete)).use { zip -> zip.getEntry(entrada)?.size ?: -1L }
} catch (e: Exception) {
    -1L
}

actual fun sha256DeEntradaZip(paquete: String, entrada: String): String? = try {
    ZipFile(File(paquete)).use { zip ->
        val e = zip.getEntry(entrada)
        if (e == null) {
            null
        } else {
            val digest = MessageDigest.getInstance("SHA-256")
            zip.getInputStream(e).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val leidos = input.read(buffer)
                    if (leidos <= 0) break
                    digest.update(buffer, 0, leidos)
                }
            }
            digest.digest().joinToString("") { b -> ((b.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
        }
    }
} catch (e: Exception) {
    null
}
