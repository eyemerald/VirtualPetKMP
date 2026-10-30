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
 * **El [message] es para el registro y la depuración, no para el usuario.** El texto que ve la
 * persona sale de los recursos de idioma (`base_mas_nueva_titulo` y `base_mas_nueva_mensaje`), lo
 * enseña `PantallaBaseMasNueva` a partir de las dos propiedades, y va traducido a los seis idiomas.
 *
 * Las dos versiones van como propiedades precisamente para eso: quien capture la excepción no
 * necesita analizar el mensaje.
 */
class BaseDeDatosMasNuevaException(
    val versionFichero: Long,
    val versionApp: Long
) : Exception(
    "La base de datos es de una versión más nueva de la aplicación " +
        "(datos en la versión $versionFichero; esta app entiende hasta la $versionApp). " +
        "No se ha tocado nada: actualiza la aplicación para poder usar estos datos."
)
