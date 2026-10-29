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
 * Es una función aparte, y no código suelto dentro de [DatabaseFactory], para poder probarla: se le
 * pasa un driver (en memoria o sobre un fichero) y se comprueba el ciclo completo.
 *
 * **Cómo se sabe en qué versión está el fichero.** SQLite reserva un entero para eso,
 * `PRAGMA user_version`, y es lo que se lee al abrir:
 *
 * - `0` → el fichero es nuevo (o lo creó una versión de la app que no lo marcaba): se ejecuta el
 *   esquema completo.
 * - menor que la versión actual → se copia el fichero y se aplican las migraciones `.sqm` que
 *   falten, todo dentro de una transacción.
 * - igual → no hay nada que hacer.
 * - **mayor** que la versión actual → no se toca nada y se lanza [BaseDeDatosMasNuevaException].
 *
 * **El marcador (`user_version`) solo sube.** Antes se reescribía siempre que fuera distinto, lo
 * que incluía bajarlo: un ejecutable viejo abriendo una base nueva "la degradaba" en silencio y la
 * siguiente versión intentaba migrar sobre columnas que ya existían.
 *
 * **Limitación conocida:** un fichero con `user_version = 0` pero con tablas ya creadas (bases de
 * las primeras versiones de escritorio, que no marcaban la versión) entra por la rama "fichero
 * nuevo" y `create` falla con "table ... already exists". No se arregla aquí porque exigiría
 * adivinar el esquema que tenía ese fichero.
 *
 * @param archivo fichero de la base, para poder hacer las copias previas (migración y limpieza).
 *   `null` en los tests en memoria, donde no hay fichero que copiar.
 * @throws BaseDeDatosMasNuevaException si la base es más nueva que [esquema].
 */
internal fun abrirBaseDeDatos(
    driver: SqlDriver,
    esquema: SqlSchema<QueryResult.Value<Unit>> = VirtualPetDatabase.Schema,
    archivo: File? = null
): VirtualPetDatabase {
    val versionActual = driver.versionDeUsuario()

    // Nunca hacia atrás: si el fichero es más nuevo, se deja intacto y se avisa.
    if (versionActual > esquema.version) {
        throw BaseDeDatosMasNuevaException(versionFichero = versionActual, versionApp = esquema.version)
    }

    val baseDeDatos = VirtualPetDatabase(driver)

    when {
        versionActual == 0L -> baseDeDatos.transaction {
            esquema.create(driver).value
            marcarVersion(driver, esquema.version)
        }

        versionActual < esquema.version -> migrarConRedDeSeguridad(
            baseDeDatos = baseDeDatos,
            driver = driver,
            esquema = esquema,
            versionVieja = versionActual,
            archivo = archivo
        )
    }

    // El PRAGMA se queda por los tests en memoria: con `IN_MEMORY` sqldelight reutiliza una sola
    // conexión, así que aquí sí basta. En fichero, lo que manda es `propiedadesSqlite()`.
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

    limpiarHuerfanosAlAbrir(baseDeDatos, driver, archivo)

    return baseDeDatos
}

/**
 * Migra de [versionVieja] a la actual, con copia previa y en **una transacción**.
 *
 * La transacción se pide al código generado (`VirtualPetDatabase.transaction`) porque el
 * `endTransaction` del driver es protegido en sqldelight 2.x. Dentro de una transacción el driver
 * fija la conexión del hilo, así que las decenas de sentencias de la migración y el `user_version`
 * van juntos: o entra todo, o no entra nada (SQLite admite DDL transaccional).
 *
 * Antes de empezar se **copia el fichero** a `virtualpet-antes-de-migrar-vN.db` (N = versión de
 * origen), sobrescribiendo la copia anterior de esa misma versión. La copia no se usa para
 * recuperar automáticamente (la transacción ya lo garantiza); se deja por si el usuario necesita
 * volver atrás a mano.
 */
private fun migrarConRedDeSeguridad(
    baseDeDatos: VirtualPetDatabase,
    driver: SqlDriver,
    esquema: SqlSchema<QueryResult.Value<Unit>>,
    versionVieja: Long,
    archivo: File?
) {
    val copia = archivo?.let { copiarAntesDeMigrar(it, versionVieja) }
    try {
        baseDeDatos.transaction {
            esquema.migrate(driver, versionVieja, esquema.version).value
            marcarVersion(driver, esquema.version)
        }
    } catch (e: Exception) {
        // La transacción se deshace sola: el fichero queda como estaba antes de migrar.
        println(
            "[VirtualPet] la migración desde la versión $versionVieja falló (${e.message}). " +
                "No se ha cambiado nada." + (copia?.let { " Copia previa en $it" } ?: "")
        )
        throw e
    }
}

/** Copia el fichero antes de migrar. Devuelve la ruta de la copia. */
private fun copiarAntesDeMigrar(archivo: File, versionVieja: Long): String {
    val destino = File(archivo.parentFile, "virtualpet-antes-de-migrar-v$versionVieja.db")
    archivo.copyTo(destino, overwrite = true)
    return destino.absolutePath
}

/** Escribe la versión del esquema en el fichero. Solo se llama para subirla. */
private fun marcarVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0)
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
