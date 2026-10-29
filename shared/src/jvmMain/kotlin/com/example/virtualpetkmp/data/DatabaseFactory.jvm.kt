package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import java.util.Properties

actual class DatabaseFactory {
    actual fun createDatabase(): VirtualPetDatabase {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".virtualpet")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val dbFile = File(appDir, "virtualpet.db")

        return abrirBaseDeDatos(crearDriverJdbc(dbFile), archivo = dbFile)
    }
}

/**
 * Driver JDBC sobre un fichero, con [propiedadesSqlite] (claves foráneas activas en cada conexión).
 *
 * Está aparte de [DatabaseFactory] para que los tests abran la base **exactamente igual** que la
 * app; si se configurara solo ahí dentro, los tests podrían pasar con una configuración distinta de
 * la de producción.
 */
internal fun crearDriverJdbc(archivo: File): SqlDriver =
    JdbcSqliteDriver(
        url = "jdbc:sqlite:${archivo.absolutePath}",
        properties = propiedadesSqlite()
    )

/**
 * Propiedades de la conexión JDBC de SQLite.
 *
 * **Por qué `foreign_keys=true` y no solo el `PRAGMA`.** El `PRAGMA foreign_keys` es una propiedad
 * **de cada conexión**, no de la base de datos. `JdbcSqliteDriver` con un fichero abre conexiones
 * nuevas constantemente (`ThreadedConnectionManager.closeConnection` hace `connection.close()` y
 * olvida la conexión cuando la sentencia no va dentro de una transacción), y cada una llegaba con
 * las claves foráneas **apagadas**: el `PRAGMA` que se ejecuta una vez al abrir no la alcanzaba.
 * El resultado era que borrar una mascota desde `Dispatchers.IO` dejaba sus vacunas, pesos y notas
 * huérfanas (reproducido con 19 filas en la base real).
 *
 * Pasando la propiedad al `DriverManager`, **todas** las conexiones nacen con las claves foráneas
 * activas. Comprobado: tres conexiones seguidas con estas propiedades devuelven
 * `PRAGMA foreign_keys = 1`; la misma conexión sin ellas devuelve `0`.
 */
internal fun propiedadesSqlite(): Properties = Properties().apply {
    setProperty("foreign_keys", "true")
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
 *
 * @param archivo fichero de la base, para poder hacer la copia previa a la limpieza de huérfanas.
 *   `null` en los tests en memoria, donde no hay fichero que copiar.
 */
internal fun abrirBaseDeDatos(
    driver: SqlDriver,
    esquema: SqlSchema<QueryResult.Value<Unit>> = SqlSchemaDelegado(VirtualPetDatabase.Schema),
    archivo: File? = null
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

    // El PRAGMA se queda por los tests en memoria: con `IN_MEMORY` sqldelight reutiliza una sola
    // conexión, así que aquí sí basta. En fichero, lo que manda es `propiedadesSqlite()`.
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

    val baseDeDatos = VirtualPetDatabase(driver)
    limpiarHuerfanosAlAbrir(baseDeDatos, driver, archivo)
    return baseDeDatos
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
