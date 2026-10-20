package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Cuándo toca avisar de un tratamiento.
 *
 * Es la única regla del recordatorio y vive aquí, aparte de la pantalla y del `AlarmManager`,
 * por dos motivos: se puede probar sin móvil, y la usan los dos lados de la alarma —el que la
 * programa y el que la vuelve a programar para el día siguiente cuando salta—, así que si se
 * duplicaran acabarían avisando a horas distintas.
 *
 * El recordatorio es **diario**: cada día a la misma hora mientras dure el tratamiento. No hay
 * días sueltos todavía (ver `docs/`), así que la regla es una sola:
 *
 * - Nunca antes de [Tratamiento.fechaInicio].
 * - Nunca después de [Tratamiento.fechaFin] (si es `null`, el tratamiento es crónico y no acaba).
 * - Si la hora de hoy ya pasó, se avisa mañana.
 * - Si no hay hora o no se puede leer, no hay aviso: es preferible no avisar a avisar a una hora
 *   inventada.
 */
object RecordatoriosTratamiento {

    /**
     * Desplazamiento que se suma al id de un tratamiento para obtener el de su notificación.
     *
     * Vacunas y preventivos ya usan dos rangos propios (ver `ReprogramadorNotificaciones`) porque
     * las tres tablas numeran desde 1 y, sin separarlos, el aviso de una mascota sustituía al de
     * otra. Este es el tercer rango.
     */
    private const val DESPLAZAMIENTO_TRATAMIENTO = 2_000_000_000L

    /** Id de notificación de un tratamiento. */
    fun idNotificacion(tratamientoId: Long): Long = tratamientoId + DESPLAZAMIENTO_TRATAMIENTO

    /**
     * Lee una hora "HH:mm" y devuelve (hora, minuto), o `null` si no es una hora válida.
     *
     * Acepta "8:30" y "08:30"; rechaza "24:00", "8:60" y cualquier cosa que no sea una hora.
     */
    fun leerHora(texto: String?): Pair<Int, Int>? {
        val limpio = texto?.trim().orEmpty()
        if (limpio.isEmpty()) return null

        val partes = limpio.split(":")
        if (partes.size != 2) return null

        val hora = partes[0].toIntOrNull() ?: return null
        val minuto = partes[1].toIntOrNull() ?: return null
        if (hora !in 0..23 || minuto !in 0..59) return null

        return hora to minuto
    }

    /** Escribe una hora como "HH:mm" (con dos cifras), que es como se guarda. */
    fun formatearHora(hora: Int, minuto: Int): String =
        "${hora.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}"

    /**
     * El próximo aviso de un tratamiento a partir de [desde], o `null` si ya no toca ninguno.
     *
     * @param hora en "HH:mm", tal y como se guarda.
     * @param inicio primer día del tratamiento: no se avisa antes.
     * @param fin último día, o `null` si es crónico.
     * @param desde el momento del que se parte (normalmente "ahora").
     */
    fun proximoAviso(
        hora: String?,
        inicio: LocalDate,
        fin: LocalDate?,
        desde: LocalDateTime
    ): LocalDateTime? {
        val (h, m) = leerHora(hora) ?: return null

        // Nunca antes de empezar el tratamiento: si aún no ha llegado el día, el primer aviso es
        // ese día a la hora pedida.
        var dia = if (desde.date > inicio) desde.date else inicio

        var candidato = LocalDateTime(dia.year, dia.monthNumber, dia.dayOfMonth, h, m)
        if (candidato <= desde) {
            // La hora de hoy ya pasó (o es justo ahora): el siguiente es mañana.
            dia = LocalDate.fromEpochDays(dia.toEpochDays() + 1)
            candidato = LocalDateTime(dia.year, dia.monthNumber, dia.dayOfMonth, h, m)
        }

        if (fin != null && candidato.date > fin) return null

        return candidato
    }

    /**
     * El próximo aviso de este tratamiento, con sus propios datos. Es el atajo que usan la
     * pantalla y el aviso de arranque para no repetir los tres campos en cada sitio.
     */
    fun proximoAviso(tratamiento: Tratamiento, desde: LocalDateTime): LocalDateTime? =
        if (!tratamiento.notificar) {
            null
        } else {
            proximoAviso(
                hora = tratamiento.hora,
                inicio = tratamiento.fechaInicio,
                fin = tratamiento.fechaFin,
                desde = desde
            )
        }
}
