package com.example.virtualpetkmp.util

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La sincronización de avisos, con un programador de mentira.
 *
 * Lo que se comprueba es el arreglo del fallo: **el conjunto esperado, calculado desde la base de
 * datos, manda**. Todo lo que el registro dice que hay programado se cancela primero, así que una
 * alarma de una fila que ya no existe (mascota borrada, copia restaurada, fusión deshecha) no puede
 * quedarse viva, y no puede haber ni sobrantes ni duplicados.
 */
class SincronizacionAvisosTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: VirtualPetDatabase
    private lateinit var programador: ProgramadorDePrueba
    private lateinit var registro: RegistroAvisosEnMemoria

    private val localizacion = Localizacion.desdeRegion("es-ES")
    private val ahora = LocalDateTime(2026, 5, 20, 7, 0)

    /** Programador de mentira: apunta qué se programa y qué se cancela, sin tocar Android. */
    private class ProgramadorDePrueba : ProgramadorNotificaciones {
        val programadas = linkedMapOf<Long, AvisoProgramado>()
        val canceladas = mutableListOf<Long>()

        override fun programar(aviso: AvisoDosis) {
            programadas[aviso.notificacionId] = AvisoProgramado(
                notificacionId = aviso.notificacionId,
                tipo = aviso.tipo,
                uuid = aviso.uuid
            )
        }

        override fun programarTratamiento(aviso: AvisoTratamiento) {
            programadas[aviso.notificacionId] = AvisoProgramado(
                notificacionId = aviso.notificacionId,
                tipo = TipoAviso.TRATAMIENTO,
                uuid = aviso.tratamientoUuid,
                hora = aviso.hora
            )
        }

        override fun cancelar(id: Long) {
            canceladas.add(id)
        }

        fun olvidar() {
            programadas.clear()
            canceladas.clear()
        }
    }

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VirtualPetDatabase.Schema.create(driver)
        db = VirtualPetDatabase(driver)
        programador = ProgramadorDePrueba()
        registro = RegistroAvisosEnMemoria()
    }

    private suspend fun crearMascota(nombre: String, uuid: String): Long =
        MascotaRepository(db).insertMascota(
            Mascota(
                nombre = nombre,
                especie = "Perro",
                raza = "Mestizo",
                fechaNacimiento = LocalDate(2020, 1, 1),
                sexo = "Hembra",
                color = "Negro",
                microchip = null
            ),
            uuid = uuid
        ).getOrThrow()

    private suspend fun crearTratamiento(
        mascotaId: Long,
        uuid: String,
        horas: String,
        vecesAlDia: Int
    ): Long {
        TratamientoRepository(db).insertTratamiento(
            mascotaId = mascotaId,
            nombreMedicamento = "Oticoear",
            dosis = "4 gotas",
            frecuencia = "Cada 12 h",
            fechaInicio = ahora.date,
            fechaFin = null,
            hora = horas.substringBefore(','),
            notificar = true,
            vecesAlDia = vecesAlDia,
            horas = horas,
            uuid = uuid
        ).getOrThrow()
        return db.tratamientosQueries.selectAll().executeAsList()
            .first { it.uuid == uuid }.id
    }

    private suspend fun sincronizar() = sincronizarAvisos(
        programador = programador,
        db = db,
        localizacion = localizacion,
        registro = registro,
        ahora = ahora
    )

    @Test
    fun `programa una alarma por toma y una por dosis, y el registro dice lo mismo`() = runBlocking {
        val mascotaId = crearMascota("Luna", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val tratamiento = crearTratamiento(
            mascotaId = mascotaId,
            uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
            horas = "08:00,20:00",
            vecesAlDia = 2
        )

        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        VacunaRepository(db).insertVacuna(
            mascotaId = mascotaId,
            nombre = "Rabia",
            fechaAplicacion = hoy,
            fechaProximaDosis = hoy.plus(DatePeriod(days = 40)),
            veterinario = null,
            lote = null,
            uuid = "c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1"
        ).getOrThrow()
        PreventivoRepository(db).insertPreventivo(
            mascotaId = mascotaId,
            tipo = "Pipeta",
            nombre = "Frontline",
            fechaAplicacion = hoy,
            fechaProximaDosis = hoy.plus(DatePeriod(days = 40)),
            veterinario = null,
            lote = null,
            uuid = "d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1"
        ).getOrThrow()

        val esperados = sincronizar()

        assertEquals(
            listOf(
                RecordatoriosTratamiento.idNotificacion(tratamiento, 0),
                RecordatoriosTratamiento.idNotificacion(tratamiento, 1),
                1L,
                1_000_000_001L
            ).sorted(),
            esperados.map { it.notificacionId }.sorted(),
            "una alarma por toma, la de la vacuna y la del preventivo"
        )
        assertEquals(
            esperados.distinctBy { it.notificacionId }.sortedBy { it.notificacionId },
            registro.actuales.sortedBy { it.notificacionId },
            "el registro tiene que ser exactamente lo programado"
        )
        assertEquals(
            esperados.size,
            programador.programadas.size,
            "no puede haber alarmas programadas que no estén en el registro"
        )
    }

    @Test
    fun `lo que ya no esta en la base de datos se cancela`() = runBlocking {
        val mascotaId = crearMascota("Prueba", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val tratamiento = crearTratamiento(
            mascotaId = mascotaId,
            uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
            horas = "08:00,20:00",
            vecesAlDia = 2
        )
        sincronizar()
        programador.olvidar()

        // Borrar la mascota arrastra su tratamiento (ON DELETE CASCADE): las alarmas de sus tomas
        // se quedan sin fila y, sin sincronizar, seguirían sonando.
        MascotaRepository(db).deleteMascota(mascotaId)
        sincronizar()

        assertTrue(
            programador.canceladas.containsAll(
                listOf(
                    RecordatoriosTratamiento.idNotificacion(tratamiento, 0),
                    RecordatoriosTratamiento.idNotificacion(tratamiento, 1)
                )
            ),
            "las dos tomas del tratamiento borrado tienen que cancelarse: [${programador.canceladas}]"
        )
        assertTrue(registro.actuales.isEmpty(), "no puede quedar nada apuntado")
        assertTrue(programador.programadas.isEmpty(), "no hay nada que programar")
    }

    /**
     * El caso exacto del móvil del usuario: se restaura un `.vpet` que sustituye los datos, así que
     * el tratamiento reaparece con **otro id** y **otra hora**. La alarma vieja no la cancela nadie
     * —no hay fila que la reclame— y sigue sonando todos los días.
     */
    @Test
    fun `tras restaurar solo queda programado lo del paquete`() = runBlocking {
        val mascotaId = crearMascota("Prueba", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val tratamientoViejo = crearTratamiento(
            mascotaId = mascotaId,
            uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
            horas = "07:38",
            vecesAlDia = 1
        )
        sincronizar()
        programador.olvidar()

        // Restauración: la tabla se vacía y se reinserta el paquete. La mascota y el tratamiento
        // son otros (otro id, otra hora).
        db.transaction {
            db.tratamientosQueries.deleteAll()
            db.mascotasQueries.deleteAll()
        }
        val mascotaNueva = crearMascota("Nueva", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
        val tratamientoNuevo = crearTratamiento(
            mascotaId = mascotaNueva,
            uuid = "e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2",
            horas = "20:32",
            vecesAlDia = 1
        )
        assertTrue(tratamientoNuevo != tratamientoViejo, "AUTOINCREMENT no reutiliza ids")

        val quedan = sincronizar()

        assertEquals(
            listOf(RecordatoriosTratamiento.idNotificacion(tratamientoNuevo, 0)),
            quedan.map { it.notificacionId },
            "solo la toma que existe de verdad"
        )
        assertTrue(
            programador.canceladas.contains(RecordatoriosTratamiento.idNotificacion(tratamientoViejo, 0)),
            "la alarma del tratamiento que la restauración sustituyó tiene que cancelarse"
        )
        assertTrue(
            programador.programadas.keys.none {
                it == RecordatoriosTratamiento.idNotificacion(tratamientoViejo, 0)
            },
            "la alarma vieja no puede volver a programarse"
        )
    }

    @Test
    fun `sincronizar dos veces seguidas no duplica ni deja sobrantes`() = runBlocking {
        val mascotaId = crearMascota("Luna", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        crearTratamiento(
            mascotaId = mascotaId,
            uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
            horas = "08:00,14:00,22:00",
            vecesAlDia = 3
        )

        val primera = sincronizar()
        val registroPrimera = registro.actuales
        val segunda = sincronizar()

        assertEquals(3, primera.size, "tres tomas, tres alarmas")
        assertEquals(primera, segunda, "sincronizar es idempotente")
        assertEquals(registroPrimera, registro.actuales, "el registro no puede crecer")
        assertEquals(
            primera.map { it.notificacionId }.toSet(),
            registro.actuales.map { it.notificacionId }.toSet()
        )
    }

    @Test
    fun `borrar un tratamiento quita su alarma`() = runBlocking {
        val mascotaId = crearMascota("Luna", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val tratamiento = crearTratamiento(
            mascotaId = mascotaId,
            uuid = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1",
            horas = "08:00",
            vecesAlDia = 1
        )
        sincronizar()
        programador.olvidar()

        TratamientoRepository(db).deleteTratamiento(tratamiento)
        sincronizar()

        assertTrue(
            programador.canceladas.contains(RecordatoriosTratamiento.idNotificacion(tratamiento, 0)),
            "la alarma del tratamiento borrado tiene que cancelarse"
        )
        assertTrue(registro.actuales.isEmpty())
    }
}
