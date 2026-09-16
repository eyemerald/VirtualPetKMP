package com.example.virtualpetkmp.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun createDatabase(): VirtualPetDatabase {
        val driver: SqlDriver = AndroidSqliteDriver(
            schema = VirtualPetDatabase.Schema,
            context = context,
            name = "virtualpet.db"
        )

        // SQLite tiene las claves foráneas desactivadas por defecto.
        // Sin esto, ON DELETE CASCADE de las tablas relacionadas (notas, etc.)
        // no se ejecutaría realmente al borrar una mascota.
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

        return VirtualPetDatabase(driver)
    }
}
