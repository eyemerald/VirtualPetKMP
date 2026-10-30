package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El `id` que devuelve un alta tiene que ser el de la fila que se acaba de crear.
 *
 * **Por qué no lo cazaban los tests anteriores.** Todos usaban una base **en memoria**, y en ese
 * modo sqldelight reutiliza una sola conexión: `SELECT last_insert_rowid()` devuelve el valor
 * correcto. Sobre un **fichero**, en cambio, cada sentencia va por una conexión distinta (ver
 * `CascadaEscritorioTest`), así que `last_insert_rowid()` se ejecutaba en una conexión donde no se
 * había insertado nada y devolvía **0**.
 *
 * Con el id a 0, cualquier cosa que dependa de él se rompe en silencio: por ejemplo
 * `FichaMascotaViewModel` programa el aviso de una vacuna con el id del alta
 * (`FichaMascotaViewModel.kt:416`), así que el aviso se programaba con id 0.
 *
 * El arreglo es que el `insert` y la lectura del id vayan **en la misma transacción**, que fija la
 * conexión del hilo.
 */
class IdInsertadoTest {

    private lateinit var carpeta: File
    private lateinit var archivo: File
    private lateinit var driver: SqlDriver
    private lateinit var db: VirtualPetDatabase

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-id").toFile()
        archivo = File(carpeta, "virtualpet.db")
        driver = crearDriverJdbc(archivo)
        db = abrirBaseDeDatos(driver, archivo = archivo)
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

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

    private fun mascotaDePrueba() = Mascota(
        nombre = "Luna",
        especie = "Perro",
        raza = "Labrador Retriever",
        fechaNacimiento = LocalDate(2024, 2, 4),
        sexo = "Hembra",
        color = "Negro",
        microchip = null
    )

    @Test
    fun `insertMascota devuelve el id de la mascota creada`() = runBlocking {
        val id = MascotaRepository(db).insertMascota(mascotaDePrueba()).getOrThrow()

        assertTrue(id > 0, "el id de un alta no puede ser 0 (fue $id)")
        assertEquals(
            "Luna",
            db.mascotasQueries.selectById(id).executeAsOne().nombre,
            "el id devuelto no apunta a la mascota insertada"
        )
    }

    @Test
    fun `insertVacuna devuelve el id de la vacuna creada`() = runBlocking {
        val mascotaId = MascotaRepository(db).insertMascota(mascotaDePrueba()).getOrThrow()

        val id = VacunaRepository(db).insertVacuna(
            mascotaId = mascotaId,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2026, 1, 10),
            fechaProximaDosis = LocalDate(2027, 1, 10),
            veterinario = "Dra. Ruiz",
            lote = "L-1"
        ).getOrThrow()

        assertTrue(id > 0, "el id de la vacuna no puede ser 0 (fue $id)")
        val vacuna = db.vacunasQueries.selectAll().executeAsList().single { it.id == id }
        assertEquals("Rabia", vacuna.nombre)
        assertEquals(mascotaId, vacuna.mascotaId, "la vacuna quedó colgando de otra mascota")
    }

    @Test
    fun `insertPreventivo devuelve el id del preventivo creado`() = runBlocking {
        val mascotaId = MascotaRepository(db).insertMascota(mascotaDePrueba()).getOrThrow()

        val id = PreventivoRepository(db).insertPreventivo(
            mascotaId = mascotaId,
            tipo = "Pipeta",
            nombre = "Frontline",
            fechaAplicacion = LocalDate(2026, 2, 1),
            fechaProximaDosis = LocalDate(2026, 3, 1),
            veterinario = null,
            lote = null
        ).getOrThrow()

        assertTrue(id > 0, "el id del preventivo no puede ser 0 (fue $id)")
        val preventivo = db.preventivosQueries.selectAll().executeAsList().single { it.id == id }
        assertEquals("Frontline", preventivo.nombre)
        assertEquals(mascotaId, preventivo.mascotaId)
    }

    @Test
    fun `insertVeterinario devuelve el id del veterinario creado`() = runBlocking {
        val id = VeterinarioRepository(db).insertVeterinario(
            Veterinario(
                nombreClinica = "Clínica Central",
                nombreVeterinario = "Dra. Ruiz",
                telefono = "600000000",
                direccion = "Calle Mayor 1",
                esUrgencias = false
            )
        ).getOrThrow()

        assertTrue(id > 0, "el id del veterinario no puede ser 0 (fue $id)")
        val veterinario = db.veterinariosQueries.selectById(id).executeAsOne()
        assertEquals("Clínica Central", veterinario.nombreClinica)
    }
}
