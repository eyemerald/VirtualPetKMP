package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprueba las dos reglas de aviso de la app: con cuánta antelación se avisa y con qué id se
 * registra la notificación.
 *
 * El id importa más de lo que parece: vacunas y preventivos son tablas distintas y **ambas
 * numeran sus filas desde 1**, así que si compartieran el id de notificación, el aviso de una
 * vacuna y el de un preventivo de la misma mascota se pisarían y uno de los dos se perdería
 * sin que nadie se enterara. Este test existe justo por eso.
 *
 * El texto de la notificación también se comprueba: es lo primero que ve el usuario, sale fuera
 * de la app y hasta hace poco estaba escrito a mano en español.
 */
class ReprogramadorNotificacionesTest {

    /** Programador de mentira: apunta lo que le piden, sin tocar AlarmManager. */
    private class ProgramadorFalso : ProgramadorNotificaciones {
        val programadas = mutableMapOf<Long, Long>()
        val canceladas = mutableListOf<Long>()
        val textos = mutableMapOf<Long, Pair<String, String>>()

        /** Recordatorios de tratamiento: a qué hora y con qué fecha de fin se programaron. */
        val recordatorios = mutableMapOf<Long, Pair<String, String?>>()

        /** El aviso completo, que es lo que se lleva la alarma a Android. */
        val avisos = mutableMapOf<Long, AvisoTratamiento>()

        override fun programar(id: Long, titulo: String, mensaje: String, fechaDisparoMillis: Long) {
            programadas[id] = fechaDisparoMillis
            textos[id] = titulo to mensaje
        }

        override fun programarTratamiento(aviso: AvisoTratamiento) {
            programar(aviso.notificacionId, aviso.titulo, aviso.mensaje, aviso.fechaDisparoMillis)
            recordatorios[aviso.notificacionId] = aviso.hora to aviso.fechaFin?.toString()
            avisos[aviso.notificacionId] = aviso
        }

        override fun cancelar(id: Long) {
            canceladas.add(id)
        }
    }

    private val hoy = LocalDate.parse("2026-10-09")

    private fun vacuna(id: Long, venceEnDias: Int) = Vacuna(
        id = id,
        mascotaId = 1L,
        nombre = "Rabia",
        fechaAplicacion = hoy,
        fechaProximaDosis = LocalDate.fromEpochDays(hoy.toEpochDays() + venceEnDias)
    )

    private fun preventivo(id: Long, venceEnDias: Int) = Preventivo(
        id = id,
        mascotaId = 1L,
        tipo = "Pipeta",
        nombre = "Frontline",
        fechaAplicacion = hoy,
        fechaProximaDosis = LocalDate.fromEpochDays(hoy.toEpochDays() + venceEnDias)
    )

