package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import app.cash.sqldelight.db.QueryResult

/**
 * La revisión de versión que hace el arranque de escritorio **antes** de abrir la ventana.
 *
 * Se prueba aquí y no en `desktopApp` porque la función vive en `shared` (necesita los recursos de
 * los seis idiomas) y así se comprueba sin abrir una ventana.
 */
class RevisarVersionBaseTest {

    private lateinit var carpeta: File
    private lateinit var archivo: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-revisar-version").toFile()
        archivo = File(carpeta, "virtualpet.db")
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    /** Deja un fichero con el esquema actual y la versión que se le pida. */
    private fun crearBaseConVersion(version: Long) {
        val driver = crearDriverJdbc(archivo)
        try {
            VirtualPetDatabase.Schema.create(driver).value
            driver.execute(null, "PRAGMA user_version = $version;", 0)
        } finally {
            driver.close()
        }
    }

    @Test
    fun `una base al dia no da problema`() {
        crearBaseConVersion(VirtualPetDatabase.Schema.version)

        assertNull(revisarVersionDeLaBaseDeDatos(archivo))
    }

    @Test
    fun `una base mas nueva devuelve las dos versiones`() {
        val versionDelFichero = VirtualPetDatabase.Schema.version + 5
        crearBaseConVersion(versionDelFichero)

        val problema = revisarVersionDeLaBaseDeDatos(archivo)

        assertNotNull(problema, "una base más nueva tiene que detectarse")
        assertEquals(versionDelFichero, problema.versionFichero)
        assertEquals(VirtualPetDatabase.Schema.version, problema.versionApp)
    }

    @Test
    fun `si no hay fichero no hay nada que revisar`() {
        assertNull(revisarVersionDeLaBaseDeDatos(File(carpeta, "no-existe.db")))
    }

    @Test
    fun `la revision no cambia la base`() {
        val versionDelFichero = VirtualPetDatabase.Schema.version + 5
        crearBaseConVersion(versionDelFichero)

        revisarVersionDeLaBaseDeDatos(archivo)

        val driver = crearDriverJdbc(archivo)
        try {
            val version = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version;",
                mapper = { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                },
                parameters = 0
            ).value
            assertEquals(versionDelFichero, version, "revisar la versión no puede tocar el fichero")
        } finally {
            driver.close()
        }
    }
}
