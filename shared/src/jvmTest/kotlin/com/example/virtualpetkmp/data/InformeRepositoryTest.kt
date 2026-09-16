package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InformeRepositoryTest {

    private lateinit var repository: InformeRepository

    private fun informeDePrueba(mascotaId: Long = 1L, tipo: String = "Analítica") = Informe(
        id = null,
        mascotaId = mascotaId,
        tipo = tipo,
        descripcion = "Resultados normales",
        fecha = LocalDate(2020, 1, 15),
        nombreArchivo = "analitica_2020.pdf",
        rutaArchivo = "/documentos/analitica_2020.pdf"
    )

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val database = VirtualPetDatabase(driver)
        repository = InformeRepository(database)
    }

    @Test
    fun `insertar un informe permite recuperarlo luego`() = runBlocking {
        val resultado = repository.insertInforme(
            mascotaId = 1L,
            tipo = "Analítica",
            descripcion = "Resultados normales",
            fecha = LocalDate(2020, 1, 15),
            nombreArchivo = "analitica_2020.pdf",
            rutaArchivo = "/documentos/analitica_2020.pdf"
        )
        assertTrue(resultado.isSuccess)

        val todos = repository.getInformesByMascotaId(1L)
        assertEquals(1, todos.size)
        assertEquals("Analítica", todos.first().tipo)
    }

    @Test
    fun `getInformesByMascotaId devuelve solo los informes de esa mascota`() = runBlocking {
        repository.insertInforme(
            mascotaId = 1L,
            tipo = "Analítica",
            descripcion = "Resultados normales",
            fecha = LocalDate(2020, 1, 15),
            nombreArchivo = "analitica_2020.pdf",
            rutaArchivo = "/documentos/analitica_2020.pdf"
        )
        repository.insertInforme(
            mascotaId = 2L,
            tipo = "Radiografía",
            descripcion = "Sin anomalías",
            fecha = LocalDate(2020, 2, 15),
            nombreArchivo = "radiografia_2020.jpg",
            rutaArchivo = "/documentos/radiografia_2020.jpg"
        )

        val informesMascota1 = repository.getInformesByMascotaId(1L)
        assertEquals(1, informesMascota1.size)
        assertEquals("Analítica", informesMascota1.first().tipo)
    }

    @Test
    fun `actualizar un informe cambia sus datos`() = runBlocking {
        repository.insertInforme(
            mascotaId = 1L,
            tipo = "Analítica",
            descripcion = "Resultados normales",
            fecha = LocalDate(2020, 1, 15),
            nombreArchivo = "analitica_2020.pdf",
            rutaArchivo = "/documentos/analitica_2020.pdf"
        )

        val todos = repository.getInformesByMascotaId(1L)
        val id = todos.first().id!!

        val resultado = repository.updateInforme(
            id = id,
            tipo = "Analítica actualizada",
            descripcion = "Resultados excelentes",
            fecha = LocalDate(2020, 1, 15)
        )

        assertTrue(resultado.isSuccess)
        val recargado = repository.getInformesByMascotaId(1L)
        assertEquals("Analítica actualizada", recargado.first().tipo)
    }

    @Test
    fun `eliminar un informe hace que ya no aparezca`() = runBlocking {
        repository.insertInforme(
            mascotaId = 1L,
            tipo = "Analítica",
            descripcion = "Resultados normales",
            fecha = LocalDate(2020, 1, 15),
            nombreArchivo = "analitica_2020.pdf",
            rutaArchivo = "/documentos/analitica_2020.pdf"
        )

        val todos = repository.getInformesByMascotaId(1L)
        val id = todos.first().id!!

        repository.deleteInforme(id)

        val informesRestantes = repository.getInformesByMascotaId(1L)
        assertTrue(informesRestantes.isEmpty())
    }
}
