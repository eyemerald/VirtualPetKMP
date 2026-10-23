package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las migraciones de esquema, que son lo único que puede **perder los datos del usuario**.
 *
 * Hasta ahora había once ficheros `.sqm` sin ninguna prueba: si uno estuviera mal, el fallo
 * aparecería en el móvil de quien actualiza la app —y solo en el suyo, porque una instalación
 * nueva crea el esquema directamente y no migra nada—. Ese es exactamente el fallo que no se
 * detecta probando la app desde cero.
 *
 * Se comprueban dos cosas distintas:
 *
 * 1. **Los datos sobreviven**, desde cualquier versión anterior hasta la última. No basta con
 *    probar el salto desde la penúltima: cada versión intermedia es un punto de partida posible
 *    y el recorrido de migraciones es distinto en cada caso.
 * 2. **El esquema migrado es idéntico al de una instalación nueva.** Si alguien añade una columna
 *    al `.sq` y se olvida del `.sqm` (o al revés), las dos rutas acaban con bases de datos
 *    distintas y el fallo aparecerá mucho después, en forma de "a mí no me sale ese campo".
 *
 * **Limitación consciente:** la versión 1 de `mascotas` se reconstruye aquí a mano, porque la
 * tabla es anterior a los ficheros de migración (ninguno la crea) y su historia no está en el
 * repositorio. Se deduce del esquema actual quitando lo que añaden las migraciones 10 (`foto`) y
 * 11 (`peso_ideal`). Es decir: esto valida las **sentencias de migración**, no el `create()` que
 * tuviera la app hace años.
 */
class MigracionesTest {

    private val versionFinal = VirtualPetDatabase.Schema.version

    /**
     * Esquema de la versión 1: **solo la tabla `mascotas`**, y sin las columnas que llegaron
     * después.
     *
     * Los ficheros `.sqm` se numeran por la versión **desde** la que migran (lo confirma el
     * código generado: `1.sqm` se aplica con `oldVersion <= 1`), así que la versión 1 es la base
     * tal y como la dejaba la primera versión de la app: la tabla de mascotas y nada más. `notas`
     * la añade `1.sqm` al pasar a la 2.
     *
     * Las columnas de `mascotas` se deducen del esquema actual quitando lo que añaden las
     * migraciones 10 (`foto`) y 11 (`peso_ideal`), porque la tabla es anterior a los ficheros de
     * migración y su historia no está en el repositorio.
     */
    private fun crearEsquemaV1(driver: SqlDriver) {
        driver.ejecutar(
            """
            CREATE TABLE mascotas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                especie TEXT NOT NULL,
                raza TEXT NOT NULL,
                fechaNacimiento TEXT NOT NULL,
                sexo TEXT NOT NULL,
                color TEXT NOT NULL,
                microchip TEXT
            );
            """.trimIndent()
        )
    }

    private fun nuevoDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    /**
     * Datos de un usuario real: una mascota con su ficha rellena.
     *
     * **Se adapta a la versión de la que se parte.** Desde la 14 la tabla tiene `uuid` y
     * `actualizadoEn`, y una fila insertada sin ellos se quedaría con los valores por defecto de la
     * migración (`''` y `0`): eso es un artefacto del test, porque la app nunca escribe así. Antes
     * de la 14 esas columnas no existen y no se pueden nombrar.
     */
    private fun insertarMascota(driver: SqlDriver) {
        val tieneUuid = driver.columnasDe("mascotas").any { it.startsWith("uuid ") }

        val columnas = if (tieneUuid) {
            "(nombre, especie, raza, fechaNacimiento, sexo, color, microchip, uuid, actualizadoEn)"
        } else {
            "(nombre, especie, raza, fechaNacimiento, sexo, color, microchip)"
        }
        val valores = if (tieneUuid) {
            "('Luna', 'Perro', 'Labrador Retriever', '2024-02-04', 'Hembra', 'Negro', '941000012345678', " +
                "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1000)"
        } else {
            "('Luna', 'Perro', 'Labrador Retriever', '2024-02-04', 'Hembra', 'Negro', '941000012345678')"
        }

        driver.ejecutar("INSERT INTO mascotas $columnas VALUES $valores;")
    }

