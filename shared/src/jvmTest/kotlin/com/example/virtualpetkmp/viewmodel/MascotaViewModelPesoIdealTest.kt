package com.example.virtualpetkmp.viewmodel

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * El peso ideal tiene que llegar a la interfaz **solo**, sin reiniciar la app.
 *
 * Es la queja que trajo el usuario: al cambiarlo desde la hoja de peso, el gráfico seguía con el
 * valor viejo. La cadena que lo hace posible es repositorio → ViewModel → `StateFlow` → pantalla;
 * aquí se comprueba el trozo que puede romperse en silencio, que es el del ViewModel: si tras
 * escribir deja de recargar la lista, la pantalla no tiene forma de enterarse.
 *
 * Se construye el ViewModel con `Dispatchers.Unconfined` porque `Dispatchers.Main` no existe en
 * jvmTest; el resto es el código de producción, con la base de datos de verdad en memoria.
 */
class MascotaViewModelPesoIdealTest {

    private fun bdVacia(): VirtualPetDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        return VirtualPetDatabase(driver)
    }

    private fun nuevoViewModel(db: VirtualPetDatabase) = MascotaViewModel(
        repository = MascotaRepository(db),
        pesoRepository = PesoRepository(db),
        dispatcher = Dispatchers.Unconfined
    )

    private suspend fun insertarMascota(db: VirtualPetDatabase, pesoIdeal: Double? = null): Long =
        MascotaRepository(db).insertMascota(
            Mascota(
                nombre = "Luna",
                especie = "Perro",
                raza = "Labrador Retriever",
                fechaNacimiento = LocalDate(2024, 2, 4),
                sexo = "Hembra",
                color = "Negro",
                microchip = null,
                pesoIdeal = pesoIdeal
            )
        ).getOrThrow()

    @Test
    fun `fijar el peso ideal se refleja en el estado sin recargar la app`() = runBlocking {
        val db = bdVacia()
        val id = insertarMascota(db)
        val viewModel = nuevoViewModel(db)

        // Se espera a la carga inicial, que hace el ViewModel en su `init`.
        assertEquals(1, viewModel.mascotas.first { it.isNotEmpty() }.size)
        assertNull(viewModel.mascotas.value.first().pesoIdeal)

        viewModel.updatePesoIdeal(id, 26.4)

        val actualizada = withTimeout(5_000) {
            viewModel.mascotas.first { it.first().pesoIdeal == 26.4 }
        }
        assertEquals(26.4, actualizada.first().pesoIdeal)
    }

    @Test
    fun `cambiar el peso ideal por otro valor tambien llega al estado`() = runBlocking {
        val db = bdVacia()
        val id = insertarMascota(db, pesoIdeal = 20.0)
        val viewModel = nuevoViewModel(db)

        viewModel.mascotas.first { it.isNotEmpty() }
        assertEquals(20.0, viewModel.mascotas.value.first().pesoIdeal)

        viewModel.updatePesoIdeal(id, 24.5)

        val actualizada = withTimeout(5_000) {
            viewModel.mascotas.first { it.first().pesoIdeal == 24.5 }
        }
        assertEquals(24.5, actualizada.first().pesoIdeal)
    }

    @Test
    fun `quitar el peso ideal deja el estado en null`() = runBlocking {
        val db = bdVacia()
        val id = insertarMascota(db, pesoIdeal = 20.0)
        val viewModel = nuevoViewModel(db)

        viewModel.mascotas.first { it.isNotEmpty() }

        // `null` es "sin configurar", no 0: el gráfico tiene que dejar de dibujar la referencia.
        viewModel.updatePesoIdeal(id, null)

        val actualizada = withTimeout(5_000) {
            viewModel.mascotas.first { it.first().pesoIdeal == null }
        }
        assertNull(actualizada.first().pesoIdeal)
    }

    @Test
    fun `el cambio queda guardado en la base de datos, no solo en memoria`() = runBlocking {
        val db = bdVacia()
        val id = insertarMascota(db)
        val viewModel = nuevoViewModel(db)

        viewModel.mascotas.first { it.isNotEmpty() }
        viewModel.updatePesoIdeal(id, 31.2)
        withTimeout(5_000) { viewModel.mascotas.first { it.first().pesoIdeal == 31.2 } }

        // Leyendo de nuevo del repositorio: si solo se hubiera cambiado el estado en memoria,
        // al reiniciar la app volvería el valor viejo.
        assertEquals(31.2, MascotaRepository(db).getMascotaById(id)?.pesoIdeal)
    }
}
