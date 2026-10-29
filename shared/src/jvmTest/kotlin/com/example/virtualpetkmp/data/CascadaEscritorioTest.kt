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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El `ON DELETE CASCADE` tiene que funcionar **en la app de escritorio de verdad**.
 *
 * Por qué existe este test: el `PRAGMA foreign_keys` es una propiedad **de cada conexión**, y
 * `JdbcSqliteDriver` sobre un fichero abre conexiones nuevas constantemente (su
 * `ThreadedConnectionManager.closeConnection` cierra la conexión y la olvida cuando la sentencia no
 * va dentro de una transacción). El `PRAGMA` que se ejecutaba una vez al abrir la base solo valía
 * para esa conexión: los borrados que hacen los repositorios desde `Dispatchers.IO` iban con las
 * claves foráneas **apagadas** y dejaban filas huérfanas.
 *
 * Los tests anteriores no lo cazaban porque usaban una base **en memoria** (con `IN_MEMORY`
 * sqldelight reutiliza una sola conexión) y porque `MascotaRepositoryTest` solo comprueba que la
 * mascota desaparece, sin mirar las tablas hijas.
 *
 * Aquí la base es un **fichero**, se abre con `crearDriverJdbc` (la misma configuración que en
 * producción) y se borra a través del repositorio, igual que desde la interfaz.
 */
class CascadaEscritorioTest {

    private lateinit var carpeta: File
    private lateinit var archivo: File
    private lateinit var driver: SqlDriver
    private lateinit var db: VirtualPetDatabase

    private val tablasHijas = listOf(
        "vacunas", "preventivos", "revisiones", "tratamientos", "pesos", "informes", "notas"
    )

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-cascada").toFile()
        archivo = File(carpeta, "virtualpet.db")
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    /** Abre la base como la abre la app de escritorio. */
    private fun abrirComoLaApp(): SqlDriver {
        driver = crearDriverJdbc(archivo)
        db = abrirBaseDeDatos(driver, archivo = archivo)
        return driver
    }

    private fun SqlDriver.ejecutar(sql: String) = execute(null, sql, 0)

