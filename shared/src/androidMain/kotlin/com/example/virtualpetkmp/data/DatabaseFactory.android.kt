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
        return VirtualPetDatabase(driver)
    }
}