    /** Las notas solo existen a partir de la versión 2, así que no se pueden meter antes. */
    private fun insertarNotas(driver: SqlDriver) {
        // Igual que con la mascota: desde la 14 la tabla lleva uuid y actualizadoEn.
        val tieneUuid = driver.columnasDe("notas").any { it.startsWith("uuid ") }

        if (tieneUuid) {
            driver.ejecutar(
                "INSERT INTO notas (mascotaId, texto, uuid, actualizadoEn) VALUES " +
                    "(1, 'Primera nota', 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1', 1000);"
            )
            driver.ejecutar(
                "INSERT INTO notas (mascotaId, texto, uuid, actualizadoEn) VALUES " +
                    "(1, 'Segunda nota', 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2', 1000);"
            )
        } else {
            driver.ejecutar("INSERT INTO notas (mascotaId, texto) VALUES (1, 'Primera nota');")
            driver.ejecutar("INSERT INTO notas (mascotaId, texto) VALUES (1, 'Segunda nota');")
        }
    }

    private fun hayTabla(driver: SqlDriver, tabla: String): Boolean =
        driver.tablas().contains(tabla)

    /** Comprueba que la mascota (y sus notas, si las había) siguen ahí después de migrar. */
    private fun comprobarDatosIntactos(driver: SqlDriver, desde: Long, conNotas: Boolean) {
        val fila = driver.consulta(
            "SELECT nombre, especie, raza, fechaNacimiento, sexo, color, microchip FROM mascotas;",
            columnas = 7
        )
        assertEquals(1, fila.size, "se perdió la mascota al migrar desde la versión $desde")
        assertEquals(
            listOf("Luna", "Perro", "Labrador Retriever", "2024-02-04", "Hembra", "Negro", "941000012345678"),
            fila.first(),
            "algún campo de la mascota cambió al migrar desde la versión $desde"
        )

        if (conNotas) {
            val notas = driver.consulta("SELECT texto FROM notas ORDER BY id;")
            assertEquals(
                listOf(listOf("Primera nota"), listOf("Segunda nota")),
                notas,
                "se perdieron notas al migrar desde la versión $desde"
            )
        }

        // Las columnas del traspaso de datos (migración 13) se rellenan solas: una mascota sin
        // identificador universal no se podría reconocer en otro dispositivo, y una sin fecha de
        // modificación no se podría comparar con la copia del otro lado.
        val traspaso = driver.consulta("SELECT uuid, actualizadoEn FROM mascotas;", columnas = 2).single()
        assertEquals(
            32,
            traspaso[0]?.length,
            "la mascota migrada desde la versión $desde se quedó sin identificador universal"
        )
        assertTrue(
            (traspaso[1]?.toLongOrNull() ?: 0L) > 0L,
            "la mascota migrada desde la versión $desde se quedó sin fecha de modificación"
        )

        if (conNotas) {
            val identificadores = driver.consulta("SELECT uuid FROM notas ORDER BY id;")
                .map { it.first() }
            assertEquals(2, identificadores.toSet().size, "las notas migradas comparten identificador")
        }
    }

    @Test
    fun `los datos sobreviven migrando desde cualquier version anterior`() {
        // Cada versión intermedia es un punto de partida posible: quien no actualiza la app en
        // meses se salta varias de golpe, y el recorrido no es el mismo que desde la penúltima.
        (1 until versionFinal).forEach { desde ->
            val driver = nuevoDriver()
            crearEsquemaV1(driver)

            // Se deja la base de datos tal y como la tendría un usuario en esa versión.
            if (desde > 1) {
                VirtualPetDatabase.Schema.migrate(driver, 1, desde).value
            }
            insertarMascota(driver)
            val conNotas = hayTabla(driver, "notas")
            if (conNotas) insertarNotas(driver)

            // Y ahora se actualiza la app.
            VirtualPetDatabase.Schema.migrate(driver, desde, versionFinal).value

            comprobarDatosIntactos(driver, desde, conNotas)

            // Las columnas que llegan con las migraciones 10 y 11 quedan vacías, no con basura.
            val nuevas = driver.consulta("SELECT foto, peso_ideal FROM mascotas;", columnas = 2)
            assertEquals(listOf(listOf<String?>(null, null)), nuevas, "las columnas nuevas no salieron vacías")
        }
    }

