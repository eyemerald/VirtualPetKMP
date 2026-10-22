package com.example.virtualpetkmp.util.copia

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lo que va dentro de un paquete de datos (`.vpet`), en `datos.json`.
 *
 * El paquete es un ZIP con tres cosas: este fichero, el `manifiesto.json` (qué app y qué versión
 * lo escribió) y una carpeta `archivos/` con las fotos y los PDFs. Los dos JSON se pueden abrir
 * con cualquier editor de texto: si algún día hay que recuperar datos a mano, se puede.
 *
 * **Las relaciones entre filas se guardan por `uuid`, no por `id`.** El `id` es un autonumérico
 * local: al restaurar en otro dispositivo los ids se recalculan, así que una vacuna que apunta a
 * `mascotaId = 1` podría acabar colgando de otra mascota. Con `mascotaUuid` no puede pasar.
 *
 * **Los archivos no se guardan por ruta.** La ruta absoluta del móvil (`/data/user/0/...`) no
 * existe en el ordenador ni en otro móvil. En el paquete cada archivo se referencia por su nombre
 * dentro del ZIP, y al restaurar se vuelve a escribir en la carpeta local correspondiente.
 */
@Serializable
data class PaqueteDatos(
    val formato: String = FORMATO,
    val versionFormato: Int = VERSION_FORMATO,
    val app: String = "VirtualPet",
    /** Versión de la app que escribió el paquete ("1.5"). Informativo. */
    val versionApp: String = "",
    /** Versión del esquema de base de datos que había al exportar (14). */
    val esquema: Long = 0,
    /** Momento del export, en ISO-8601. Informativo. */
    val creadoEn: String = "",
    /** "android" o "escritorio". Informativo. */
    val plataforma: String = "",
    val mascotas: List<FilaMascota> = emptyList(),
    val vacunas: List<FilaVacuna> = emptyList(),
    val preventivos: List<FilaPreventivo> = emptyList(),
    val revisiones: List<FilaRevision> = emptyList(),
    val tratamientos: List<FilaTratamiento> = emptyList(),
    val pesos: List<FilaPeso> = emptyList(),
    val informes: List<FilaInforme> = emptyList(),
    val notas: List<FilaNota> = emptyList(),
    val veterinarios: List<FilaVeterinario> = emptyList()
) {
    /** Cuántas filas trae, sin contar los archivos. Sirve para la vista previa y los mensajes. */
    val totalFilas: Int
        get() = mascotas.size + vacunas.size + preventivos.size + revisiones.size +
            tratamientos.size + pesos.size + informes.size + notas.size + veterinarios.size

    companion object {
        /** Marca del formato. Sirve para rechazar un ZIP que no sea nuestro. */
        const val FORMATO = "virtualpet-datos"

        /** Versión del formato en sí (no de la app). Sube si cambia la estructura del JSON. */
        const val VERSION_FORMATO = 1

        /** Nombre del fichero de datos dentro del ZIP. */
        const val ENTRADA_DATOS = "datos.json"

        /** Nombre del manifiesto dentro del ZIP. */
        const val ENTRADA_MANIFIESTO = "manifiesto.json"

        /** Prefijo de la carpeta de adjuntos dentro del ZIP. */
        const val CARPETA_ARCHIVOS = "archivos/"

        /**
         * Lector tolerante: un campo nuevo que este código no conozca **no** rompe la lectura.
         * Es lo que permitirá abrir un paquete creado por una versión futura sin perder el resto.
         */
        val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }
}

/** Manifiesto: lo mínimo para reconocer el paquete antes de intentar leer los datos. */
@Serializable
data class ManifiestoPaquete(
    val formato: String = PaqueteDatos.FORMATO,
    val versionFormato: Int = PaqueteDatos.VERSION_FORMATO,
    val app: String = "VirtualPet",
    val versionApp: String = "",
    val esquema: Long = 0,
    val creadoEn: String = "",
    val plataforma: String = "",
    /** Número de archivos adjuntos, para poder avisar antes de extraer. */
    val archivos: Int = 0,
    val hashDatos: String = ""
)

@Serializable
data class FilaMascota(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val nombre: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: String,
    val sexo: String,
    val color: String,
    val microchip: String? = null,
    /** Nombre del archivo dentro del paquete (`archivos/fotos/...`), no una ruta del móvil. */
    val foto: String? = null,
    val pesoIdeal: Double? = null
)

