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
        val driver: SqlDriver = AndroidSqliteDriver(
            schema = VirtualPetDatabase.Schema,
            context = context,
            name = "virtualpet.db",
            callback = CallbackConClavesForaneas(VirtualPetDatabase.Schema)
        )

        // `AndroidSqliteDriver` abre el fichero **en la primera consulta**, no al construirse. Se
        // fuerza aquí para que un fichero de una versión más nueva lance
        // `BaseDeDatosMasNuevaException` al arrancar: así `App` puede enseñar el aviso traducido en
        // vez de aparecer como un error de la lista ("Error al cargar mascotas: ...").
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(Unit)
            },
            parameters = 0
        ).value

        return VirtualPetDatabase(driver)
    }
}

/**
 * Callback del esquema: activa las claves foráneas y rechaza una base más nueva.
 *
 * **Claves foráneas.** SQLite las tiene apagadas por defecto, y el `PRAGMA foreign_keys` que había
 * antes solo valía para la conexión donde se ejecutaba: `SQLiteDatabase` mantiene un **pool de
 * conexiones**, así que el `ON DELETE CASCADE` dependía de que Android hiciera el borrado en "la
 * conexión buena". `onConfigure` se llama al abrir la base y
 * `setForeignKeyConstraintsEnabled(true)` es configuración del pool: vale para todas las conexiones,
 * presentes y futuras.
 *
 * **Base más nueva que la app.** `onDowngrade` es lo que llama Android cuando el fichero tiene una
 * versión mayor que el esquema del código. Por defecto lanzaría un mensaje en inglés; aquí se lanza
 * [BaseDeDatosMasNuevaException] con el mensaje en español y **sin tocar la base**.
 *
 * `onCreate`/`onUpgrade` (los del esquema, que aplican las migraciones `.sqm`) se heredan de
 * [AndroidSqliteDriver.Callback].
 */
internal class CallbackConClavesForaneas(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    vararg callbacks: AfterVersion
) : AndroidSqliteDriver.Callback(schema, *callbacks) {

    override fun onConfigure(db: SupportSQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw BaseDeDatosMasNuevaException(
            versionFichero = oldVersion.toLong(),
            versionApp = newVersion.toLong()
        )
    }
}
