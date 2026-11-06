package com.example.virtualpetkmp.util

/**
 * Qué entidad hay detrás de un aviso programado.
 *
 * Va junto al `uuid` porque el `id` local **no identifica** una fila entre dos estados de la base:
 * al restaurar una copia, la misma mascota o el mismo tratamiento se vuelven a insertar con otro
 * `id`. Ver [AvisoProgramado].
 */
enum class TipoAviso {
    TRATAMIENTO,
    VACUNA,
    PREVENTIVO;

    companion object {
        /** El tipo a partir de su nombre guardado, o `null` si no se reconoce. */
        fun desdeNombre(texto: String?): TipoAviso? =
            entries.firstOrNull { it.name.equals(texto?.trim(), ignoreCase = true) }
    }
}

/**
 * Un aviso que está programado en el [ProgramadorNotificaciones], con la entidad a la que
 * pertenece.
 *
 * **Para qué existe.** Hasta la 1.14, la única forma de cancelar una alarma era recalcular su id a
 * partir de la base de datos: `cancelar(idNotificacion(tratamiento.id, toma))`. Eso deja vivas
 * todas las alarmas cuya fila **ya no está** —la mascota de prueba que se borró, el tratamiento que
 * la restauración sustituyó—, y como el aviso se reprograma solo al sonar, siguen sonando todos los
 * días para siempre. Con esta lista, la app sabe qué hay programado de verdad, sin deducirlo.
 *
 * @param notificacionId el id del `PendingIntent` y de la notificación.
 * @param tipo a qué tabla pertenece.
 * @param uuid identificador universal de la fila. Vacío solo en alarmas de versiones anteriores.
 * @param hora "HH:mm" de la toma, para los tratamientos; `null` en vacunas y preventivos.
 * @param toma índice de la toma dentro del tratamiento (`null` en dosis y en filas antiguas).
 *   Distingue las alarmas de un mismo tratamiento: posponer la de la mañana no toca la de la noche.
 * @param objetivoMillis cuándo va a sonar **de verdad** esta alarma. Normalmente es la hora de la
 *   toma de hoy, pero si el usuario la pospuso es la hora pospuesta: así el registro dice la
 *   verdad, y una posposición viva se puede comprobar (y conservar) sin deducirla de la fecha.
 */
data class AvisoProgramado(
    val notificacionId: Long,
    val tipo: TipoAviso,
    val uuid: String,
    val hora: String? = null,
    val toma: Int? = null,
    val objetivoMillis: Long? = null
)

/**
 * Lectura y escritura del registro de avisos programados.
 *
 * Es una interfaz y no un objeto global para que las pruebas puedan usar una implementación en
 * memoria: el fichero real vive en la carpeta de datos de la app, que en jvmTest no existe.
 */
interface RegistroAvisos {

    /** Los avisos que la app cree que están programados ahora mismo. */
    fun leer(): List<AvisoProgramado>

    /** Deja el registro exactamente con [avisos]. */
    fun escribir(avisos: List<AvisoProgramado>)

    /** Apunta un aviso recién programado, sustituyendo la entrada previa de ese mismo id. */
    fun apuntar(aviso: AvisoProgramado) {
        escribir(leer().filterNot { it.notificacionId == aviso.notificacionId } + aviso)
    }

    /** Quita del registro el aviso con ese id (se haya cancelado o se haya descubierto zombi). */
    fun quitar(notificacionId: Long) {
        val actuales = leer()
        val sinEse = actuales.filterNot { it.notificacionId == notificacionId }
        if (sinEse.size != actuales.size) escribir(sinEse)
    }
}

/**
 * El registro, en un fichero de texto de la carpeta de datos de la app.
 *
 * Se elige texto plano por el mismo motivo que [Posposiciones]: es estado de la app, no un dato del
 * usuario, así que no necesita una tabla ni una migración del esquema. Una línea por aviso:
 *
 * ```
 * 2000000003|TRATAMIENTO|e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1|08:00|0|1779602400000
 * 1000000002|VACUNA|c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1|||
 * ```
 *
 * Las dos últimas columnas ([AvisoProgramado.toma] y [AvisoProgramado.objetivoMillis]) son
 * opcionales: una línea escrita por la 1.14 no las tiene y se sigue leyendo bien.
 *
 * Si el fichero no existe o tiene una línea rota, se ignora esa línea: un registro corrupto no
 * puede impedir que se programen los avisos.
 */
object RegistroAvisosEnFichero : RegistroAvisos {

    private const val FICHERO = "avisos_programados.txt"
    private const val SEPARADOR = "|"

    override fun leer(): List<AvisoProgramado> = leer(carpetaDatosApp())

    override fun escribir(avisos: List<AvisoProgramado>) = escribir(avisos, carpetaDatosApp())

    /** Igual que [leer], pero sobre una carpeta concreta (para las pruebas del formato). */
    fun leer(carpeta: String): List<AvisoProgramado> {
        val texto = leerBytes("$carpeta/$FICHERO")?.toString(Charsets.UTF_8) ?: return emptyList()

        return texto.lineSequence().mapNotNull { linea ->
            val partes = linea.trim().split(SEPARADOR)
            if (partes.size < 3) return@mapNotNull null
            val id = partes[0].toLongOrNull() ?: return@mapNotNull null
            val tipo = TipoAviso.desdeNombre(partes[1]) ?: return@mapNotNull null
            val uuid = partes[2].trim()
            if (uuid.isEmpty()) return@mapNotNull null
            AvisoProgramado(
                notificacionId = id,
                tipo = tipo,
                uuid = uuid,
                hora = partes.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() },
                toma = partes.getOrNull(4)?.trim()?.toIntOrNull(),
                objetivoMillis = partes.getOrNull(5)?.trim()?.toLongOrNull()
            )
        }.toList()
    }

    /** Igual que [escribir], pero sobre una carpeta concreta. */
    fun escribir(avisos: List<AvisoProgramado>, carpeta: String) {
        crearCarpeta(carpeta)
        val texto = avisos.joinToString("\n") { aviso ->
            listOf(
                aviso.notificacionId.toString(),
                aviso.tipo.name,
                aviso.uuid,
                aviso.hora.orEmpty(),
                aviso.toma?.toString().orEmpty(),
                aviso.objetivoMillis?.toString().orEmpty()
            ).joinToString(SEPARADOR)
        }
        // Se escribe siempre, aunque quede vacío: así "borrar todo" deja el fichero limpio.
        val escrito = escribirBytes("$carpeta/$FICHERO", texto.toByteArray(Charsets.UTF_8))
        if (!escrito) println("[RegistroAvisos] no se pudo guardar en $carpeta")
    }
}

/** Registro en memoria: el de las pruebas. */
class RegistroAvisosEnMemoria(inicial: List<AvisoProgramado> = emptyList()) : RegistroAvisos {

    private var avisos: List<AvisoProgramado> = inicial

    /** Lo que hay apuntado ahora mismo, para comprobarlo desde las pruebas. */
    val actuales: List<AvisoProgramado> get() = avisos

    override fun leer(): List<AvisoProgramado> = avisos

    override fun escribir(avisos: List<AvisoProgramado>) {
        this.avisos = avisos
    }
}
