package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.aviso_aceptar
import virtualpetkmp.shared.generated.resources.aviso_posponer
import virtualpetkmp.shared.generated.resources.aviso_preventivo_mensaje
import virtualpetkmp.shared.generated.resources.aviso_preventivo_titulo
import virtualpetkmp.shared.generated.resources.aviso_tratamiento_mensaje
import virtualpetkmp.shared.generated.resources.aviso_tratamiento_titulo
import virtualpetkmp.shared.generated.resources.aviso_vacuna_mensaje
import virtualpetkmp.shared.generated.resources.aviso_vacuna_titulo

/**
 * Traduce las dosis pendientes de vacunas y preventivos a notificaciones locales.
 *
 * Aquí viven las dos únicas reglas de aviso de la app: una vacuna avisa 15 días antes de la
 * próxima dosis y un preventivo 1 día antes, siempre a las 9 de la mañana. Si la fecha de
 * aviso ya ha pasado, no se programa nada (no tiene sentido avisar de algo que ya venció).
 *
 * **Lo llama `sincronizarAvisos()`**, que calcula el conjunto completo de avisos de la base de
 * datos: antes lo llamaba cada pantalla por su cuenta y por eso quedaban alarmas vivas de filas que
 * ya no existían.
 *
 * **El texto de la notificación está traducido**: es lo primero que ve el usuario y sale fuera
 * de la app, así que se lee con `getString` (la variante `suspend`, porque aquí no hay contexto
 * composable) y la fecha se formatea con la configuración regional que se le pasa.
 *
 * **Nada se programa sin `uuid`.** El `uuid` es lo único que permite reconocer la fila cuando la
 * alarma salte; un aviso sin él sería una alarma que no se puede verificar y que el receptor
 * cancelaría como zombi, así que es preferible no programarla.
 */
object ReprogramadorNotificaciones {

    /** Días antes de la próxima dosis para avisar de una vacuna. */
    private const val DIAS_AVISO_VACUNA = 15

    /** Días antes de la próxima dosis para avisar de un preventivo. */
    private const val DIAS_AVISO_PREVENTIVO = 1

    /** Hora del día a la que se lanza la notificación. */
    private const val HORA_AVISO = 9

    /**
     * Desplazamiento que se suma al id de un preventivo para obtener su id de notificación.
     *
     * Vacunas y preventivos son tablas distintas y **ambas numeran sus filas desde 1**, así que
     * usar el id tal cual hacía que una vacuna y un preventivo de la misma mascota pidieran
     * la misma alarma: la segunda sustituía a la primera y uno de los dos avisos se perdía en
     * silencio. Con dos rangos separados no pueden chocar.
     */
    private const val DESPLAZAMIENTO_PREVENTIVO = 1_000_000_000L

    /**
     * Programa el aviso de la próxima dosis de una vacuna y devuelve lo que quedó registrado.
     *
     * @param localizacion configuración regional activa, para escribir la fecha como la espera
     *   el usuario. Por defecto, español de España, que es lo que usan los tests.
     */
    suspend fun programarVacuna(
        programador: ProgramadorNotificaciones,
        vacuna: Vacuna,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES")
    ): AvisoProgramado? {
        val id = vacuna.id ?: return null
        val (titulo, mensaje) = textoVacuna(vacuna, localizacion)
        return programar(
            programador = programador,
            id = id,
            tipo = TipoAviso.VACUNA,
            uuid = vacuna.uuid,
            fechaProximaDosis = vacuna.fechaProximaDosis,
            diasAviso = DIAS_AVISO_VACUNA,
            titulo = titulo,
            mensaje = mensaje
        )
    }

    /** Programa el aviso de la próxima dosis de un preventivo. */
    suspend fun programarPreventivo(
        programador: ProgramadorNotificaciones,
        preventivo: Preventivo,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES")
    ): AvisoProgramado? {
        val id = preventivo.id ?: return null
        val (titulo, mensaje) = textoPreventivo(preventivo, localizacion)
        return programar(
            programador = programador,
            id = id + DESPLAZAMIENTO_PREVENTIVO,
            tipo = TipoAviso.PREVENTIVO,
            uuid = preventivo.uuid,
            fechaProximaDosis = preventivo.fechaProximaDosis,
            diasAviso = DIAS_AVISO_PREVENTIVO,
            titulo = titulo,
            mensaje = mensaje
        )
    }

    /** Cancela el aviso de un preventivo, usando el mismo id desplazado que al programarlo. */
    fun cancelarPreventivo(programador: ProgramadorNotificaciones, id: Long) {
        programador.cancelar(id + DESPLAZAMIENTO_PREVENTIVO)
    }

    /** Cancela el aviso de una vacuna. */
    fun cancelar(programador: ProgramadorNotificaciones, id: Long) {
        programador.cancelar(id)
    }

