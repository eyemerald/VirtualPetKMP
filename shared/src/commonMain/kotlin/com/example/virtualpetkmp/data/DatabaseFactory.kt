package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.example.virtualpetkmp.db.VirtualPetDatabase

expect class DatabaseFactory {
    fun createDatabase(): VirtualPetDatabase
}

/**
 * Envuelve el esquema para que, al abrir una base de datos que ya existía en una versión
 * anterior, se ejecuten las migraciones (los ficheros `.sqm` de la carpeta `migrations`)
 * en lugar de recrear la base.
 *
 * Sin esto, las migraciones nunca se aplican: el driver se limita a abrir el fichero tal
 * cual y la app se encuentra un esquema viejo (columnas que faltan) o, si el driver intenta
 * actualizar la versión por su cuenta, un fallo al arrancar. Envolver el esquema es lo que
 * hace que el `ALTER TABLE` llegue de verdad a la base de datos del usuario, conservando
 * todos sus datos.
 */
internal class SqlSchemaDelegado(
    private val esquema: SqlSchema<QueryResult.Value<Unit>>
) : SqlSchema<QueryResult.Value<Unit>> {

    override val version: Long
        get() = esquema.version

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = esquema.create(driver)

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.Value<Unit> = esquema.migrate(driver, oldVersion, newVersion, *callbacks)
}
