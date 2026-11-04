package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * La apertura de la base de datos no puede estropear un fichero que el usuario ya tiene.
 *
 * Tres casos, todos con fichero de verdad:
 * 1. **La base es más nueva que la app** (versión del fichero > versión del esquema): no se toca
 *    nada y se lanza [BaseDeDatosMasNuevaException]. Antes, el marcador se reescribía hacia abajo y
 *    la siguiente versión intentaba migrar sobre columnas que ya existían.
 * 2. **La migración falla a mitad**: la transacción se deshace, el `user_version` no cambia y la
 *    base se puede volver a abrir (y migrar) después.
 * 3. **Antes de migrar se deja copia** del fichero: `virtualpet-antes-de-migrar-vN.db`.
 */
class AperturaBaseDatosTest {

    private lateinit var carpeta: File
    private lateinit var archivo: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-apertura").toFile()
        archivo = File(carpeta, "virtualpet.db")
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    private fun SqlDriver.ejecutar(sql: String) = execute(null, sql, 0)

    private fun SqlDriver.numero(sql: String): Long =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: -1L)
            },
            parameters = 0
        ).value

    /** Deja un fichero con el esquema actual, una mascota y la versión que se le pida. */
    private fun crearBaseConUnaMascota(version: Long) {
        val driver = crearDriverJdbc(archivo)
        try {
            VirtualPetDatabase.Schema.create(driver).value
            driver.ejecutar(
                "INSERT INTO mascotas (nombre, especie, raza, fechaNacimiento, sexo, color, uuid, actualizadoEn) " +
                    "VALUES ('Luna', 'Perro', 'Labrador', '2024-02-04', 'Hembra', 'Negro', " +
                    "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 1)"
            )
            // El fichero tiene que ser el de una version ANTERIOR de verdad: se le quitan las
            // columnas que anade la ultima migracion (16). Antes no hacia falta porque la ultima
            // migracion era un CREATE INDEX IF NOT EXISTS y se podia repetir; un ALTER TABLE ADD
            // COLUMN no, y repetirlo da "duplicate column name".
            driver.ejecutar("ALTER TABLE tratamientos DROP COLUMN vecesAlDia;")
            driver.ejecutar("ALTER TABLE tratamientos DROP COLUMN horas;")
            driver.ejecutar("PRAGMA user_version = $version;")
        } finally {
            driver.close()
        }
    }

    @Test
    fun `una base mas nueva que la app no se toca y se rechaza`() {
        val versionDelFichero = VirtualPetDatabase.Schema.version + 5
        crearBaseConUnaMascota(versionDelFichero)

        val driver = crearDriverJdbc(archivo)
        try {
            val excepcion = assertFailsWith<BaseDeDatosMasNuevaException> {
                abrirBaseDeDatos(driver, archivo = archivo)
            }
            assertEquals(versionDelFichero, excepcion.versionFichero)
            assertEquals(VirtualPetDatabase.Schema.version, excepcion.versionApp)
            assertTrue(
                excepcion.message!!.contains("versión más nueva"),
                "el mensaje tiene que ser claro: ${excepcion.message}"
            )

            // La base sigue intacta: misma versión, misma mascota y sin copias de migración.
            assertEquals(versionDelFichero, driver.numero("PRAGMA user_version;"))
            assertEquals(1, driver.numero("SELECT count(*) FROM mascotas;"))
            assertTrue(
                carpeta.listFiles()!!.none { it.name.startsWith("virtualpet-antes-de-migrar") },
                "no se debe migrar (ni copiar) cuando la base es más nueva"
            )
        } finally {
            driver.close()
        }
    }

    /** Esquema que falla **a mitad**: ejecuta una sentencia y luego revienta. */
    private class EsquemaQueFallaAmitad(private val versionDeclarada: Long) : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long get() = versionDeclarada

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> =
            throw UnsupportedOperationException("create no se usa en este test")

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> {
            driver.execute(null, "ALTER TABLE mascotas ADD COLUMN columna_a_medias TEXT;", 0)
            throw IllegalStateException("fallo simulado a mitad de la migración")
        }
    }

    @Test
    fun `si la migracion falla a mitad no cambia la version y la base sigue utilizable`() {
        val versionVieja = VirtualPetDatabase.Schema.version - 1
        crearBaseConUnaMascota(versionVieja)

        // 1. Abrir con un esquema que revienta después de una sentencia.
        val driverQueFalla = crearDriverJdbc(archivo)
        try {
            assertFailsWith<IllegalStateException> {
                abrirBaseDeDatos(driverQueFalla, EsquemaQueFallaAmitad(VirtualPetDatabase.Schema.version), archivo)
            }
            assertEquals(
                versionVieja,
                driverQueFalla.numero("PRAGMA user_version;"),
                "el marcador no puede subir si la migración no terminó"
            )
            // La sentencia intermedia se deshizo con la transacción.
            val columnas = driverQueFalla.numero(
                "SELECT count(*) FROM pragma_table_info('mascotas') WHERE name = 'columna_a_medias';"
            )
            assertEquals(0, columnas, "la migración fallida dejó cambios a medias")
            assertEquals(1, driverQueFalla.numero("SELECT count(*) FROM mascotas;"), "se perdió la mascota")
        } finally {
            driverQueFalla.close()
        }

        // 2. Y la app, con su esquema de verdad, puede abrirla y terminar la migración.
        val driverBueno = crearDriverJdbc(archivo)
        try {
            val db = abrirBaseDeDatos(driverBueno, archivo = archivo)
            assertEquals(
                VirtualPetDatabase.Schema.version,
                driverBueno.numero("PRAGMA user_version;"),
                "el segundo intento no dejó la versión al día"
            )
            assertEquals("Luna", db.mascotasQueries.selectAll().executeAsList().single().nombre)
        } finally {
            driverBueno.close()
        }
    }

    @Test
    fun `antes de migrar se deja una copia del fichero con la version de origen`() {
        val versionVieja = VirtualPetDatabase.Schema.version - 1
        crearBaseConUnaMascota(versionVieja)

        val driver = crearDriverJdbc(archivo)
        try {
            abrirBaseDeDatos(driver, archivo = archivo)
            assertEquals(VirtualPetDatabase.Schema.version, driver.numero("PRAGMA user_version;"))
        } finally {
            driver.close()
        }

        val copia = File(carpeta, "virtualpet-antes-de-migrar-v$versionVieja.db")
        assertTrue(copia.exists(), "no se dejó la copia previa a migrar")
        assertTrue(copia.length() > 0, "la copia está vacía")

        // La copia guarda la versión de origen: es la foto de antes de migrar.
        val driverCopia = JdbcSqliteDriver(url = "jdbc:sqlite:${copia.absolutePath}")
        try {
            assertEquals(versionVieja, driverCopia.numero("PRAGMA user_version;"))
        } finally {
            driverCopia.close()
        }

        // Y la segunda migración de la misma versión sobrescribe la copia, no crea otra.
        assertTrue(
            carpeta.listFiles()!!.count { it.name.startsWith("virtualpet-antes-de-migrar") } == 1,
            "solo puede quedar una copia por versión de origen"
        )
    }

    @Test
    fun `una base al dia no genera copias ni toca nada`() {
        crearBaseConUnaMascota(VirtualPetDatabase.Schema.version)

        val driver = crearDriverJdbc(archivo)
        try {
            val db = abrirBaseDeDatos(driver, archivo = archivo)
            assertEquals(1, db.mascotasQueries.selectAll().executeAsList().size)
        } finally {
            driver.close()
        }

        assertTrue(
            carpeta.listFiles()!!.none { it.name.startsWith("virtualpet-antes-de-migrar") },
            "no se debe copiar nada si no hay migración"
        )
    }
}
