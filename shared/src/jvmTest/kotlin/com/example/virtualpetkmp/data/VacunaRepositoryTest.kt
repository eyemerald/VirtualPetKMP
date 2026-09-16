package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VacunaRepositoryTest {

    private lateinit var repository: VacunaRepository

    private fun vacunaDePrueba(mascotaId: Long = 1L, nombre: String = "Rabia") = Vacuna(
        id = null,
        mascotaId = mascotaId,
        nombre = nombre,
        fechaAplicacion = LocalDate(2020, 1, 15),
        fechaProximaDosis = LocalDate(2021, 1, 15),
        veterinario = "Dr. Smith",
        lote = "ABC123"
    )

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = VacunaRepository(database)
    }

    @Test
    fun `insertar una vacuna permite recuperarla luego`() = runBlocking {
        val resultado = repository.insertVacuna(
            mascotaId = 1L,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2020, 1, 15),
            fechaProximaDosis = LocalDate(2021, 1, 15),
            veterinario = "Dr. Smith",
            lote = "ABC123"
        )
        assertTrue(resultado.isSuccess)

        val todas = repository.getVacunasByMascotaId(1L)
        assertEquals(1, todas.size)
        assertEquals("Rabia", todas.first().nombre)
    }

    @Test
    fun `getVacunasByMascotaId devuelve solo las vacunas de esa mascota`() = runBlocking {
        repository.insertVacuna(
            mascotaId = 1L,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2020, 1, 15),
            fechaProximaDosis = LocalDate(2021, 1, 15),
            veterinario = "Dr. Smith",
            lote = "ABC123"
        )
        repository.insertVacuna(
            mascotaId = 2L,
            nombre = "Parvovirus",
            fechaAplicacion = LocalDate(2020, 2, 15),
            fechaProximaDosis = LocalDate(2021, 2, 15),
            veterinario = "Dr. Jones",
            lote = "DEF456"
        )

        val vacunasMascota1 = repository.getVacunasByMascotaId(1L)
        assertEquals(1, vacunasMascota1.size)
        assertEquals("Rabia", vacunasMascota1.first().nombre)
    }

    @Test
    fun `actualizar una vacuna cambia sus datos`() = runBlocking {
        val insertResult = repository.insertVacuna(
            mascotaId = 1L,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2020, 1, 15),
            fechaProximaDosis = LocalDate(2021, 1, 15),
            veterinario = "Dr. Smith",
            lote = "ABC123"
        )
        // Necesitamos obtener el ID insertado, pero el repositorio devuelve Result<Unit>
        // Para este test, asumimos que podemos obtener el ID de alguna forma
        // En la práctica, podríamos necesitar modificar el repositorio para devolver el ID
        
        val todas = repository.getVacunasByMascotaId(1L)
        val id = todas.first().id!!

        val resultado = repository.updateVacuna(
            id = id,
            nombre = "Rabia Actualizada",
            fechaAplicacion = LocalDate(2020, 1, 15),
            fechaProximaDosis = LocalDate(2022, 1, 15),
            veterinario = "Dr. Johnson",
            lote = "XYZ789"
        )

        assertTrue(resultado.isSuccess)
        val recargada = repository.getVacunasByMascotaId(1L)
        assertEquals("Rabia Actualizada", recargada.first().nombre)
    }

    @Test
    fun `eliminar una vacuna hace que ya no aparezca`() = runBlocking {
        val insertResult = repository.insertVacuna(
            mascotaId = 1L,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2020, 1, 15),
            fechaProximaDosis = LocalDate(2021, 1, 15),
            veterinario = "Dr. Smith",
            lote = "ABC123"
        )

        val todas = repository.getVacunasByMascotaId(1L)
        val id = todas.first().id!!

        repository.deleteVacuna(id)

        val vacunasRestantes = repository.getVacunasByMascotaId(1L)
        assertTrue(vacunasRestantes.isEmpty())
    }
}
