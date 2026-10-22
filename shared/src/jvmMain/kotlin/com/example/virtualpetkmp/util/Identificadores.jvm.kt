package com.example.virtualpetkmp.util

/**
 * Escritorio: el mismo `UUID.randomUUID()` que en Android. Al ser también JVM, un ordenador y un
 * móvil generan identificadores con el mismo formato y la misma garantía de unicidad.
 */
actual fun nuevoIdentificadorUniversal(): String =
    java.util.UUID.randomUUID().toString().replace("-", "")