    private fun SqlDriver.consultarUnNumero(sql: String): Long =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: -1L)
            },
            parameters = 0
        ).value

    /** Inserta una fila en cada una de las 7 tablas que cuelgan de `mascotas`. */
    private fun SqlDriver.llenarTablasHijas(mascotaId: Long) {
        ejecutar(
            "INSERT INTO vacunas (mascotaId, nombre, fechaAplicacion, fechaProximaDosis, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, 'Rabia', '2026-01-10', '2027-01-10', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 1)"
        )
        ejecutar(
            "INSERT INTO preventivos (mascotaId, tipo, nombre, fechaAplicacion, fechaProximaDosis, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, 'Pipeta', 'Frontline', '2026-02-01', '2026-03-01', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 1)"
        )
        ejecutar(
            "INSERT INTO revisiones (mascotaId, fecha, motivo, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, '2026-03-01', 'Revisión anual', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 1)"
        )
        ejecutar(
            "INSERT INTO tratamientos (mascotaId, nombreMedicamento, fechaInicio, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, 'Antibiótico', '2026-04-01', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 1)"
        )
        ejecutar(
            "INSERT INTO pesos (mascotaId, fecha, peso, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, '2026-05-01', 4.7, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5', 1)"
        )
        ejecutar(
            "INSERT INTO informes (mascotaId, tipo, fecha, nombreArchivo, rutaArchivo, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, 'Analítica', '2026-06-01', 'a.pdf', '/tmp/a.pdf', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa6', 1)"
        )
        ejecutar(
            "INSERT INTO notas (mascotaId, texto, uuid, actualizadoEn) " +
                "VALUES ($mascotaId, 'Alérgica a las pulgas', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7', 1)"
        )
    }

    private fun SqlDriver.filasEn(tabla: String, mascotaId: Long): Long =
        consultarUnNumero("SELECT count(*) FROM $tabla WHERE mascotaId = $mascotaId;")

    private fun SqlDriver.huerfanas(): Long = consultarUnNumero("SELECT count(*) FROM pragma_foreign_key_check;")

    @Test
    fun `borrar una mascota con datos no deja huerfanas en ninguna de las 7 tablas`() = runBlocking {
        abrirComoLaApp()
        val repositorio = MascotaRepository(db)

        repositorio.insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Labrador Retriever",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = null
            )
        ).getOrThrow()

        // El id se lee de la base y no del `Result` del alta: ver la nota de más abajo.
        val id = driver.consultarUnNumero("SELECT id FROM mascotas ORDER BY id DESC LIMIT 1;")
        driver.llenarTablasHijas(id)
        assertTrue(
            tablasHijas.all { driver.filasEn(it, id) == 1L },
            "el montaje del test está mal: faltan filas hijas"
        )

        // El borrado va por el repositorio: corre en Dispatchers.IO, no en el hilo que abrió la base.
        repositorio.deleteMascota(id).getOrThrow()

        assertEquals(0, db.mascotasQueries.selectAll().executeAsList().size, "la mascota no se borró")

        val quedan = tablasHijas.filter { driver.filasEn(it, id) > 0 }
        assertEquals(
            emptyList(),
            quedan,
            "quedaron filas huérfanas en $quedan: el ON DELETE CASCADE no se aplicó"
        )
        assertEquals(0, driver.huerfanas(), "PRAGMA foreign_key_check sigue encontrando huérfanas")
    }

    @Test
    fun `en un hilo distinto las claves foraneas siguen activas`() {
        abrirComoLaApp()
        var fallo: Throwable? = null

        val hilo = Thread {
            // Una fila hija sin padre: si las claves foráneas están activas, SQLite la rechaza.
            fallo = try {
                driver.ejecutar(
                    "INSERT INTO notas (mascotaId, texto, uuid, actualizadoEn) " +
                        "VALUES (9999, 'sin padre', 'ccccccccccccccccccccccccccccccc1', 1)"
                )
                null
            } catch (e: Exception) {
                e
            }
        }
        hilo.start()
        hilo.join()

        assertTrue(
            fallo != null,
            "en un hilo de trabajo se pudo insertar una fila huérfana: las claves foráneas están apagadas"
        )
        assertEquals(0, driver.consultarUnNumero("SELECT count(*) FROM notas;"), "la fila huérfana se guardó")
    }

    @Test
    fun `el fichero de la base existe y no queda vacio`() {
        abrirComoLaApp()

        assertTrue(archivo.exists(), "la base de datos no se creó en el fichero")
        assertTrue(archivo.length() > 0, "el fichero de la base está vacío")
    }

    // --- Limpieza única de las huérfanas que ya había en el fichero -----------------------------

    /**
     * Deja en el fichero filas huérfanas *como las dejaría la versión anterior de la app*: se abre
     * un driver **sin** las propiedades (claves foráneas apagadas), se rellena y se borra la
     * mascota con SQL suelto, que es lo que hacía el `DELETE` del repositorio.
     */
    private fun crearBaseConHuerfanas(): Long {
        // Driver SIN las propiedades (como la versión anterior de la app) y abierto con
        // `abrirBaseDeDatos` para que el fichero quede con su `user_version` marcada.
        val sinClavesForaneas = JdbcSqliteDriver(url = "jdbc:sqlite:${archivo.absolutePath}")
        try {
            abrirBaseDeDatos(sinClavesForaneas, archivo = archivo)
            sinClavesForaneas.ejecutar(
                "INSERT INTO mascotas (nombre, especie, raza, fechaNacimiento, sexo, color, uuid, actualizadoEn) " +
                    "VALUES ('Luna', 'Perro', 'Labrador', '2024-02-04', 'Hembra', 'Negro', " +
                    "'ddddddddddddddddddddddddddddddd1', 1)"
            )
            val id = sinClavesForaneas.consultarUnNumero("SELECT id FROM mascotas LIMIT 1;")
            sinClavesForaneas.llenarTablasHijas(id)
            sinClavesForaneas.ejecutar("DELETE FROM mascotas WHERE id = $id;")
            assertEquals(7, sinClavesForaneas.huerfanas(), "el montaje no dejó las 7 huérfanas")
            return id
        } finally {
            sinClavesForaneas.close()
        }
    }

    @Test
    fun `al abrir se limpian las huerfanas que ya habia y se deja copia previa`() {
        crearBaseConHuerfanas()

        abrirComoLaApp()

        assertEquals(0, driver.huerfanas(), "la limpieza no dejó la base sin huérfanas")
        tablasHijas.forEach { tabla ->
            assertEquals(0, driver.consultarUnNumero("SELECT count(*) FROM $tabla;"), "quedan filas en $tabla")
        }

        val copia = File(carpeta, COPIA_ANTES_DE_LIMPIAR)
        assertTrue(copia.exists(), "no se dejó la copia previa a la limpieza")
        assertTrue(copia.length() > 0, "la copia previa está vacía")

        // Y la copia conserva las huérfanas: es la red de seguridad para volver atrás.
        val copiaDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${copia.absolutePath}")
        try {
            assertEquals(7, copiaDriver.huerfanas(), "la copia no tiene las filas de antes de limpiar")
        } finally {
            copiaDriver.close()
        }
    }

    @Test
    fun `sin huerfanas no se crea ninguna copia`() {
        abrirComoLaApp()

        assertFalse(
            File(carpeta, COPIA_ANTES_DE_LIMPIAR).exists(),
            "se creó una copia sin haber nada que limpiar"
        )
    }
}
