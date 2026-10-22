package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El identificador universal y la fecha de modificación, que son la base del traspaso de datos.
 *
 * Lo que se comprueba aquí no se ve en ninguna pantalla: si el `uuid` se repitiera entre dos
 * filas, al importar un paquete una sobrescribiría a la otra; si cambiara al editar, el mismo
 * registro se duplicaría en el otro dispositivo en cada importación. Por eso son pruebas, y no
 * una comprobación a ojo en la interfaz.
 */
class IdentificadorUniversalTest {

    private lateinit var db: VirtualPetDatabase

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        db = VirtualPetDatabase(driver)
    }

    private fun mascota(nombre: String) = Mascota(
        nombre = nombre,
        especie = "Perro",
        raza = "Mestizo",
        fechaNacimiento = LocalDate(2020, 1, 1),
        sexo = "Hembra",
        color = "Negro",
        microchip = null
    )

    private fun uuid(mascotaId: Long): String =
        db.mascotasQueries.selectAll().executeAsList().first { it.id == mascotaId }.uuid

    private fun actualizadoEn(mascotaId: Long): Long =
        db.mascotasQueries.selectAll().executeAsList().first { it.id == mascotaId }.actualizadoEn

    @Test
    fun `el generador produce 32 caracteres hexadecimales`() {
        val identificador = nuevoIdentificadorUniversal()

        assertEquals(32, identificador.length, "el formato debe coincidir con el de la migración")
        assertTrue(
            identificador.all { it in "0123456789abcdef" },
            "solo minúsculas hexadecimales, sin guiones: [$identificador]"
        )
    }

    @Test
    fun `mil identificadores generados no repiten ninguno`() {
        val generados = List(1000) { nuevoIdentificadorUniversal() }

        assertEquals(1000, generados.toSet().size, "hay identificadores repetidos")
    }

    @Test
    fun `una mascota nueva recibe identificador y fecha de modificacion`() = runBlocking {
        val id = MascotaRepository(db).insertMascota(mascota("Luna")).getOrThrow()

        assertEquals(32, uuid(id).length, "la fila se guardó sin identificador universal")
        assertTrue(actualizadoEn(id) > 0, "la fila se guardó sin fecha de modificación")
    }

    @Test
    fun `dos mascotas nunca comparten identificador`() = runBlocking {
        val repository = MascotaRepository(db)

        val primera = repository.insertMascota(mascota("Luna")).getOrThrow()
        val segunda = repository.insertMascota(mascota("Nilo")).getOrThrow()

        assertTrue(uuid(primera) != uuid(segunda), "dos filas distintas no pueden tener el mismo uuid")
    }

    @Test
    fun `editar una mascota conserva el identificador y sube la fecha`() = runBlocking {
        val repository = MascotaRepository(db)
        val id = repository.insertMascota(mascota("Luna"), actualizadoEn = 1_000L).getOrThrow()
        val identificadorOriginal = uuid(id)

        repository.updateMascota(
            mascota("Luna").copy(id = id, color = "Blanco"),
            actualizadoEn = 2_000L
        ).getOrThrow()

        assertEquals(
            identificadorOriginal,
            uuid(id),
            "el uuid no puede cambiar al editar: es lo que reconoce la misma mascota en otro móvil"
        )
        assertEquals(2_000L, actualizadoEn(id), "la fecha de modificación no se actualizó")
    }

    @Test
    fun `el identificador se puede fijar a mano para conservar el de un paquete`() = runBlocking {
        val repository = MascotaRepository(db)

        val id = repository.insertMascota(
            mascota("Luna"),
            uuid = "0123456789abcdef0123456789abcdef",
            actualizadoEn = 1_700_000_000_000L
        ).getOrThrow()

        assertEquals("0123456789abcdef0123456789abcdef", uuid(id))
        assertEquals(1_700_000_000_000L, actualizadoEn(id))
    }

    /**
     * La importación tiene que poder conservar los identificadores de **cualquier** tabla, no solo
     * de la mascota. Si una tabla se quedara sin él, sus filas no se podrían reconocer.
     */
    @Test
    fun `todas las tablas guardan identificador y fecha de modificacion`() = runBlocking {
        val mascotaId = MascotaRepository(db).insertMascota(mascota("Luna")).getOrThrow()

        VacunaRepository(db).insertVacuna(
            mascotaId = mascotaId,
            nombre = "Rabia",
            fechaAplicacion = LocalDate(2026, 1, 10),
            fechaProximaDosis = LocalDate(2027, 1, 10),
            veterinario = "Dra. Ruiz",
            lote = "L-1"
        ).getOrThrow()

        PreventivoRepository(db).insertPreventivo(
            mascotaId = mascotaId,
            tipo = "Pipeta",
            nombre = "Frontline",
            fechaAplicacion = LocalDate(2026, 2, 1),
            fechaProximaDosis = LocalDate(2026, 3, 1),
            veterinario = null,
            lote = null
        ).getOrThrow()

        RevisionRepository(db).insertRevision(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 2),
            motivo = "Revisión",
            diagnostico = null,
            notas = null,
            veterinario = null
        ).getOrThrow()

        TratamientoRepository(db).insertTratamiento(
            mascotaId = mascotaId,
            nombreMedicamento = "Oticoear",
            dosis = "4 gotas",
            frecuencia = "Cada 12 horas",
            fechaInicio = LocalDate(2026, 2, 3),
            fechaFin = null
        ).getOrThrow()

        PesoRepository(db).insertPeso(
            mascotaId = mascotaId,
            fecha = LocalDate(2026, 2, 4),
            peso = 26.3,
            notas = null
        ).getOrThrow()

        InformeRepository(db).insertInforme(
            mascotaId = mascotaId,
            tipo = "Analítica",
            descripcion = null,
            fecha = LocalDate(2026, 2, 5),
            nombreArchivo = "analitica.pdf",
            rutaArchivo = "/tmp/analitica.pdf"
        ).getOrThrow()

        NotaRepository(db).insertNota(mascotaId = mascotaId, texto = "Primera nota").getOrThrow()

        VeterinarioRepository(db).insertVeterinario(
            com.example.virtualpetkmp.Veterinario(
                nombreClinica = "Clínica del Barrio",
                nombreVeterinario = "Dr. López",
                telefono = "918765432",
                direccion = "Avenida del Parque 4",
                esUrgencias = false
            )
        ).getOrThrow()

        // Cada tabla, con su identificador (32 caracteres) y su fecha (> 0).
        val filas: Map<String, Pair<String, Long>> = mapOf(
            "mascotas" to db.mascotasQueries.selectAll().executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "vacunas" to db.vacunasQueries.selectAll().executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "preventivos" to db.preventivosQueries.selectAll().executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "revisiones" to db.revisionesQueries.selectByMascotaId(mascotaId).executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "tratamientos" to db.tratamientosQueries.selectByMascotaId(mascotaId).executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "pesos" to db.pesosQueries.selectByMascotaId(mascotaId).executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "informes" to db.informesQueries.selectByMascotaId(mascotaId).executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "notas" to db.notasQueries.selectByMascotaId(mascotaId).executeAsList().map { it.uuid to it.actualizadoEn }.single(),
            "veterinarios" to db.veterinariosQueries.selectAll().executeAsList().map { it.uuid to it.actualizadoEn }.single()
        )

        filas.forEach { (tabla, fila) ->
            val (identificador, fecha) = fila
            assertEquals(32, identificador.length, "la tabla '$tabla' guardó un identificador raro")
            assertTrue(fecha > 0, "la tabla '$tabla' no tiene fecha de modificación")
        }

        assertEquals(
            filas.size,
            filas.values.map { it.first }.toSet().size,
            "dos tablas distintas acabaron con el mismo identificador"
        )
    }
}
