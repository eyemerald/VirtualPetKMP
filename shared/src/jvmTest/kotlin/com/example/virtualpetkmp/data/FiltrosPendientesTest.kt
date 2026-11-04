package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.ui.filtrarMascotas
import com.example.virtualpetkmp.util.Pendientes
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
 * Los dos filtros de pendientes de la lista de mascotas, con una base de datos **en fichero**.
 *
 * En fichero y no en memoria a propósito: la consulta usa `EXISTS` con una subconsulta agrupada por
 * nombre, y eso es justo lo que puede comportarse distinto según el plan que elija SQLite. Una base
 * real en un directorio temporal (nunca la del usuario) es la prueba honesta.
 *
 * La regla que se prueba: **pendiente = la próxima dosis ya pasó o cae en los próximos 30 días, y
 * solo cuenta el último registro de cada vacuna o preventivo**.
 */
class FiltrosPendientesTest {

    private lateinit var carpeta: File
    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var db: VirtualPetDatabase
    private lateinit var mascotas: MascotaRepository
    private lateinit var vacunas: VacunaRepository
    private lateinit var preventivos: PreventivoRepository

    /** "Hoy" fijo: la ventana de 30 días se prueba con fechas relativas a este día. */
    private val hoy = LocalDate(2026, 6, 15)

    @BeforeTest
    fun abrir() {
        carpeta = Files.createTempDirectory("virtualpet-filtros").toFile()
        driver = crearDriverJdbc(File(carpeta, "prueba.db"))
        VirtualPetDatabase.Schema.create(driver).value
        db = VirtualPetDatabase(driver)
        mascotas = MascotaRepository(db)
        vacunas = VacunaRepository(db)
        preventivos = PreventivoRepository(db)
    }

    @AfterTest
    fun cerrar() {
        driver.close()
        carpeta.deleteRecursively()
    }

    // --- Vacunas ---------------------------------------------------------------------------

    @Test
    fun `una vacuna puesta varias veces solo cuenta por su ultimo registro`() = runBlocking {
        val luna = crearMascota("Luna")
        // La primera dosis está vencida, pero la segunda (la última) no vence hasta dentro de 90
        // días: la mascota NO tiene nada pendiente. Es el caso que distingue "último registro" de
        // "cualquier registro".
        vacunas.insertVacuna(luna, "Polivalente", LocalDate(2024, 1, 1), LocalDate(2026, 1, 1), veterinario = null, lote = null)
        vacunas.insertVacuna(luna, "Polivalente", LocalDate(2026, 5, 1), fechaEnDias(90), veterinario = null, lote = null)

        val pendientes = mascotas.getIdsConVacunaPendiente(hoy)

        assertTrue(pendientes.isEmpty(), "una vacuna ya repetida no puede salir como pendiente: $pendientes")
    }

    @Test
    fun `una dosis vencida esta pendiente`() = runBlocking {
        val luna = crearMascota("Luna")
        vacunas.insertVacuna(luna, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(-5), veterinario = null, lote = null)

        assertEquals(setOf(luna), mascotas.getIdsConVacunaPendiente(hoy))
    }

    @Test
    fun `una dosis dentro de diez dias esta pendiente`() = runBlocking {
        val luna = crearMascota("Luna")
        vacunas.insertVacuna(luna, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(10), veterinario = null, lote = null)

        assertEquals(setOf(luna), mascotas.getIdsConVacunaPendiente(hoy))
    }

    @Test
    fun `una dosis dentro de noventa dias no esta pendiente`() = runBlocking {
        val luna = crearMascota("Luna")
        vacunas.insertVacuna(luna, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(90), veterinario = null, lote = null)

        assertTrue(mascotas.getIdsConVacunaPendiente(hoy).isEmpty())
    }

    @Test
    fun `el limite son treinta dias justos`() = runBlocking {
        // El día 30 entra y el 31 no: es la frontera de la ventana, la que se rompe al cambiarla.
        val luna = crearMascota("Luna")
        vacunas.insertVacuna(luna, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(30), veterinario = null, lote = null)
        assertEquals(setOf(luna), mascotas.getIdsConVacunaPendiente(hoy))

        val michi = crearMascota("Michi")
        vacunas.insertVacuna(michi, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(31), veterinario = null, lote = null)
        assertEquals(setOf(luna), mascotas.getIdsConVacunaPendiente(hoy))
    }

    @Test
    fun `una mascota sin vacunas no esta pendiente`() = runBlocking {
        crearMascota("Luna")

        assertTrue(mascotas.getIdsConVacunaPendiente(hoy).isEmpty())
        assertTrue(mascotas.getIdsConPreventivoPendiente(hoy).isEmpty())
    }

    // --- Preventivos -----------------------------------------------------------------------

    @Test
    fun `los preventivos son independientes de las vacunas`() = runBlocking {
        val luna = crearMascota("Luna")
        val michi = crearMascota("Michi")

        // Luna: vacuna vencida y pipeta al día. Michi: al revés.
        vacunas.insertVacuna(luna, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(-2), veterinario = null, lote = null)
        preventivos.insertPreventivo(michi, "Interno", "Pipeta", LocalDate(2025, 1, 1), fechaEnDias(-2), veterinario = null, lote = null)

        assertEquals(setOf(luna), mascotas.getIdsConVacunaPendiente(hoy))
        assertEquals(setOf(michi), mascotas.getIdsConPreventivoPendiente(hoy))
    }

