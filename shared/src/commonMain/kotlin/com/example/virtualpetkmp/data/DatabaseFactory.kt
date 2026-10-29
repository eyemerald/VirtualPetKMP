package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.db.VirtualPetDatabase

expect class DatabaseFactory {
    fun createDatabase(): VirtualPetDatabase
}

/**
 * La base de datos del usuario es de una **versión más nueva** que esta app.
 *
 * Pasa cuando se instala una versión anterior encima de una que ya migró el fichero (por ejemplo,
 * volver a un `.exe` o un APK viejos). Abrirla con un esquema que no conoce las columnas nuevas
 * sería el peor final posible: la app no puede escribir bien y, si intentara "arreglarlo", podría
 * perder datos. Así que **no se toca la base**: se lanza este error y el fichero se queda como está.
 *
 * El mensaje va en español y sin tecnicismos porque es lo que verá el usuario en el registro de
 * fallos; hoy no hay una pantalla que lo enseñe (eso pediría tocar la interfaz, fuera de este
 * arreglo), pero el fichero queda intacto y no se pierde nada.
 */
class BaseDeDatosMasNuevaException(
    val versionFichero: Long,
    val versionApp: Long
) : Exception(
    "La base de datos es de una versión más nueva de la aplicación " +
        "(datos en la versión $versionFichero; esta app entiende hasta la $versionApp). " +
        "No se ha tocado nada: actualiza la aplicación para poder usar estos datos."
)