    @Test
    fun `el esquema migrado es identico al de una instalacion nueva`() {
        // Esta es la comprobación que caza el despiste clásico: tocar el `.sq` y olvidar el
        // `.sqm`. Las dos rutas (instalar de cero y actualizar) tienen que acabar iguales.
        val recienInstalada = nuevoDriver().apply { VirtualPetDatabase.Schema.create(this).value }

        val migrada = nuevoDriver()
        crearEsquemaV1(migrada)
        VirtualPetDatabase.Schema.migrate(migrada, 1, versionFinal).value

        val tablasNueva = recienInstalada.tablas()
        val tablasMigrada = migrada.tablas()
        assertEquals(tablasNueva, tablasMigrada, "las tablas no coinciden entre instalar y migrar")

        tablasNueva.forEach { tabla ->
            assertEquals(
                recienInstalada.columnasDe(tabla),
                migrada.columnasDe(tabla),
                "las columnas de '$tabla' no coinciden entre instalar y migrar"
            )

            // Los índices también: la migración 14 crea uno por tabla y, si alguien lo añade al
            // `.sq` y se olvida del `.sqm`, las dos rutas acaban con bases distintas (una con
            // índice y otra sin él). No rompe nada, pero hace que la fusión vaya más lenta en un
            // caso y no en el otro, y es justo el despiste que esta prueba existe para cazar.
            assertEquals(
                recienInstalada.indicesDe(tabla),
                migrada.indicesDe(tabla),
                "los índices de '$tabla' no coinciden entre instalar y migrar"
            )
        }
    }

    @Test
    fun `la base de datos migrada se puede usar desde los repositorios`() = runBlocking {
        // Que las columnas existan no basta: tienen que poder leerse con las consultas generadas.
        // Un `SELECT *` sobre una tabla migrada a medias revienta justo aquí.
        val driver = nuevoDriver()
        crearEsquemaV1(driver)
        insertarMascota(driver)
        VirtualPetDatabase.Schema.migrate(driver, 1, versionFinal).value

        val db = VirtualPetDatabase(driver)
        val mascota = MascotaRepository(db).getMascotaById(1L)

        assertEquals("Luna", mascota?.nombre)
        assertNull(mascota?.foto)
        assertNull(mascota?.pesoIdeal)
    }

