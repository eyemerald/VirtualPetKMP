package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento

/**
 * Las horas de las tomas de un tratamiento.
 *
 * Un tratamiento de la 1.14 en adelante puede tener varias tomas al día ("08:00,14:00,22:00"),
 * y cada una lleva su propio aviso. Los de antes de la 1.14 solo tienen [Tratamiento.hora] y se
 * tratan como una toma única, así que **hay un solo sitio** ([horasDelTratamiento]) donde se
 * decide cuántas tomas tiene un tratamiento: si esa regla se duplicara, la pantalla y el
 * programador de alarmas acabarían avisando un número distinto de veces.
 *
 * El formato de cada hora es el que ya usa la app en todas partes: **"HH:mm"** con dos cifras,
 * que es lo que acepta `RecordatoriosTratamiento` y lo que se guarda en la base de datos.
 */
object HorasDeToma {

    /** Cuántas tomas al día admite la app. El tope es de producto: más de seis no se sostiene. */
    const val MAXIMO = 6

    /** Mínimo de tomas: una al día, que es lo que había antes de la 1.14. */
    const val MINIMO = 1

    /** Separador con el que se guardan varias horas en una sola columna. */
    private const val SEPARADOR = ","

    /**
     * Horas propuestas para un número de tomas.
     *
     * Están elegidas por tramos y no calculadas con una fórmula a propósito: con una fórmula, para
     * seis tomas salen horas como "10:48", que nadie pauta. Estas se leen como un horario real y
     * el usuario puede cambiar cualquiera de ellas.
     */
    fun horasPorDefecto(vecesAlDia: Int): List<String> = when (vecesAlDia.coerceIn(MINIMO, MAXIMO)) {
        1 -> listOf("08:00")
        2 -> listOf("08:00", "20:00")
        3 -> listOf("08:00", "14:00", "22:00")
        4 -> listOf("08:00", "12:00", "16:00", "20:00")
        5 -> listOf("08:00", "11:00", "14:00", "17:00", "20:00")
        else -> listOf("06:00", "09:00", "12:00", "15:00", "18:00", "21:00")
    }

    /**
     * Lee las horas guardadas ("08:00,14:00") y devuelve las que son horas válidas, ya
     * normalizadas. Se descartan las que no lo sean en vez de fallar: un dato malo no puede
     * dejar al usuario sin el resto de sus tomas.
     */
    fun leerHoras(texto: String?): List<String> {
        val limpio = texto?.trim().orEmpty()
        if (limpio.isEmpty()) return emptyList()

        return limpio.split(SEPARADOR)
            .mapNotNull { trozo ->
                RecordatoriosTratamiento.leerHora(trozo)
                    ?.let { (hora, minuto) -> RecordatoriosTratamiento.formatearHora(hora, minuto) }
            }
    }

    /** Escribe una lista de horas en la columna, o `null` si no hay ninguna. */
    fun escribirHoras(horas: List<String>?): String? {
        val limpias = horas.orEmpty().filter { it.isNotBlank() }
        return if (limpias.isEmpty()) null else limpias.joinToString(SEPARADOR)
    }

    /**
     * Trocea el texto de la columna sin normalizar, para poder editarlo en el formulario.
     *
     * Se diferencia de [leerHoras] en que **no descarta lo que aún no es una hora**: mientras se
     * escribe "1" camino de "14:00", hay que poder seguir escribiendo.
     */
    fun desdeTexto(texto: String): List<String> =
        if (texto.isEmpty()) emptyList() else texto.split(SEPARADOR)

    /** El texto con la hora de la posición [indice] cambiada y el resto intacto. */
    fun conHoraEn(texto: String, indice: Int, valor: String): String {
        val horas = desdeTexto(texto).toMutableList()
        while (horas.size <= indice) horas.add("")
        horas[indice] = valor
        return horas.joinToString(SEPARADOR)
    }

    /** Si hay dos tomas a la misma hora, que en la práctica sería un aviso duplicado. */
    fun hayRepetidas(horas: List<String>): Boolean = horas.size != horas.toSet().size

    /**
     * Las horas a las que hay que avisar de un tratamiento: **la única regla** de cuántas tomas
     * tiene.
     *
     * - Sin recordatorio (`notificar = false`): ninguna.
     * - Con horas guardadas: esas.
     * - Sin horas (tratamiento de antes de la 1.14): la de [Tratamiento.hora], si la tiene.
     */
    fun horasDelTratamiento(tratamiento: Tratamiento): List<String> {
        if (!tratamiento.notificar) return emptyList()

        val guardadas = leerHoras(tratamiento.horas)
        if (guardadas.isNotEmpty()) return guardadas

        val unica = RecordatoriosTratamiento.leerHora(tratamiento.hora) ?: return emptyList()
        return listOf(RecordatoriosTratamiento.formatearHora(unica.first, unica.second))
    }

    /**
     * Ajusta la lista de horas al número de tomas: recorta si sobran y rellena con las de por
     * defecto si faltan. Se usa al cambiar el selector de "veces al día" en el formulario, para
     * que el usuario no tenga que escribir las seis horas cada vez que pasa de tres a cuatro.
     */
    fun ajustar(horas: List<String>, vecesAlDia: Int): List<String> {
        val objetivo = vecesAlDia.coerceIn(MINIMO, MAXIMO)
        val actuales = horas.take(objetivo)
        if (actuales.size == objetivo) return actuales

        val relleno = horasPorDefecto(objetivo).drop(actuales.size)
        return actuales + relleno
    }

    /** Lo que puede estar mal en el horario de un tratamiento, ya en forma de motivo. */
    enum class ErrorHoras {
        /** Ninguna: el horario es válido. */
        NINGUNO,

        /** Alguna toma se quedó sin hora. */
        INCOMPLETAS,

        /** Dos tomas a la misma hora. */
        REPETIDAS
    }

    /**
     * Valida el horario antes de guardar.
     *
     * Es una función pura y no código dentro del formulario para poder probarla: el formulario
     * solo tiene que traducir el motivo a un texto y enseñarlo.
     */
    fun validar(horas: List<String>, vecesAlDia: Int): ErrorHoras {
        val objetivo = vecesAlDia.coerceIn(MINIMO, MAXIMO)
        val conHora = horas.map { it.trim() }.filter { it.isNotEmpty() }

        if (conHora.size < objetivo) return ErrorHoras.INCOMPLETAS
        if (hayRepetidas(conHora)) return ErrorHoras.REPETIDAS
        return ErrorHoras.NINGUNO
    }
}
