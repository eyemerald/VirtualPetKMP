package com.example.virtualpetkmp.viewmodel

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fija el caso que hacía que las tarjetas hub pareciesen vacías: una vacuna que vence
 * dentro de la ventana de aviso (15 días) NO cuenta como "al día" ni como "vencida".
 *
 * Si este test cambia, hay que revisar cómo lo muestran las tarjetas de la ficha.
 *
 * Nota: no se instancia el ViewModel porque usa `Dispatchers.Main`, que no existe en
 * jvmTest; se comprueba la misma clasificación sobre los datos del repositorio.
 */
class FichaMascotaResumenTest {

    private fun clasificarVacuna(fechaProximaDosis: LocalDate, hoy: LocalDate): String {
        val en15Dias = LocalDate.fromEpochDays(hoy.toEpochDays() + 15)
        return when {
            fechaProximaDosis < hoy -> "vencida"
            fechaProximaDosis <= en15Dias -> "proxima"
            else -> "alDia"
        }
    }

    private fun bdVacia(): VirtualPetDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        return VirtualPetDatabase(driver)
    }

    @Test
    fun `una vacuna que vence en la ventana de aviso se clasifica como proxima`() = runBlocking {
        val db = bdVacia()
        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val en10Dias = LocalDate.fromEpochDays(hoy.toEpochDays() + 10)
        val haceUnAno = LocalDate.fromEpochDays(hoy.toEpochDays() - 365)

        val id = MascotaRepository(db).insertMascota(
            Mascota(
                nombre = "Rex",
                especie = "Perro",
                raza = "Labrador",
                fechaNacimiento = LocalDate(2020, 1, 15),
                sexo = "Macho",
                color = "Marrón",
                microchip = null
            )
        ).getOrThrow()

        // Exactamente el caso del usuario: la vacuna vence en unos días
        VacunaRepository(db).insertVacuna(
            mascotaId = id,
            nombre = "rabia",
            fechaAplicacion = haceUnAno,
            fechaProximaDosis = en10Dias,
            veterinario = null,
            lote = null
        )

        val vacunas = VacunaRepository(db).getVacunasByMascotaId(id)
        assertEquals(1, vacunas.size)

        val clasificaciones = vacunas.map { clasificarVacuna(it.fechaProximaDosis, hoy) }
        assertEquals(listOf("proxima"), clasificaciones)

        // Consecuencia en el resumen: ni al día ni vencida
        val alDia = clasificaciones.count { it == "alDia" }
        val vencidas = clasificaciones.count { it == "vencida" }
        val proximas = clasificaciones.count { it == "proxima" }
        assertEquals(0, alDia)
        assertEquals(0, vencidas)
        assertEquals(1, proximas)
    }

    @Test
    fun `el informe y la nota se cuentan por separado para el desglose de la tarjeta`() = runBlocking {
        val db = bdVacia()
        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val id = MascotaRepository(db).insertMascota(
            Mascota(
                nombre = "Rex",
                especie = "Perro",
                raza = "Labrador",
                fechaNacimiento = LocalDate(2020, 1, 15),
                sexo = "Macho",
                color = "Marrón",
                microchip = null
            )
        ).getOrThrow()

        InformeRepository(db).insertInforme(
            mascotaId = id,
            tipo = "analitica",
            descripcion = null,
            fecha = hoy,
            nombreArchivo = "analitica.pdf",
            rutaArchivo = "/tmp/analitica.pdf"
        )
        NotaRepository(db).insertNota(id, "NO PUEDE BAJAR SOLA")

        assertEquals(1, InformeRepository(db).getInformesByMascotaId(id).size)
        assertEquals(1, NotaRepository(db).getNotasByMascotaId(id).size)
    }
}
