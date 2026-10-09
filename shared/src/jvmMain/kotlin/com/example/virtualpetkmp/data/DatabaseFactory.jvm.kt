package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
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

        // El esquema se envuelve para que, si la base de datos ya existía con una versión
        // anterior, se le apliquen las migraciones en lugar de recrearla.
        val esquema = SqlSchemaDelegado(VirtualPetDatabase.Schema)

        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbFile.absolutePath}")

        // Si el fichero es nuevo (versión 0) se crea el esquema completo; si ya existía en
        // una versión anterior, se migra conservando los datos.
        val versionActual = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0
        ).value

        if (versionActual == 0L) {
            esquema.create(driver).value
        } else if (versionActual < esquema.version) {
            esquema.migrate(driver, versionActual, esquema.version).value
        }

        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

        return VirtualPetDatabase(driver)
    }
}
