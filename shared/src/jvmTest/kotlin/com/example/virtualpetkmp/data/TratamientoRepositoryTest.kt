package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TratamientoRepositoryTest {

    private lateinit var repository: TratamientoRepository

    private fun tratamientoDePrueba(mascotaId: Long = 1L, nombre: String = "Antibiótico") = Tratamiento(
        id = null,
        mascotaId = mascotaId,
        nombreMedicamento = nombre,
        dosis = "500mg",
        frecuencia = "Cada 8 horas",
        fechaInicio = LocalDate(2020, 1, 15),
        fechaFin = LocalDate(2020, 1, 22)
    )

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = TratamientoRepository(database)
    }

    @Test
    fun `insertar un tratamiento permite recuperarlo luego`() = runBlocking {
        val resultado = repository.insertTratamiento(
            mascotaId = 1L,
            nombreMedicamento = "Antibiótico",
            dosis = "500mg",
            frecuencia = "Cada 8 horas",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = LocalDate(2020, 1, 22)
        )
        assertTrue(resultado.isSuccess)

        val todos = repository.getTratamientosByMascotaId(1L)
        assertEquals(1, todos.size)
        assertEquals("Antibiótico", todos.first().nombreMedicamento)
    }

    @Test
    fun `getTratamientosByMascotaId devuelve solo los tratamientos de esa mascota`() = runBlocking {
        repository.insertTratamiento(
            mascotaId = 1L,
            nombreMedicamento = "Antibiótico",
            dosis = "500mg",
            frecuencia = "Cada 8 horas",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = LocalDate(2020, 1, 22)
        )
        repository.insertTratamiento(
            mascotaId = 2L,
            nombreMedicamento = "Antiinflamatorio",
            dosis = "200mg",
            frecuencia = "Cada 12 horas",
            fechaInicio = LocalDate(2020, 2, 15),
            fechaFin = LocalDate(2020, 2, 22)
        )

        val tratamientosMascota1 = repository.getTratamientosByMascotaId(1L)
        assertEquals(1, tratamientosMascota1.size)
        assertEquals("Antibiótico", tratamientosMascota1.first().nombreMedicamento)
    }

    @Test
    fun `actualizar un tratamiento cambia sus datos`() = runBlocking {
        repository.insertTratamiento(
            mascotaId = 1L,
            nombreMedicamento = "Antibiótico",
            dosis = "500mg",
            frecuencia = "Cada 8 horas",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = LocalDate(2020, 1, 22)
        )

        val todos = repository.getTratamientosByMascotaId(1L)
        val id = todos.first().id!!

        val resultado = repository.updateTratamiento(
            id = id,
            nombreMedicamento = "Antibiótico actualizado",
            dosis = "1000mg",
            frecuencia = "Cada 6 horas",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = LocalDate(2020, 1, 29)
        )

        assertTrue(resultado.isSuccess)
        val recargado = repository.getTratamientosByMascotaId(1L)
        assertEquals("Antibiótico actualizado", recargado.first().nombreMedicamento)
    }

    @Test
    fun `eliminar un tratamiento hace que ya no aparezca`() = runBlocking {
        repository.insertTratamiento(
            mascotaId = 1L,
            nombreMedicamento = "Antibiótico",
            dosis = "500mg",
            frecuencia = "Cada 8 horas",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = LocalDate(2020, 1, 22)
        )

        val todos = repository.getTratamientosByMascotaId(1L)
        val id = todos.first().id!!

        repository.deleteTratamiento(id)

        val tratamientosRestantes = repository.getTratamientosByMascotaId(1L)
        assertTrue(tratamientosRestantes.isEmpty())
    }

    @Test
    fun `tratamiento cronico sin fecha fin`() = runBlocking {
        val resultado = repository.insertTratamiento(
            mascotaId = 1L,
            nombreMedicamento = "Medicamento crónico",
            dosis = "100mg",
            frecuencia = "Diario",
            fechaInicio = LocalDate(2020, 1, 15),
            fechaFin = null
        )
        assertTrue(resultado.isSuccess)

        val todos = repository.getTratamientosByMascotaId(1L)
        assertEquals(1, todos.size)
        assertEquals(null, todos.first().fechaFin)
    }
}
