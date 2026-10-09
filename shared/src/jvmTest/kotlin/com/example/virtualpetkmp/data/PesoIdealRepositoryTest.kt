package com.example.virtualpetkmp.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * El peso ideal es opcional y se puede fijar, cambiar y quitar sin tocar el resto de la
 * mascota. Se comprueba sobre una base de datos real (en memoria) para asegurar que la
 * columna nueva guarda `null` de verdad y no un 0, que es la diferencia entre "no
 * configurado" y "peso ideal cero".
 */
class PesoIdealRepositoryTest {

    private fun repositorio(): Pair<MascotaRepository, VirtualPetDatabase> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val db = VirtualPetDatabase(driver)
        return MascotaRepository(db) to db
    }

    private fun mascotaDePrueba(pesoIdeal: Double? = null) = Mascota(
        nombre = "Nala",
        especie = "Gato",
        raza = "Común europeo",
        fechaNacimiento = LocalDate.parse("2021-03-15"),
        sexo = "Hembra",
        color = "Gris",
        microchip = null,
        pesoIdeal = pesoIdeal
    )

    @Test
    fun `se guarda el peso ideal al crear la mascota`() = runBlocking {
        val (repositorio, _) = repositorio()

        val id = repositorio.insertMascota(mascotaDePrueba(pesoIdeal = 4.2)).getOrThrow()

        assertEquals(4.2, repositorio.getMascotaById(id)?.pesoIdeal)
    }

    @Test
    fun `sin peso ideal la columna queda a null, no a cero`() = runBlocking {
        val (repositorio, db) = repositorio()

        val id = repositorio.insertMascota(mascotaDePrueba(pesoIdeal = null)).getOrThrow()

        assertNull(repositorio.getMascotaById(id)?.pesoIdeal)
        assertNull(db.mascotasQueries.selectById(id).executeAsOne().peso_ideal)
    }

    @Test
    fun `el peso ideal se puede cambiar y quitar desde la ficha de peso`() = runBlocking {
        val (repositorio, _) = repositorio()
        val id = repositorio.insertMascota(mascotaDePrueba(pesoIdeal = 4.2)).getOrThrow()

        // Cambiarlo
        repositorio.updatePesoIdeal(id, 4.8).getOrThrow()
        assertEquals(4.8, repositorio.getMascotaById(id)?.pesoIdeal)

        // Quitarlo vuelve a dejarlo sin configurar
        repositorio.updatePesoIdeal(id, null).getOrThrow()
        assertNull(repositorio.getMascotaById(id)?.pesoIdeal)
    }

    @Test
    fun `actualizar la mascota completa conserva y respeta el peso ideal`() = runBlocking {
        val (repositorio, _) = repositorio()
        val id = repositorio.insertMascota(mascotaDePrueba(pesoIdeal = 4.2)).getOrThrow()

        // Editar otros campos sin tocar el peso ideal lo mantiene
        val original = repositorio.getMascotaById(id)
        assertNotNull(original)
        repositorio.updateMascota(original.copy(color = "Atigrado")).getOrThrow()
        assertEquals(4.2, repositorio.getMascotaById(id)?.pesoIdeal)
        assertEquals("Atigrado", repositorio.getMascotaById(id)?.color)

        // Y quitarlo desde el formulario también se respeta
        repositorio.updateMascota(original.copy(pesoIdeal = null)).getOrThrow()
        assertNull(repositorio.getMascotaById(id)?.pesoIdeal)
    }
}
