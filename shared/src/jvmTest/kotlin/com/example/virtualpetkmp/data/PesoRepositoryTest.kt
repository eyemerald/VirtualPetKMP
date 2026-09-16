package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PesoRepositoryTest {

    private lateinit var repository: PesoRepository

    private fun pesoDePrueba(mascotaId: Long = 1L, peso: Double = 25.5) = Peso(
        id = null,
        mascotaId = mascotaId,
        fecha = LocalDate(2020, 1, 15),
        peso = peso,
        notas = "Pesado en casa"
    )

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = PesoRepository(database)
    }

    @Test
    fun `insertar un peso permite recuperarlo luego`() = runBlocking {
        val resultado = repository.insertPeso(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            peso = 25.5,
            notas = "Pesado en casa"
        )
        assertTrue(resultado.isSuccess)

        val todos = repository.getPesosByMascotaId(1L)
        assertEquals(1, todos.size)
        assertEquals(25.5, todos.first().peso)
    }

    @Test
    fun `getPesosByMascotaId devuelve solo los pesos de esa mascota`() = runBlocking {
        repository.insertPeso(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            peso = 25.5,
            notas = "Pesado en casa"
        )
        repository.insertPeso(
            mascotaId = 2L,
            fecha = LocalDate(2020, 2, 15),
            peso = 30.0,
            notas = "Pesado en veterinario"
        )

        val pesosMascota1 = repository.getPesosByMascotaId(1L)
        assertEquals(1, pesosMascota1.size)
        assertEquals(25.5, pesosMascota1.first().peso)
    }

    @Test
    fun `actualizar un peso cambia sus datos`() = runBlocking {
        repository.insertPeso(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            peso = 25.5,
            notas = "Pesado en casa"
        )

        val todos = repository.getPesosByMascotaId(1L)
        val id = todos.first().id!!

        val resultado = repository.updatePeso(
            id = id,
            fecha = LocalDate(2020, 1, 15),
            peso = 26.0,
            notas = "Pesado en veterinario"
        )

        assertTrue(resultado.isSuccess)
        val recargado = repository.getPesosByMascotaId(1L)
        assertEquals(26.0, recargado.first().peso)
    }

    @Test
    fun `eliminar un peso hace que ya no aparezca`() = runBlocking {
        repository.insertPeso(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            peso = 25.5,
            notas = "Pesado en casa"
        )

        val todos = repository.getPesosByMascotaId(1L)
        val id = todos.first().id!!

        repository.deletePeso(id)

        val pesosRestantes = repository.getPesosByMascotaId(1L)
        assertTrue(pesosRestantes.isEmpty())
    }

    @Test
    fun `peso sin notas`() = runBlocking {
        val resultado = repository.insertPeso(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            peso = 25.5,
            notas = null
        )
        assertTrue(resultado.isSuccess)

        val todos = repository.getPesosByMascotaId(1L)
        assertEquals(1, todos.size)
        assertEquals(null, todos.first().notas)
    }
}
