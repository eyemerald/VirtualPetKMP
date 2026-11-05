package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * La entidad de un aviso, **leída ahora mismo** de la base de datos, con lo que hace falta para
 * decidir si el aviso debe sonar.
 *
 * Es lo que consulta el receptor de la alarma cuando salta: la app puede estar cerrada y los datos
 * haber cambiado (una copia restaurada, una mascota borrada, un tratamiento editado), así que el
 * aviso no puede fiarse de lo que llevaba dentro.
 */
sealed interface EntidadDeAviso {

    /** Lo que la base de datos dice de la fila que el aviso dice ser. */
    val estado: EstadoEntidadAviso

    data class DeTratamiento(
        val tratamiento: Tratamiento,
        val nombreMascota: String,
        override val estado: EstadoEntidadAviso
    ) : EntidadDeAviso

    data class DeVacuna(
        val vacuna: Vacuna,
        override val estado: EstadoEntidadAviso
    ) : EntidadDeAviso

    data class DePreventivo(
        val preventivo: Preventivo,
        override val estado: EstadoEntidadAviso
    ) : EntidadDeAviso
}

/**
 * Busca la entidad de un aviso por su `uuid` y dice si su aviso sigue vigente.
 *
 * Devuelve `null` si la fila ya no existe, que es el caso de la alarma zombi: la mascota o el
 * tratamiento se borraron, o una restauración sustituyó los datos por otros.
 *
 * **Por qué por `uuid` y no por id.** Al restaurar una copia, las filas se vuelven a insertar y
 * reciben ids nuevos (`AUTOINCREMENT` no reutiliza): una alarma programada con el id viejo apunta a
 * una fila que ya no es la suya, o a ninguna. El `uuid` sobrevive a la restauración porque va dentro
 * del paquete.
 *
 * Para vacunas y preventivos, además de existir, la fecha de aviso tiene que seguir siendo la
 * misma: si al usuario le cambian la próxima dosis, la alarma programada para la fecha vieja ya no
 * toca.
 *
 * @param diaDelAvisoMillis el día que la alarma tenía previsto saltar.
 * @param hoy fecha de hoy, para saber si el tratamiento está en curso.
 */
suspend fun entidadDeAviso(
    db: VirtualPetDatabase,
    tipo: TipoAviso,
    uuid: String,
    diaDelAvisoMillis: Long,
    hoy: LocalDate
): EntidadDeAviso? = when (tipo) {
    TipoAviso.TRATAMIENTO -> {
        TratamientoRepository(db).getTratamientoPorUuid(uuid)?.let { (tratamiento, nombreMascota) ->
            EntidadDeAviso.DeTratamiento(
                tratamiento = tratamiento,
                nombreMascota = nombreMascota,
                estado = EstadoEntidadAviso(
                    uuid = uuid,
                    horasDeAviso = HorasDeToma.horasDelTratamiento(tratamiento),
                    // "Sigue vigente" = pide aviso y hoy cae dentro de sus fechas.
                    vigente = tratamiento.notificar &&
                        hoy >= tratamiento.fechaInicio &&
                        (tratamiento.fechaFin == null || hoy <= tratamiento.fechaFin)
                )
            )
        }
    }

    TipoAviso.VACUNA -> {
        VacunaRepository(db).getVacunaPorUuid(uuid)?.let { vacuna ->
            EntidadDeAviso.DeVacuna(
                vacuna = vacuna,
                estado = EstadoEntidadAviso(
                    uuid = uuid,
                    vigente = caeElMismoDia(
                        diaDelAvisoMillis,
                        ReprogramadorNotificaciones.fechaDeAviso(
                            vacuna.fechaProximaDosis,
                            ReprogramadorNotificaciones.diasDeAviso(TipoAviso.VACUNA) ?: 0
                        )
                    )
                )
            )
        }
    }

    TipoAviso.PREVENTIVO -> {
        PreventivoRepository(db).getPreventivoPorUuid(uuid)?.let { preventivo ->
            EntidadDeAviso.DePreventivo(
                preventivo = preventivo,
                estado = EstadoEntidadAviso(
                    uuid = uuid,
                    vigente = caeElMismoDia(
                        diaDelAvisoMillis,
                        ReprogramadorNotificaciones.fechaDeAviso(
                            preventivo.fechaProximaDosis,
                            ReprogramadorNotificaciones.diasDeAviso(TipoAviso.PREVENTIVO) ?: 0
                        )
                    )
                )
            )
        }
    }
}

/** Si el momento [millis] cae en el día [fecha], en la zona horaria del dispositivo. */
private fun caeElMismoDia(millis: Long, fecha: LocalDate): Boolean {
    if (millis <= 0L) return false
    val dia = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return dia == fecha
}
