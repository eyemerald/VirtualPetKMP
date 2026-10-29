package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File

/**
 * Limpieza, **una sola vez al abrir**, de las filas que se quedaron sin padre.
 *
 * **De dónde vienen.** En la app de escritorio, el `PRAGMA foreign_keys` solo afectaba a la
 * conexión donde se ejecutaba, así que el `ON DELETE CASCADE` no se aplicaba y cada mascota
 * borrada dejaba detrás sus vacunas, pesos, informes… La causa está arreglada en
 * `propiedadesSqlite()`, pero los ficheros que ya se borraron así siguen con esas filas: no se
 * pueden borrar con una migración `.sqm` (las migraciones son cambios de esquema, y esto es
 * limpieza de datos de un fallo concreto).
 *
 * **Qué hace, y en qué orden:**
 * 1. `PRAGMA foreign_key_check` — lo hace SQLite, no una lista de consultas nuestra.
 * 2. Si no hay nada, no toca nada (ni copia).
 * 3. Si hay algo, **primero copia el fichero** a `virtualpet-antes-de-limpiar.db`, para que el
 *    usuario pueda volver atrás a mano.
 * 4. Borra las filas huérfanas en **una transacción** (todo o nada).
 * 5. Lo deja escrito en un log por consola, con el recuento por tabla.
 *
 * No hay riesgo de borrar datos buenos: una fila huérfana es, por definición, una que apunta a una
 * mascota que ya no existe y que la app no puede mostrar en ninguna pantalla.
 */

/** Nombre de la copia de seguridad que se deja antes de limpiar. */
internal const val COPIA_ANTES_DE_LIMPIAR = "virtualpet-antes-de-limpiar.db"

/**
 * Tablas hijas conocidas. `PRAGMA foreign_key_check` devuelve el nombre de la tabla como texto y
 * ese texto se interpola en el `DELETE`, así que solo se aceptan nombres de esta lista: si la base
 * estuviera corrupta y devolviera otra cosa, se ignora en vez de ejecutar SQL inventado.
 */
private val tablasHijasConocidas = setOf(
    "vacunas", "preventivos", "revisiones", "tratamientos", "pesos", "informes", "notas"
)

/** Una fila huérfana: en qué tabla está y qué `rowid` tiene. */
private data class Huerfana(val tabla: String, val rowid: Long)

/**
 * Borra las filas huérfanas que ya hubiera en la base. Devuelve cuántas ha borrado (0 si no había).
 *
 * La transacción se pide al **generado** (`VirtualPetDatabase.transaction`), no al driver: el
 * `endTransaction` del driver es protegido en sqldelight 2.x. Además, dentro de una transacción el
 * driver fija la conexión del hilo, así que los `DELETE` van por la misma conexión.
 *
 * @param archivo fichero de la base, del que se hace copia antes de limpiar. `null` (tests en
 *   memoria) → no se copia nada, solo se limpia.
 */
internal fun limpiarHuerfanosAlAbrir(baseDeDatos: VirtualPetDatabase, driver: SqlDriver, archivo: File?): Int {
    val huerfanas = driver.filasHuerfanas()
    if (huerfanas.isEmpty()) return 0

    val copia = archivo?.let { copiarAntesDeLimpiar(it) }

    baseDeDatos.transaction {
        huerfanas.forEach { huerfana ->
            driver.execute(
                identifier = null,
                sql = "DELETE FROM ${huerfana.tabla} WHERE rowid = ?;",
                parameters = 1
            ) { bindLong(0, huerfana.rowid) }
        }
    }

    val porTabla = huerfanas.groupingBy { it.tabla }.eachCount()
        .entries
        .joinToString(", ") { "${it.key}: ${it.value}" }
    println(
        "[VirtualPet] limpieza de filas huérfanas: ${huerfanas.size} borradas ($porTabla). " +
            (copia?.let { "Copia previa en $it" } ?: "Sin copia (base en memoria)")
    )
    return huerfanas.size
}

/** Copia el fichero de la base al lado, sobrescribiendo la copia anterior. Devuelve su ruta. */
private fun copiarAntesDeLimpiar(archivo: File): String {
    val destino = File(archivo.parentFile, COPIA_ANTES_DE_LIMPIAR)
    archivo.copyTo(destino, overwrite = true)
    return destino.absolutePath
}

/**
 * Filas huérfanas según SQLite.
 *
 * `PRAGMA foreign_key_check` devuelve una fila por cada fila hija cuyo padre no existe:
 * `(tabla, rowid, padre, índiceDeClaveForánea)`. Es una comprobación del motor, así que también
 * encuentra las que se crearon con las claves foráneas apagadas.
 */
private fun SqlDriver.filasHuerfanas(): List<Huerfana> =
    executeQuery(
        identifier = null,
        sql = "PRAGMA foreign_key_check;",
        mapper = { cursor ->
            val filas = mutableListOf<Huerfana>()
            while (cursor.next().value) {
                val tabla = cursor.getString(0)
                val rowid = cursor.getLong(1)
                if (tabla != null && rowid != null && tabla in tablasHijasConocidas) {
                    filas.add(Huerfana(tabla, rowid))
                }
            }
            QueryResult.Value(filas)
        },
        parameters = 0
    ).value