    @Test
    fun `un preventivo repetido tampoco cuenta por su registro viejo`() = runBlocking {
        val luna = crearMascota("Luna")
        preventivos.insertPreventivo(luna, "Externo", "Pipeta", LocalDate(2024, 1, 1), LocalDate(2025, 1, 1), veterinario = null, lote = null)
        preventivos.insertPreventivo(luna, "Externo", "Pipeta", LocalDate(2026, 6, 1), fechaEnDias(60), veterinario = null, lote = null)

        assertTrue(mascotas.getIdsConPreventivoPendiente(hoy).isEmpty())
    }

    // --- Combinación con la búsqueda por nombre --------------------------------------------

    @Test
    fun `el filtro se combina con la busqueda por nombre`() = runBlocking {
        val luna = crearMascota("Luna")
        val lunaDos = crearMascota("Luna II")
        val rocky = crearMascota("Rocky")
        listOf(luna, lunaDos, rocky).forEach { id ->
            vacunas.insertVacuna(id, "Rabia", LocalDate(2025, 1, 1), fechaEnDias(-2), veterinario = null, lote = null)
        }

        val pendientes = mascotas.getIdsConVacunaPendiente(hoy)
        val todas = mascotas.getAllMascotas()

        // Es exactamente lo que hace la pantalla: primero el nombre, después el filtro.
        val visibles = filtrarMascotas(todas, "luna").filter { it.id in pendientes }

        assertEquals(2, visibles.size, "la busqueda y el filtro tienen que poder usarse a la vez")
        assertTrue(visibles.all { it.nombre.startsWith("Luna") })
    }

    @Test
    fun `la regla de pendiente coincide con la que aplica el SQL`() {
        // La misma frontera escrita en Kotlin, para que un cambio en la ventana rompa esta prueba.
        assertEquals(30, Pendientes.VENTANA_DIAS)
        assertTrue(Pendientes.estaPendiente(hoy, hoy))
        assertTrue(Pendientes.estaPendiente(fechaEnDias(30), hoy))
        assertTrue(!Pendientes.estaPendiente(fechaEnDias(31), hoy))
    }

    // --- Rendimiento ------------------------------------------------------------------------

    @Test
    fun `filtrar con quinientas mascotas no se nota`() = runBlocking {
        // La lista de la protectora puede tener cientos de animales. Si el filtro se hiciera
        // recorriendo mascotas en Kotlin, cada apertura de la lista costaria lo mismo que cargar
        // todas las vacunas; con la consulta SQL cuesta una consulta.
        val inicioInsercion = System.nanoTime()
        repeat(500) { indice ->
            val mascotaId = crearMascota("Prueba %03d".format(indice))
            // Cinco dosis de la misma vacuna por mascota: 2.500 filas, que es lo que hace lento
            // un filtro mal escrito.
            repeat(5) { dosis ->
                vacunas.insertVacuna(
                    mascotaId,
                    "Polivalente",
                    fechaEnDias(-365 + dosis),
                    // Una de cada dos mascotas tiene la dosis vencida.
                    fechaEnDias(if (indice % 2 == 0) -1 else 200),
                    veterinario = null,
                    lote = null
                )
            }
        }
        val insercionMs = (System.nanoTime() - inicioInsercion) / 1_000_000

        // Se mide el filtro varias veces y se informa de la mediana: la primera llamada incluye
        // el calentamiento de la JVM y no representa al usuario.
        val tiempos = (1..5).map {
            val t0 = System.nanoTime()
            val ids = mascotas.getIdsConVacunaPendiente(hoy)
            val ms = (System.nanoTime() - t0) / 1_000_000
            assertEquals(250, ids.size, "la mitad de 500 mascotas tiene la dosis vencida")
            ms
        }.sorted()

        val mediana = tiempos[tiempos.size / 2]
        println("FILTRO_500: insercion=${insercionMs}ms mediana=${mediana}ms tiempos=$tiempos")

        assertTrue(
            mediana < 2_000,
            "filtrar 500 mascotas no puede tardar 2 segundos (tardo ${mediana}ms)"
        )
    }

    // --- Ayudas ----------------------------------------------------------------------------

    private suspend fun crearMascota(nombre: String): Long {
        mascotas.insertMascota(
            Mascota(
                nombre = nombre,
                especie = "Perro",
                raza = "Mestizo",
                fechaNacimiento = LocalDate(2020, 1, 1),
                sexo = "Hembra",
                color = "Negro",
                microchip = null,
                foto = null,
                pesoIdeal = null
            )
        ).getOrThrow()
        return db.mascotasQueries.selectAll().executeAsList().single { it.nombre == nombre }.id
    }

    private fun fechaEnDias(dias: Int): LocalDate =
        LocalDate.fromEpochDays(hoy.toEpochDays() + dias)
}
