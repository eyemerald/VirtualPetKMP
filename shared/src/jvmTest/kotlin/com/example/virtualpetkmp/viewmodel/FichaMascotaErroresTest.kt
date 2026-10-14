package com.example.virtualpetkmp.viewmodel

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * La ficha tiene que **avisar cuando una operación falla**.
 *
 * Antes no lo hacía: las escrituras llamaban al repositorio, tiraban el `Result` a la basura y
 * recargaban los datos como si todo hubiera ido bien. El usuario guardaba una vacuna, no se
 * guardaba, la pantalla quedaba igual y no había forma de enterarse.
 *
 * Para provocar el fallo de verdad —y no con un doble de mentira— se le pasa una base de datos
 * **ya cerrada**: cualquier consulta lanza. Es el equivalente a que el fichero se corrompa o se
 * quede sin espacio, que es justo el caso que interesa.
 */
class FichaMascotaErroresTest {

    private val hoy = LocalDate.parse("2026-10-13")

    private fun baseCerrada(): VirtualPetDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val db = VirtualPetDatabase(driver)
        driver.close()
        return db
    }

    private fun viewModelConBaseRota(): FichaMascotaViewModel {
        val db = baseCerrada()
        return FichaMascotaViewModel(
            mascotaId = 1L,
            vacunaRepository = VacunaRepository(db),
            revisionRepository = RevisionRepository(db),
            tratamientoRepository = TratamientoRepository(db),
            pesoRepository = PesoRepository(db),
            informeRepository = InformeRepository(db),
            notaRepository = NotaRepository(db),
            preventivoRepository = PreventivoRepository(db),
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `una escritura que falla avisa en vez de callarse`() = runBlocking {
        val viewModel = viewModelConBaseRota()

        // Se espera a que falle la carga inicial, que ya deja un aviso.
        viewModel.errorMessage.first { it != null }!!
        viewModel.clearError()

        viewModel.agregarTratamiento(
            nombreMedicamento = "Antibiótico",
            dosis = "1 ml",
            frecuencia = "Cada 12 h",
            fechaInicio = hoy,
            fechaFin = null
        )

        val aviso = withTimeout(5_000) { viewModel.errorMessage.first { it != null }!! }
        assertNotNull(aviso)
        assertTrue(
            aviso.startsWith("No se pudo guardar"),
            "el aviso debería decir que no se pudo guardar: [$aviso]"
        )
    }

    @Test
    fun `borrar tambien avisa cuando falla`() = runBlocking {
        val viewModel = viewModelConBaseRota()
        viewModel.errorMessage.first { it != null }!!
        viewModel.clearError()

        viewModel.borrarPeso(id = 7L)

        val aviso = withTimeout(5_000) { viewModel.errorMessage.first { it != null }!! }
        assertTrue(
            aviso.startsWith("No se pudo borrar"),
            "el aviso debería decir que no se pudo borrar: [$aviso]"
        )
    }

    @Test
    fun `un fallo de lectura no deja la ficha muerta`() = runBlocking {
        // Este es el fallo de fondo: con un `Job` normal, la primera excepción sin capturar
        // cancelaba el scope y TODAS las operaciones siguientes se volvían invisibles. Ahora la
        // lectura va envuelta y el scope sigue vivo, así que el segundo intento también avisa.
        val viewModel = viewModelConBaseRota()

        val primerAviso = withTimeout(5_000) { viewModel.errorMessage.first { it != null }!! }
        assertTrue(primerAviso.startsWith("No se pudo cargar la ficha"), "[$primerAviso]")

        viewModel.clearError()
        viewModel.cargarResumen()

        val segundoAviso = withTimeout(5_000) { viewModel.errorMessage.first { it != null }!! }
        assertTrue(
            segundoAviso.startsWith("No se pudo cargar la ficha"),
            "la ficha dejó de responder tras el primer fallo: [$segundoAviso]"
        )
    }

    @Test
    fun `el aviso se puede cerrar`() = runBlocking {
        val viewModel = viewModelConBaseRota()
        viewModel.errorMessage.first { it != null }!!

        viewModel.clearError()

        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `sin errores no hay ningun aviso`() = runBlocking {
        // La otra cara: con una base de datos que funciona, la ficha no debe inventarse errores.
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        val db = VirtualPetDatabase(driver)

        val viewModel = FichaMascotaViewModel(
            mascotaId = 1L,
            vacunaRepository = VacunaRepository(db),
            revisionRepository = RevisionRepository(db),
            tratamientoRepository = TratamientoRepository(db),
            pesoRepository = PesoRepository(db),
            informeRepository = InformeRepository(db),
            notaRepository = NotaRepository(db),
            preventivoRepository = PreventivoRepository(db),
            dispatcher = Dispatchers.Unconfined
        )

        viewModel.agregarTratamiento("Antibiótico", null, null, hoy, null)
        viewModel.agregarNota("Revisión en casa")
        viewModel.borrarPeso(id = 1L)

        // Se le da margen a que terminen las corrutinas antes de comprobar. Se esperan las DOS
        // cosas: el tratamiento y el resumen, que se recarga por su cuenta. Esperar solo al
        // tratamiento dejaba una carrera —la lista ya estaba y el resumen todavía no— que hacía
        // fallar el test de vez en cuando, justo cuando la máquina iba cargada.
        withTimeout(5_000) { viewModel.tratamientos.first { it.isNotEmpty() } }
        withTimeout(5_000) { viewModel.resumen.first { it.totalNotas == 1 } }

        assertEquals(null, viewModel.errorMessage.value, "no debería haber ningún aviso")
        assertEquals(1, viewModel.tratamientos.value.size)
        assertEquals(1, viewModel.resumen.value.totalNotas)
    }
}
