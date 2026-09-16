package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevisionRepositoryTest {

    private lateinit var repository: RevisionRepository

    private fun revisionDePrueba(mascotaId: Long = 1L, motivo: String = "Chequeo anual") = Revision(
        id = null,
        mascotaId = mascotaId,
        fecha = LocalDate(2020, 1, 15),
        motivo = motivo,
        diagnostico = "Sano",
        notas = "Todo bien",
        veterinario = "Dr. Smith"
    )

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = RevisionRepository(database)
    }

    @Test
    fun `insertar una revision permite recuperarla luego`() = runBlocking {
        val resultado = repository.insertRevision(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            motivo = "Chequeo anual",
            diagnostico = "Sano",
            notas = "Todo bien",
            veterinario = "Dr. Smith"
        )
        assertTrue(resultado.isSuccess)

        val todas = repository.getRevisionesByMascotaId(1L)
        assertEquals(1, todas.size)
        assertEquals("Chequeo anual", todas.first().motivo)
    }

    @Test
    fun `getRevisionesByMascotaId devuelve solo las revisiones de esa mascota`() = runBlocking {
        repository.insertRevision(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            motivo = "Chequeo anual",
            diagnostico = "Sano",
            notas = "Todo bien",
            veterinario = "Dr. Smith"
        )
        repository.insertRevision(
            mascotaId = 2L,
            fecha = LocalDate(2020, 2, 15),
            motivo = "Vacunación",
            diagnostico = "Reacción leve",
            notas = "Observar",
            veterinario = "Dr. Jones"
        )

        val revisionesMascota1 = repository.getRevisionesByMascotaId(1L)
        assertEquals(1, revisionesMascota1.size)
        assertEquals("Chequeo anual", revisionesMascota1.first().motivo)
    }

    @Test
    fun `actualizar una revision cambia sus datos`() = runBlocking {
        repository.insertRevision(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            motivo = "Chequeo anual",
            diagnostico = "Sano",
            notas = "Todo bien",
            veterinario = "Dr. Smith"
        )

        val todas = repository.getRevisionesByMascotaId(1L)
        val id = todas.first().id!!

        val resultado = repository.updateRevision(
            id = id,
            fecha = LocalDate(2020, 1, 15),
            motivo = "Chequeo anual actualizado",
            diagnostico = "Muy sano",
            notas = "Excelente estado",
            veterinario = "Dr. Johnson"
        )

        assertTrue(resultado.isSuccess)
        val recargada = repository.getRevisionesByMascotaId(1L)
        assertEquals("Chequeo anual actualizado", recargada.first().motivo)
    }

    @Test
    fun `eliminar una revision hace que ya no aparezca`() = runBlocking {
        repository.insertRevision(
            mascotaId = 1L,
            fecha = LocalDate(2020, 1, 15),
            motivo = "Chequeo anual",
            diagnostico = "Sano",
            notas = "Todo bien",
            veterinario = "Dr. Smith"
        )

        val todas = repository.getRevisionesByMascotaId(1L)
        val id = todas.first().id!!

        repository.deleteRevision(id)

        val revisionesRestantes = repository.getRevisionesByMascotaId(1L)
        assertTrue(revisionesRestantes.isEmpty())
    }
}