@Serializable
data class FilaVacuna(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    /** Identificador universal de la mascota a la que pertenece. */
    val mascotaUuid: String,
    val nombre: String,
    val fechaAplicacion: String,
    val fechaProximaDosis: String,
    val veterinario: String? = null,
    val lote: String? = null
)

@Serializable
data class FilaPreventivo(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val tipo: String,
    val nombre: String,
    val fechaAplicacion: String,
    val fechaProximaDosis: String,
    val veterinario: String? = null,
    val lote: String? = null
)

@Serializable
data class FilaRevision(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val fecha: String,
    val motivo: String,
    val diagnostico: String? = null,
    val notas: String? = null,
    val veterinario: String? = null
)

@Serializable
data class FilaTratamiento(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val nombreMedicamento: String,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val hora: String? = null,
    val notificar: Int = 0
)

@Serializable
data class FilaPeso(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val fecha: String,
    val peso: Double,
    val notas: String? = null
)

@Serializable
data class FilaInforme(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val tipo: String,
    val descripcion: String? = null,
    val fecha: String,
    /** Nombre original del documento ("analitica.pdf"), que es lo que ve el usuario. */
    val nombreArchivo: String,
    /** Nombre del archivo dentro del paquete, o `null` si el documento ya no estaba en disco. */
    val archivo: String? = null
)

@Serializable
data class FilaNota(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val mascotaUuid: String,
    val texto: String
)

@Serializable
data class FilaVeterinario(
    @SerialName("uuid") val uuid: String,
    val actualizadoEn: Long = 0,
    val nombreClinica: String,
    val nombreVeterinario: String? = null,
    val telefono: String,
    val direccion: String,
    val esUrgencias: Int = 0
)

/** Resumen de lo que trae un paquete, para enseñarlo antes de tocar la base de datos. */
data class VistaPreviaPaquete(
    val mascotas: Int,
    val vacunas: Int,
    val preventivos: Int,
    val revisiones: Int,
    val tratamientos: Int,
    val pesos: Int,
    val informes: Int,
    val notas: Int,
    val veterinarios: Int,
    val archivos: Int,
    val bytesArchivos: Long,
    val nombresMascotas: List<String>,
    val versionApp: String,
    val creadoEn: String
) {
    val totalFilas: Int
        get() = mascotas + vacunas + preventivos + revisiones + tratamientos +
            pesos + informes + notas + veterinarios
}

/** Lo que ha hecho una restauración, para poder contarlo al usuario. */
data class ResultadoRestauracion(
    val mascotas: Int,
    val filas: Int,
    val archivosCopiados: Int,
    /** Adjuntos que el paquete decía traer pero no se pudieron escribir. */
    val archivosQueFaltaban: Int = 0,
    /** Filas hijas cuyo `mascotaUuid` no estaba en el paquete: se descartan, no se inventan. */
    val filasSinMascota: Int = 0,
    /** Ruta de la copia de seguridad que se hizo automáticamente antes de restaurar. */
    val copiaPrevia: String? = null
)

/** Errores que puede devolver la lectura o la restauración, ya clasificados. */
enum class ErrorCopia {
    /** El fichero elegido no es un ZIP o está dañado. */
    PAQUETE_ILEGIBLE,

    /** Es un ZIP, pero no lo escribió esta app. */
    NO_ES_DE_VIRTUALPET,

    /** Lo escribió una versión del formato más nueva de la que entiende esta app. */
    FORMATO_DEMASIADO_NUEVO,

    /** Falta `datos.json` o el JSON no se puede leer. */
    DATOS_INCOMPLETOS,

    /** Falló la escritura de algún archivo del paquete en el dispositivo. */
    ERROR_ESCRIBIENDO,

    /** Falló la escritura en la base de datos: no se ha cambiado nada. */
    ERROR_GUARDANDO
}

class ErrorCopiaException(val error: ErrorCopia) : Exception(error.name)

/** Serializador del manifiesto, expuesto para las pruebas y para el exportador. */
internal fun serializadorManifiesto(): KSerializer<ManifiestoPaquete> = ManifiestoPaquete.serializer()