    @Test
    fun `una vacuna y un preventivo con el mismo id de fila no comparten notificacion`() = runBlocking {
        val programador = ProgramadorFalso()

        // Mismo id de fila en las dos tablas: es el caso normal, ambas empiezan en 1.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = 60))
        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 1L, venceEnDias = 60))

        assertEquals(2, programador.programadas.size, "los dos avisos deben quedar registrados")
        assertNotEquals(
            programador.programadas.keys.first(),
            programador.programadas.keys.last()
        )
    }

    @Test
    fun `cancelar un preventivo usa el mismo id con el que se programo`() = runBlocking {
        val programador = ProgramadorFalso()

        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 7L, venceEnDias = 60))
        val idProgramado = programador.programadas.keys.single()

        ReprogramadorNotificaciones.cancelarPreventivo(programador, 7L)

        assertEquals(listOf(idProgramado), programador.canceladas)
    }

    @Test
    fun `la vacuna avisa 15 dias antes y el preventivo 1 dia antes`() = runBlocking {
        val programador = ProgramadorFalso()

        // Vencen dentro de 60 días, así que ambos avisos caen en el futuro y se programan.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = 60))
        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo(id = 2L, venceEnDias = 60))

        val diasHasta = { millis: Long ->
            val fechaAviso = Instant.fromEpochMilliseconds(millis)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            fechaAviso.toEpochDays() - hoy.toEpochDays()
        }

        val avisoVacuna = diasHasta(programador.programadas.values.first())
        val avisoPreventivo = diasHasta(programador.programadas.values.last())

        assertEquals(45, avisoVacuna, "60 días menos los 15 de antelación de la vacuna")
        assertEquals(59, avisoPreventivo, "60 días menos el día de antelación del preventivo")
    }

    @Test
    fun `una dosis cuyo aviso ya ha pasado no programa nada`() = runBlocking {
        val programador = ProgramadorFalso()

        // Venció hace 30 días: el aviso habría caído en la mitad del pasado.
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna(id = 1L, venceEnDias = -30))

        assertTrue(programador.programadas.isEmpty())
    }

    @Test
    fun `el texto de la notificacion sale traducido y con el formato de fecha de la region`() = runBlocking {
        val idiomaOriginal = java.util.Locale.getDefault()
        try {
            val programador = ProgramadorFalso()

            // El TEXTO sigue el idioma del sistema (que es lo que resuelve Compose Resources)...
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            ReprogramadorNotificaciones.programarVacuna(
                programador,
                vacuna(id = 1L, venceEnDias = 60),
                Localizacion.desdeRegion("es-ES")
            )
            val (tituloAleman, mensajeAleman) = programador.textos.getValue(1L)

            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("es-ES"))
            val programadorEs = ProgramadorFalso()
            ReprogramadorNotificaciones.programarVacuna(
                programadorEs,
                vacuna(id = 1L, venceEnDias = 60),
                Localizacion.desdeRegion("es-ES")
            )
            val (tituloEspanol, _) = programadorEs.textos.getValue(1L)

            assertTrue(tituloAleman.isNotBlank() && tituloEspanol.isNotBlank())
            assertTrue(
                tituloAleman != tituloEspanol,
                "el aviso no se tradujo: alemán [$tituloAleman], español [$tituloEspanol]"
            )
            assertTrue(mensajeAleman.contains("Rabia"), "falta el nombre: [$mensajeAleman]")

            // ...y la FECHA sigue la configuración regional que se le pasa, no el idioma del
            // texto: en la app las dos salen del mismo sitio, pero aquí se verifica por separado.
            // La dosis vence el 2026-12-08: día/mes en español y mes/día en inglés.
            val programadorUs = ProgramadorFalso()
            ReprogramadorNotificaciones.programarVacuna(
                programadorUs,
                vacuna(id = 1L, venceEnDias = 60),
                Localizacion.desdeRegion("en-US")
            )
            val mensajeUs = programadorUs.textos.getValue(1L).second

            assertTrue(mensajeUs.contains("12/08/2026"), "en inglés la fecha va mes/día: [$mensajeUs]")
            assertTrue(!mensajeUs.contains("08/12/2026"), "en inglés no debe salir día/mes: [$mensajeUs]")
        } finally {
            java.util.Locale.setDefault(idiomaOriginal)
        }
    }

    @Test
    fun `un tratamiento con aviso se programa a su hora y con la mascota en el texto`() = runBlocking {
        val programador = ProgramadorFalso()
        val ahora = LocalDateTime(2026, 5, 20, 7, 0)
        val tratamiento = Tratamiento(
            id = 3L,
            mascotaId = 1L,
            nombreMedicamento = "Oticoear",
            dosis = "4 gotas",
            fechaInicio = ahora.date,
            fechaFin = LocalDate.parse("2026-05-30"),
            hora = "08:30",
            notificar = true
        )

        ReprogramadorNotificaciones.programarTratamiento(programador, tratamiento, "Luna", ahora)

        val id = RecordatoriosTratamiento.idNotificacion(3L)
        assertTrue(programador.programadas.containsKey(id), "no se programó el recordatorio")

        val (hora, fin) = programador.recordatorios.getValue(id)
        assertEquals("08:30", hora, "la alarma de mañana necesita la hora, ya normalizada")
        assertEquals("2026-05-30", fin, "la fecha de fin va en el aviso para no pasarse de día")

        val mensaje = programador.textos.getValue(id).second
        assertTrue(mensaje.contains("Luna"), "falta la mascota: [$mensaje]")
        assertTrue(mensaje.contains("Oticoear"), "falta el medicamento: [$mensaje]")
        assertTrue(mensaje.contains("4 gotas"), "falta la dosis: [$mensaje]")
    }

    /**
     * El aviso viaja entero dentro de la alarma porque el receptor que lo pinta corre con la app
     * cerrada. Si faltara la mascota, tocar la notificación no podría abrir su ficha; si faltaran
     * los textos, los botones saldrían sin texto (o con el de otro idioma).
     */
    @Test
    fun `el aviso de un tratamiento lleva su mascota y los textos de los botones`() = runBlocking {
        val programador = ProgramadorFalso()
        val ahora = LocalDateTime(2026, 5, 20, 7, 0)
        val tratamiento = Tratamiento(
            id = 7L,
            mascotaId = 42L,
            nombreMedicamento = "Oticoear",
            fechaInicio = ahora.date,
            hora = "08:30",
            notificar = true
        )

        ReprogramadorNotificaciones.programarTratamiento(programador, tratamiento, "Luna", ahora)

        val aviso = programador.avisos.getValue(RecordatoriosTratamiento.idNotificacion(7L))
        assertEquals(42L, aviso.mascotaId, "sin la mascota no se puede abrir su ficha")
        assertEquals(7L, aviso.tratamientoId, "el id real hace falta para distinguir dos avisos")
        assertTrue(aviso.textoAceptar.isNotBlank(), "el botón de aceptar saldría sin texto")
        assertTrue(
            aviso.textoPosponer.contains(RecordatoriosTratamiento.POSPOSICION_MINUTOS.toString()),
            "el botón de posponer debe decir cuánto pospone: [${aviso.textoPosponer}]"
        )
    }

    @Test
    fun `un tratamiento sin aviso no programa nada`() = runBlocking {        val programador = ProgramadorFalso()
        val ahora = LocalDateTime(2026, 5, 20, 7, 0)

        ReprogramadorNotificaciones.programarTratamiento(
            programador,
            Tratamiento(
                id = 4L,
                mascotaId = 1L,
                nombreMedicamento = "Metacam",
                fechaInicio = ahora.date,
                hora = "08:30",
                notificar = false
            ),
            "Luna",
            ahora
        )

        assertTrue(programador.programadas.isEmpty())
    }

    @Test
    fun `cancelar un tratamiento usa el id desplazado con el que se programo`() = runBlocking {
        val programador = ProgramadorFalso()

        ReprogramadorNotificaciones.cancelarTratamiento(programador, 5L)

        assertEquals(listOf(RecordatoriosTratamiento.idNotificacion(5L)), programador.canceladas)
    }
}
