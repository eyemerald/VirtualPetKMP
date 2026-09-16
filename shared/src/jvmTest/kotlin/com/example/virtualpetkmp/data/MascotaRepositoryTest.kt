package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MascotaRepositoryTest {

    private lateinit var repository: MascotaRepository

    private fun mascotaDePrueba(nombre: String = "Rex") = Mascota(
        nombre = nombre,
        especie = "Perro",
        raza = "Labrador",
        fechaNacimiento = LocalDate(2020, 1, 15),
        sexo = "Macho",
        color = "Marrón",
        microchip = null
    )

    @BeforeTest
    fun setup() {
        // Base de datos en memoria: se crea limpia antes de cada test
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = MascotaRepository(database)
    }

    @Test
    fun `insertar una mascota permite recuperarla luego`() = runBlocking {
        val resultado = repository.insertMascota(mascotaDePrueba())
        assertTrue(resultado.isSuccess)

        val todas = repository.getAllMascotas()
        assertEquals(1, todas.size)
        assertEquals("Rex", todas.first().nombre)
    }

    @Test
    fun `no se puede insertar dos mascotas con el mismo nombre`() = runBlocking {
        repository.insertMascota(mascotaDePrueba("Rex"))
        val segundoIntento = repository.insertMascota(mascotaDePrueba("Rex"))

        assertTrue(segundoIntento.isFailure)
    }

    @Test
    fun `el nombre duplicado no distingue mayusculas de minusculas`() = runBlocking {
        repository.insertMascota(mascotaDePrueba("Rex"))
        val segundoIntento = repository.insertMascota(mascotaDePrueba("REX"))

        assertTrue(segundoIntento.isFailure)
    }

    @Test
    fun `getMascotaById devuelve la mascota correcta`() = runBlocking {
        val insertResult = repository.insertMascota(mascotaDePrueba())
        val id = insertResult.getOrThrow()

        val mascota = repository.getMascotaById(id)
        assertNotNull(mascota)
        assertEquals("Rex", mascota.nombre)
    }

    @Test
    fun `getMascotaById devuelve null si no existe`() = runBlocking {
        val mascota = repository.getMascotaById(999L)
        assertNull(mascota)
    }

    @Test
    fun `actualizar una mascota cambia sus datos`() = runBlocking {
        val id = repository.insertMascota(mascotaDePrueba()).getOrThrow()

        val actualizada = mascotaDePrueba().copy(id = id, color = "Negro")
        val resultado = repository.updateMascota(actualizada)

        assertTrue(resultado.isSuccess)
        val recargada = repository.getMascotaById(id)
        assertEquals("Negro", recargada?.color)
    }

    @Test
    fun `actualizar sin id falla`() = runBlocking {
        val resultado = repository.updateMascota(mascotaDePrueba())
        assertTrue(resultado.isFailure)
    }

    @Test
    fun `eliminar una mascota hace que ya no aparezca`() = runBlocking {
        val id = repository.insertMascota(mascotaDePrueba()).getOrThrow()

        repository.deleteMascota(id)

        val todas = repository.getAllMascotas()
        assertTrue(todas.isEmpty())
    }
}
