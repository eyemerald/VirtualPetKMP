package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun createDatabase(): VirtualPetDatabase {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".virtualpet")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val dbFile = File(appDir, "virtualpet.db")

        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbFile.absolutePath}")
        return abrirBaseDeDatos(driver)
    }
}

/**
 * Abre la base de datos de escritorio creándola o migrándola según lo que ya haya en el fichero.
 *
 * Es una función aparte, y no código suelto dentro de [DatabaseFactory], para poder probarla:
 * se le pasa un driver en memoria y se comprueba el ciclo completo sin tocar el disco.
 *
 * **Cómo se sabe en qué versión está el fichero.** SQLite reserva un entero para eso,
 * `PRAGMA user_version`, y es lo que se lee al abrir:
 *
 * - `0` → el fichero es nuevo (o lo creó una versión de la app que no lo marcaba): se ejecuta
 *   el esquema completo.
 * - menor que la versión actual → se aplican las migraciones `.sqm` que falten.
 * - igual → no hay nada que hacer.
 *
 * **El detalle que importaba:** ni crear ni migrar escriben `user_version` por su cuenta; eso lo
 * hace el driver de Android, pero **no** el de JDBC. Sin marcarlo a mano, el fichero se quedaba
 * siempre en `0` y cada arranque intentaba crear las tablas otra vez, así que la app de
 * escritorio reventaba al abrirse por segunda vez con un "table ... already exists". Aquí se
 * marca al final, tanto al crear como al migrar, para que el siguiente arranque no repita nada.
 */
internal fun abrirBaseDeDatos(
    driver: SqlDriver,
    esquema: SqlSchema<QueryResult.Value<Unit>> = SqlSchemaDelegado(VirtualPetDatabase.Schema)
): VirtualPetDatabase {
    val versionActual = driver.versionDeUsuario()

    when {
        versionActual == 0L -> esquema.create(driver).value
        versionActual < esquema.version -> esquema.migrate(driver, versionActual, esquema.version).value
    }

    // Se marca SIEMPRE, incluso cuando no había nada que hacer: así una base de datos creada por
    // una versión antigua de la app (que no lo marcaba) queda arreglada en el primer arranque.
    if (versionActual != esquema.version) {
        driver.execute(null, "PRAGMA user_version = ${esquema.version};", 0)
    }

    // SQLite tiene las claves foráneas desactivadas por defecto: sin esto, el ON DELETE CASCADE
    // de las tablas relacionadas no se ejecutaría de verdad al borrar una mascota.
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

    return VirtualPetDatabase(driver)
}

/** Versión del esquema que hay guardada en el fichero (`0` si nunca se ha marcado). */
private fun SqlDriver.versionDeUsuario(): Long =
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0
    ).value
