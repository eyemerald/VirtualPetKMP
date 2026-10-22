package com.example.virtualpetkmp.util

/**
 * Android: `UUID.randomUUID()` (generador criptográfico del sistema), sin guiones, para que el
 * formato coincida con el de la migración SQL (`hex(randomblob(16))`).
 */
actual fun nuevoIdentificadorUniversal(): String =
    java.util.UUID.randomUUID().toString().replace("-", "")