    @Test
    fun `abrir la base de datos dos veces seguidas no revienta`() = runBlocking {
        // Este era el fallo de la app de escritorio: ni crear ni migrar marcan `user_version`
        // por su cuenta (eso lo hace el driver de Android, pero no el de JDBC), así que el
        // fichero se quedaba en 0 y CADA arranque intentaba crear las tablas otra vez. El
        // segundo arranque moría con un "table ... already exists".
        val driver = nuevoDriver()

        // Primer arranque: se crea todo y se deja marcado.
        val primera = abrirBaseDeDatos(driver)
        // Se guarda por el repositorio y no con SQL a pelo: en una instalación nueva la tabla ya
        // exige `uuid` y `actualizadoEn`, y así se comprueba también el camino real de escritura.
        MascotaRepository(primera).insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Labrador Retriever",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = "941000012345678"
            )
        ).getOrThrow()
        assertEquals(1, primera.mascotasQueries.selectAll().executeAsList().size)
        assertEquals(versionFinal, driver.userVersion())

        // Segundo arranque sobre el mismo fichero: no toca el esquema y no pierde nada.
        val segunda = abrirBaseDeDatos(driver)
        assertEquals(1, segunda.mascotasQueries.selectAll().executeAsList().size)
        assertEquals("Luna", segunda.mascotasQueries.selectAll().executeAsList().first().nombre)
    }

    @Test
    fun `migrar deja la base marcada con la ultima version`() {
        // Sin marcarla, el arranque siguiente volvería a migrar y fallaría con
        // "duplicate column name" o "table already exists".
        val driver = nuevoDriver()
        crearEsquemaV1(driver)
        driver.ejecutar("PRAGMA user_version = 1;")

        abrirBaseDeDatos(driver)

        assertEquals(versionFinal, driver.userVersion())
    }

    @Test
    fun `una instalacion nueva queda marcada con la ultima version`() {
        // Si `create()` dejara la base en la versión 0, la app intentaría crear otra vez las
        // tablas en el arranque siguiente sobre un esquema que ya existe.
        val driver = nuevoDriver()

        abrirBaseDeDatos(driver)

        assertEquals(versionFinal, driver.userVersion())
    }

    @Test
    fun `dos arranques de la app de escritorio sobre el mismo fichero`() {
        // La prueba de verdad del fallo: no un driver en memoria reutilizado, sino abrir el
        // fichero en disco y volver a abrirlo con una conexión nueva, que es lo que pasa cada
        // vez que el usuario ejecuta la aplicación de escritorio.
        val carpeta = Files.createTempDirectory("virtualpet-migraciones").toFile()
        val archivo = File(carpeta, "virtualpet.db")
        val url = "jdbc:sqlite:${archivo.absolutePath}"

        try {
            // Primer arranque: la base no existe.
            val primerDriver = JdbcSqliteDriver(url)
            try {
                abrirBaseDeDatos(primerDriver)
            } finally {
                primerDriver.close()
            }
            assertTrue(archivo.exists(), "el primer arranque no creó el fichero")

            // Segundo arranque: el fichero ya está ahí, con todas las tablas creadas.
            val segundoDriver = JdbcSqliteDriver(url)
            try {
                val db = abrirBaseDeDatos(segundoDriver)
                assertTrue(db.mascotasQueries.selectAll().executeAsList().isEmpty())
            } finally {
                segundoDriver.close()
            }
        } finally {
            carpeta.deleteRecursively()
        }
    }
}

// --- Utilidades para hablar con SQLite directamente desde el test ---

private fun SqlDriver.ejecutar(sql: String) {
    execute(null, sql, 0)
}

/** Ejecuta una consulta y devuelve todas las filas como texto (o `null` donde no haya valor). */
private fun SqlDriver.consulta(sql: String, columnas: Int = 1): List<List<String?>> =
    executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            val filas = mutableListOf<List<String?>>()
            while (cursor.next().value) {
                filas.add((0 until columnas).map { cursor.getString(it) })
            }
            QueryResult.Value(filas)
        },
        parameters = 0
    ).value

private fun SqlDriver.tablas(): List<String> =
    executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name;",
        mapper = { cursor ->
            val nombres = mutableListOf<String>()
            while (cursor.next().value) {
                cursor.getString(0)?.let { nombres.add(it) }
            }
            QueryResult.Value(nombres)
        },
        parameters = 0
    ).value

/** Índices de una tabla: `nombre (columna, ...)`, ordenados, para poder compararlos. */
private fun SqlDriver.indicesDe(tabla: String): List<String> =
    executeQuery(
        identifier = null,
        sql = "PRAGMA index_list($tabla);",
        mapper = { cursor ->
            val indices = mutableListOf<String>()
            while (cursor.next().value) {
                // seq, name, unique, origin, partial
                val nombre = cursor.getString(1) ?: continue
                indices.add(nombre)
            }
            QueryResult.Value(indices.sorted())
        },
        parameters = 0
    ).value

/** Columnas de una tabla, con su tipo y si admiten nulos: `nombre tipo notnull`. */
private fun SqlDriver.columnasDe(tabla: String): List<String> =
    executeQuery(
        identifier = null,
        sql = "PRAGMA table_info($tabla);",
        mapper = { cursor ->
            val columnas = mutableListOf<String>()
            while (cursor.next().value) {
                // cid, name, type, notnull, dflt_value, pk
                columnas.add("${cursor.getString(1)} ${cursor.getString(2)} ${cursor.getLong(3)}")
            }
            QueryResult.Value(columnas)
        },
        parameters = 0
    ).value

private fun SqlDriver.userVersion(): Long =
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version;",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0
    ).value
