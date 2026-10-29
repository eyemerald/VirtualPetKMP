package com.example.virtualpetkmp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun createDatabase(): VirtualPetDatabase {
        // El esquema se envuelve para que Android ejecute las migraciones (.sqm) cuando la
        // base de datos del móvil ya existía con una versión anterior.
        val esquema = SqlSchemaDelegado(VirtualPetDatabase.Schema)

        val driver: SqlDriver = AndroidSqliteDriver(
            schema = esquema,
            context = context,
            name = "virtualpet.db",
            callback = CallbackConClavesForaneas(esquema)
        )

        return VirtualPetDatabase(driver)
    }
}

/**
 * Callback del esquema que activa las claves foráneas **en todas las conexiones**.
 *
 * SQLite las tiene apagadas por defecto, y el `PRAGMA foreign_keys` que había antes solo valía
 * para la conexión donde se ejecutaba: `SQLiteDatabase` mantiene un **pool de conexiones**, así
 * que el `ON DELETE CASCADE` dependía de que Android decidiera hacer el borrado en "la conexión
 * buena". Funcionaba, pero por un detalle del framework, no porque lo garantizáramos nosotros.
 *
 * `onConfigure` es el sitio que Android llama **al abrir la base**, y
 * `setForeignKeyConstraintsEnabled(true)` es una propiedad de la configuración del pool: se aplica
 * a todas las conexiones, presentes y futuras. Se mantiene el `onCreate`/`onUpgrade` del esquema
 * porque los hereda de [AndroidSqliteDriver.Callback].
 */
internal class CallbackConClavesForaneas(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    vararg callbacks: AfterVersion
) : AndroidSqliteDriver.Callback(schema, *callbacks) {

    override fun onConfigure(db: SupportSQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
