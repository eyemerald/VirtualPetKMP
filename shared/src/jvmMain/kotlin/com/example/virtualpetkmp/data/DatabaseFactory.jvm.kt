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

        // Al pasarle el schema, JdbcSqliteDriver gestiona automáticamente
        // si hay que crear la base de datos desde cero o aplicar las
        // migraciones pendientes (usando PRAGMA user_version internamente).
        val driver: SqlDriver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            schema = VirtualPetDatabase.Schema
        )

        // SQLite tiene las claves foráneas desactivadas por defecto.
        // Sin esto, ON DELETE CASCADE de las tablas relacionadas (notas, etc.)
        // no se ejecutaría realmente al borrar una mascota.
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

        return VirtualPetDatabase(driver)
    }
}
