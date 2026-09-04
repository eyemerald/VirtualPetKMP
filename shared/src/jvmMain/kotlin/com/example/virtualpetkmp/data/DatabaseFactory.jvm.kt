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

        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        // Create schema if it doesn't exist
        VirtualPetDatabase.Schema.create(driver)

        return VirtualPetDatabase(driver)
    }
}