    /** El título y el mensaje del aviso de una vacuna, en el idioma activo. */
    suspend fun textoVacuna(vacuna: Vacuna, localizacion: Localizacion): Pair<String, String> =
        getString(Res.string.aviso_vacuna_titulo) to getString(
            Res.string.aviso_vacuna_mensaje,
            vacuna.nombre,
            localizacion.formatearFecha(vacuna.fechaProximaDosis)
        )

    /** El título y el mensaje del aviso de un preventivo, en el idioma activo. */
    suspend fun textoPreventivo(
        preventivo: Preventivo,
        localizacion: Localizacion
    ): Pair<String, String> =
        getString(Res.string.aviso_preventivo_titulo) to getString(
            Res.string.aviso_preventivo_mensaje,
            preventivo.nombre,
            localizacion.formatearFecha(preventivo.fechaProximaDosis)
        )

    /**
     * Programa el recordatorio diario de un tratamiento y devuelve las alarmas que quedaron puestas.
     *
     * A diferencia de las dosis, que avisan **una vez** con días de antelación, un tratamiento se
     * recuerda **cada día a su hora** mientras dure: es una medicina, y el aviso sirve para el día
     * en que toca, no para prepararse. Por eso la alarma se vuelve a programar sola al saltar
     * (ver `NotificacionReceiver`).
     *
     * No programa nada si el tratamiento no pide aviso, si no tiene hora, si ya ha terminado, si no
     * tiene `uuid` (no se podría verificar al sonar) o si no tiene id.
     *
     * **Si el usuario lo pospuso y esa posposición sigue viva, se programa ESA hora** y no la del
     * día siguiente: si no, reiniciar el móvil (o abrir la ficha) se comía el aviso pospuesto. Ver
     * [Posposiciones].
     *
     * @param posposicion posposición viva de un tratamiento. Se puede sustituir en las pruebas para
     *   no depender del fichero real.
     */
    suspend fun programarTratamiento(
        programador: ProgramadorNotificaciones,
        tratamiento: Tratamiento,
        nombreMascota: String,
        ahora: LocalDateTime = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        posposicion: (Long) -> Long? = { Posposiciones.leer(it) }
    ): List<AvisoProgramado> {
        val id = tratamiento.id ?: return emptyList()
        val uuid = tratamiento.uuid
        if (uuid.isNullOrBlank()) {
            println("[Reprogramador] tratamiento $id sin uuid: no se programa su aviso")
            return emptyList()
        }
        // Una alarma por toma: un tratamiento de tres tomas al día tiene que avisar tres veces,
        // cada una con su propio botón de Aceptar y su propio Posponer.
        val horas = HorasDeToma.horasDelTratamiento(tratamiento)
        if (horas.isEmpty()) return emptyList()
        val zona = TimeZone.currentSystemDefault()

        val programados = mutableListOf<AvisoProgramado>()
        horas.forEachIndexed { toma, hora ->
            val notificacionId = RecordatoriosTratamiento.idNotificacion(id, toma)

            val pospuestoMillis = posposicion(notificacionId)?.takeIf { millis ->
                // Solo vale si cae dentro del tratamiento: posponer no puede alargar una medicación.
                RecordatoriosTratamiento.dentroDelTratamiento(
                    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona).date,
                    tratamiento.fechaFin
                )
            }

            val millis = pospuestoMillis ?: run {
                val proximo = RecordatoriosTratamiento.proximoAviso(tratamiento, toma, ahora)
                    ?: return@forEachIndexed
                proximo.toInstant(zona).toEpochMilliseconds()
            }

            val aviso = avisoDeToma(tratamiento, nombreMascota, toma, millis) ?: return@forEachIndexed
            try {
                programador.programarTratamiento(aviso)
                programados += AvisoProgramado(
                    notificacionId = notificacionId,
                    tipo = TipoAviso.TRATAMIENTO,
                    uuid = uuid,
                    hora = hora,
                    toma = toma,
                    // La hora a la que va a sonar de verdad: si está pospuesta, la pospuesta. Es lo
                    // que permite que la sincronización compruebe que sigue viva en vez de creerla
                    // perdida.
                    objetivoMillis = millis
                )
            } catch (e: Exception) {
                println("Error programando el recordatorio del tratamiento: ${e.message}")
            }
        }
        return programados
    }

    /**
     * El aviso de **una** toma de un tratamiento, con el texto ya leído de los recursos.
     *
     * Está aparte porque lo usan los dos lados de la alarma: quien la programa y el receptor que,
     * cuando salta, tiene que volver a construir el texto desde la base de datos. Si se duplicara,
     * el aviso de un día podría decir una cosa y el del siguiente otra.
     *
     * Devuelve `null` si la toma no existe o si el tratamiento no tiene `uuid`.
     */
    suspend fun avisoDeToma(
        tratamiento: Tratamiento,
        nombreMascota: String,
        toma: Int,
        fechaDisparoMillis: Long
    ): AvisoTratamiento? {
        val id = tratamiento.id ?: return null
        val uuid = tratamiento.uuid
        if (uuid.isNullOrBlank()) return null
        val horas = HorasDeToma.horasDelTratamiento(tratamiento)
        val hora = horas.getOrNull(toma) ?: return null

        // El medicamento y su dosis en una sola línea: "Oticoear · 4 gotas".
        val medicamento = listOfNotNull(
            tratamiento.nombreMedicamento,
            tratamiento.dosis?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        return AvisoTratamiento(
            notificacionId = RecordatoriosTratamiento.idNotificacion(id, toma),
            tratamientoId = id,
            mascotaId = tratamiento.mascotaId,
            titulo = getString(Res.string.aviso_tratamiento_titulo),
            mensaje = getString(Res.string.aviso_tratamiento_mensaje, nombreMascota, medicamento),
            fechaDisparoMillis = fechaDisparoMillis,
            // La hora de ESTA toma: es la que se usa para reprogramar la de mañana.
            hora = hora,
            toma = toma,
            fechaFin = tratamiento.fechaFin,
            // Los dos botones se traducen AQUÍ, mientras hay corrutina y recursos: el
            // receptor que los pinta corre con la app cerrada y no puede leerlos.
            textoAceptar = getString(Res.string.aviso_aceptar),
            textoPosponer = getString(
                Res.string.aviso_posponer,
                RecordatoriosTratamiento.POSPOSICION_MINUTOS
            ),
            tratamientoUuid = uuid
        )
    }

    /**
     * Cancela **todas** las tomas de un tratamiento.
     *
     * Se cancelan los seis huecos posibles y no solo los de las horas guardadas: al editar un
     * tratamiento de tres tomas a dos, la tercera ya tenía su alarma programada y, si solo se
     * cancelaran las dos nuevas, seguiría sonando.
     */
    fun cancelarTratamiento(programador: ProgramadorNotificaciones, id: Long) {
        (0 until HorasDeToma.MAXIMO).forEach { toma ->
            programador.cancelar(RecordatoriosTratamiento.idNotificacion(id, toma))
        }
    }

    /**
     * Programa el aviso de una dosis si su día de aviso sigue en el futuro.
     *
     * Devuelve lo que quedó programado, o `null` si no se programó nada (sin `uuid`, sin id, o con
     * una fecha de aviso ya pasada).
     */
    private fun programar(
        programador: ProgramadorNotificaciones,
        id: Long,
        tipo: TipoAviso,
        uuid: String?,
        fechaProximaDosis: LocalDate,
        diasAviso: Int,
        titulo: String,
        mensaje: String
    ): AvisoProgramado? {
        if (uuid.isNullOrBlank()) {
            println("[Reprogramador] dosis $id sin uuid: no se programa su aviso")
            return null
        }

        return try {
            val fechaAviso = fechaDeAviso(fechaProximaDosis, diasAviso)
            val millis = instanteDeAviso(fechaAviso)

            if (millis > Clock.System.now().toEpochMilliseconds()) {
                programador.programar(
                    AvisoDosis(
                        notificacionId = id,
                        tipo = tipo,
                        uuid = uuid,
                        titulo = titulo,
                        mensaje = mensaje,
                        fechaDisparoMillis = millis
                    )
                )
                AvisoProgramado(
                    notificacionId = id,
                    tipo = tipo,
                    uuid = uuid,
                    objetivoMillis = millis
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error programando notificación: ${e.message}")
            null
        }
    }

    /**
     * El día en que avisa una dosis, a las 9 de la mañana.
     *
     * Está expuesto porque el receptor lo necesita para comprobar que la fecha del aviso sigue
     * siendo la misma que cuando se programó.
     */
    fun fechaDeAviso(fechaProximaDosis: LocalDate, diasAviso: Int): LocalDate =
        fechaProximaDosis.minus(DatePeriod(days = diasAviso))

    /** Días de antelación con los que avisa cada tipo de dosis. */
    fun diasDeAviso(tipo: TipoAviso): Int? = when (tipo) {
        TipoAviso.VACUNA -> DIAS_AVISO_VACUNA
        TipoAviso.PREVENTIVO -> DIAS_AVISO_PREVENTIVO
        TipoAviso.TRATAMIENTO -> null
    }

    /**
     * Milisegundos del aviso de un día, a la hora fija de los avisos de dosis.
     *
     * Devuelve milisegundos y no un `Instant` porque el tipo de instante cambia entre versiones de
     * `kotlinx-datetime` y aquí no hace falta: lo único que se hace con él es compararlo y
     * guardarlo.
     */
    private fun instanteDeAviso(fecha: LocalDate): Long {
        val fechaHora = LocalDateTime(fecha.year, fecha.monthNumber, fecha.dayOfMonth, HORA_AVISO, 0)
        return fechaHora.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}
