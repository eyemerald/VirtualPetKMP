package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate

/**
 * La regla de "esta vacuna o este preventivo está pendiente".
 *
 * **Pendiente = la próxima dosis ya pasó o cae dentro de [VENTANA_DIAS] días.** Es una sola regla
 * para los dos filtros de la lista, y vive aquí y no suelta en el SQL para que se pueda leer y
 * cambiar en un sitio: el SQL solo recibe la fecha límite ya calculada.
 *
 * La ventana es más ancha que los avisos a propósito (los avisos de vacuna saltan 15 días antes y
 * los de preventivo 1 día antes): el filtro sirve para *revisar*, no para avisar, así que incluye
 * lo que hay que mirar con tiempo.
 */
object Pendientes {

    /** Días de antelación con los que una dosis ya se considera "pendiente". */
    const val VENTANA_DIAS = 30

    /** Última fecha que cuenta como pendiente: hoy + [VENTANA_DIAS]. */
    fun limite(hoy: LocalDate): LocalDate =
        LocalDate.fromEpochDays(hoy.toEpochDays() + VENTANA_DIAS)

    /**
     * Si una próxima dosis está pendiente a día de hoy.
     *
     * Es la misma condición que aplica el SQL (`fechaProximaDosis <= límite`), escrita también en
     * Kotlin para poder probar la regla sin base de datos.
     */
    fun estaPendiente(fechaProximaDosis: LocalDate, hoy: LocalDate): Boolean =
        fechaProximaDosis <= limite(hoy)
}

/**
 * Una dosis pendiente, ya con lo que hace falta para enseñarla en la lista.
 *
 * @param nombre tal y como lo escribio el usuario en el ultimo registro ("Rabia").
 * @param fecha la proxima dosis.
 */
data class DosisPendiente(val nombre: String, val fecha: LocalDate) {

    /** Ya paso: hay que ponerla. */
    fun vencida(hoy: LocalDate): Boolean = fecha < hoy
}

/**
 * Una fila de vacuna o preventivo, reducida a lo que necesitan los filtros y la ficha.
 *
 * Se llama "ultima" a la de [fechaAplicacion] mayor: la vacuna puesta cinco veces es **una**
 * vacuna, y lo que cuenta es su ultima aplicacion.
 */
data class FilaDosis(
    val mascotaId: Long,
    val nombre: String,
    val fechaAplicacion: LocalDate,
    val fechaProximaDosis: LocalDate
)

object Dosis {

    /**
     * Nombre comparable: sin espacios de sobra, en minusculas y sin tildes.
     *
     * Es lo que hace que "Rabia", "rabia " y "RABIA" (o "RÁBIA") sean la misma vacuna. Sin esto,
     * un nombre escrito de dos formas contaba como dos vacunas distintas y el filtro enseñaba la
     * vieja como pendiente aunque estuviera ya puesta.
     *
     * Se hace a mano y no con `Normalizer` porque este codigo es comun con Android, donde
     * `Normalizer` se comporta distinto segun la version: para nombres de vacunas basta con las
     * vocales acentuadas y la enye.
     */
    fun normalizarNombre(nombre: String): String =
        nombre.trim().lowercase()
            .map { letra ->
                when (letra) {
                    'á', 'à', 'ä', 'â', 'ã', 'å' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    'ñ' -> 'n'
                    'ç' -> 'c'
                    else -> letra
                }
            }
            .joinToString("")
            .trim()

    /**
     * El ultimo registro de cada vacuna/preventivo (por nombre normalizado) de una mascota.
     *
     * Devuelve un mapa `nombre normalizado -> fila`, conservando en la fila el nombre **tal y como
     * lo escribio el usuario** para poder enseñarlo.
     */
    fun ultimasPorNombre(filas: List<FilaDosis>): Map<String, FilaDosis> {
        val porNombre = mutableMapOf<String, FilaDosis>()
        filas.forEach { fila ->
            val clave = normalizarNombre(fila.nombre)
            val actual = porNombre[clave]
            // Si hay empate en la fecha de aplicacion se queda la de proxima dosis mas lejana:
            // es la que dice cuando toca de verdad.
            val mejor = actual == null ||
                fila.fechaAplicacion > actual.fechaAplicacion ||
                (fila.fechaAplicacion == actual.fechaAplicacion && fila.fechaProximaDosis > actual.fechaProximaDosis)
            if (mejor) porNombre[clave] = fila
        }
        return porNombre
    }

    /**
     * Las dosis pendientes de una mascota: su ultimo registro por nombre, si cae dentro de la
     * ventana de [Pendientes] (vencidas o en los proximos 30 dias).
     */
    fun pendientesDe(filas: List<FilaDosis>, hoy: LocalDate): List<DosisPendiente> =
        ultimasPorNombre(filas)
            .values
            .filter { Pendientes.estaPendiente(it.fechaProximaDosis, hoy) }
            .map { DosisPendiente(nombre = it.nombre, fecha = it.fechaProximaDosis) }
            .sortedBy { it.fecha }

    /**
     * Cuenta de la ficha: vencidas y proximas sobre el **ultimo** registro de cada vacuna.
     *
     * Las ventanas se mantienen tal y como estaban (15 dias en vacunas, 5 en preventivos): lo que
     * cambia es que una vacuna repetida ya no cuenta dos veces, y una dosis vieja no sale como
     * vencida si hay una posterior.
     */
    fun contarParaFicha(filas: List<FilaDosis>, hoy: LocalDate, diasProximos: Int): Pair<Int, Int> {
        val ultimas = ultimasPorNombre(filas).values
        val vencidas = ultimas.count { it.fechaProximaDosis < hoy }
        val limite = LocalDate.fromEpochDays(hoy.toEpochDays() + diasProximos)
        val proximas = ultimas.count { it.fechaProximaDosis >= hoy && it.fechaProximaDosis <= limite }
        return vencidas to proximas
    }
}
