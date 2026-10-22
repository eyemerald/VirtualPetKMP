package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.escribirZip
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Lo que ha salido de una exportación. */
data class PaqueteExportado(
    /** Ruta del `.vpet` escrito. */
    val ruta: String,
    val filas: Int,
    /** Adjuntos que se han metido en el paquete. */
    val archivos: Int,
    /** Adjuntos que la base de datos menciona pero ya no estaban en el dispositivo. */
    val archivosQueFaltaban: Int,
    val bytes: Long
)

/**
 * Escribe un paquete `.vpet` con **todo** el contenido de la app: todas las mascotas, su historial
 * completo y los archivos adjuntos (fotos y PDFs) dentro.
 *
 * Es una copia de seguridad, no una transferencia selectiva: entra todo, para no tener que decidir
 * qué se queda fuera. Los adjuntos viajan **dentro** del paquete porque las rutas del disco no
 * valen en otro dispositivo: la ruta `/data/user/0/.../informes/123_analitica.pdf` del móvil no
 * existe en el ordenador.
 *
 * Los nombres de los adjuntos dentro del ZIP se derivan del `uuid` de su fila, así que el mismo
 * documento exportado dos veces acaba con el mismo nombre (lo que más adelante permitirá comparar
 * paquetes sin miedo).
 */
class ExportadorCopia(
    private val db: VirtualPetDatabase,
    private val carpetas: CarpetasApp,
    /** Versión de la app que exporta ("1.5"), solo informativa en el paquete. */
    private val versionApp: String,
    /** "android" o "escritorio", solo informativa. */
    private val plataforma: String
) {

    suspend fun exportar(destino: String): Result<PaqueteExportado> = withContext(Dispatchers.IO) {
        try {
            val archivosZip = mutableMapOf<String, String>()
            var faltaban = 0

            // --- Mascotas (con su foto) ------------------------------------------------
            val mascotas = db.mascotasQueries.selectAll().executeAsList().map { fila ->
                val fotoEnPaquete = fila.foto?.let { ruta ->
                    if (existeArchivo(ruta)) {
                        val nombre = "${PaqueteDatos.CARPETA_ARCHIVOS}fotos/${fila.uuid}${extensionDe(ruta, "jpg")}"
                        archivosZip[nombre] = ruta
                        nombre
                    } else {
                        faltaban++
                        null
                    }
                }

                FilaMascota(
                    uuid = fila.uuid,
                    actualizadoEn = fila.actualizadoEn,
                    nombre = fila.nombre,
                    especie = fila.especie,
                    raza = fila.raza,
                    fechaNacimiento = fila.fechaNacimiento,
                    sexo = fila.sexo,
                    color = fila.color,
                    microchip = fila.microchip,
                    foto = fotoEnPaquete,
                    pesoIdeal = fila.peso_ideal
                )
            }
            val uuidPorId = db.mascotasQueries.selectAll().executeAsList().associate { it.id to it.uuid }

            val vacunas = db.vacunasQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaVacuna(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        nombre = fila.nombre,
                        fechaAplicacion = fila.fechaAplicacion,
                        fechaProximaDosis = fila.fechaProximaDosis,
                        veterinario = fila.veterinario,
                        lote = fila.lote
                    )
                }
            }

            val preventivos = db.preventivosQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaPreventivo(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        tipo = fila.tipo,
                        nombre = fila.nombre,
                        fechaAplicacion = fila.fechaAplicacion,
                        fechaProximaDosis = fila.fechaProximaDosis,
                        veterinario = fila.veterinario,
                        lote = fila.lote
                    )
                }
            }

            val revisiones = db.revisionesQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaRevision(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        fecha = fila.fecha,
                        motivo = fila.motivo,
                        diagnostico = fila.diagnostico,
                        notas = fila.notas,
                        veterinario = fila.veterinario
                    )
                }
            }

            val tratamientos = db.tratamientosQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaTratamiento(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        nombreMedicamento = fila.nombreMedicamento,
                        dosis = fila.dosis,
                        frecuencia = fila.frecuencia,
                        fechaInicio = fila.fechaInicio,
                        fechaFin = fila.fechaFin,
                        hora = fila.hora,
                        notificar = fila.notificar.toInt()
                    )
                }
            }

            val pesos = db.pesosQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaPeso(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        fecha = fila.fecha,
                        peso = fila.peso,
                        notas = fila.notas
                    )
                }
            }

            val informes = db.informesQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    val archivoEnPaquete = if (existeArchivo(fila.rutaArchivo)) {
                        val nombre = "${PaqueteDatos.CARPETA_ARCHIVOS}informes/${fila.uuid}" +
                            extensionDe(fila.nombreArchivo.ifBlank { fila.rutaArchivo }, "pdf")
                        archivosZip[nombre] = fila.rutaArchivo
                        nombre
                    } else {
                        faltaban++
                        null
                    }

                    FilaInforme(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        tipo = fila.tipo,
                        descripcion = fila.descripcion,
                        fecha = fila.fecha,
                        nombreArchivo = fila.nombreArchivo,
                        archivo = archivoEnPaquete
                    )
                }
            }

            val notas = db.notasQueries.selectAll().executeAsList().mapNotNull { fila ->
                uuidPorId[fila.mascotaId]?.let { mascotaUuid ->
                    FilaNota(
                        uuid = fila.uuid,
                        actualizadoEn = fila.actualizadoEn,
                        mascotaUuid = mascotaUuid,
                        texto = fila.texto
                    )
                }
            }

            val veterinarios = db.veterinariosQueries.selectAll().executeAsList().map { fila ->
                FilaVeterinario(
                    uuid = fila.uuid,
                    actualizadoEn = fila.actualizadoEn,
                    nombreClinica = fila.nombreClinica,
                    nombreVeterinario = fila.nombreVeterinario,
                    telefono = fila.telefono,
                    direccion = fila.direccion,
                    esUrgencias = fila.esUrgencias.toInt()
                )
            }

            val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val datos = PaqueteDatos(
                versionApp = versionApp,
                esquema = VirtualPetDatabase.Schema.version,
                creadoEn = ahora.toString(),
                plataforma = plataforma,
                mascotas = mascotas,
                vacunas = vacunas,
                preventivos = preventivos,
                revisiones = revisiones,
                tratamientos = tratamientos,
                pesos = pesos,
                informes = informes,
                notas = notas,
                veterinarios = veterinarios
            )

            val textoDatos = PaqueteDatos.json.encodeToString(PaqueteDatos.serializer(), datos)
            val manifiesto = ManifiestoPaquete(
                versionApp = versionApp,
                esquema = VirtualPetDatabase.Schema.version,
                creadoEn = ahora.toString(),
                plataforma = plataforma,
                archivos = archivosZip.size,
                // Huella del JSON: al restaurar se comprueba que el paquete no llegó a medias.
                hashDatos = sha256(textoDatos.toByteArray(Charsets.UTF_8))
            )
            val textoManifiesto = PaqueteDatos.json.encodeToString(ManifiestoPaquete.serializer(), manifiesto)

            val escrito = escribirZip(
                destino = destino,
                textos = mapOf(
                    PaqueteDatos.ENTRADA_MANIFIESTO to textoManifiesto,
                    PaqueteDatos.ENTRADA_DATOS to textoDatos
                ),
                archivos = archivosZip
            )

            if (!escrito) {
                return@withContext Result.failure(ErrorCopiaException(ErrorCopia.ERROR_ESCRIBIENDO))
            }

            Result.success(
                PaqueteExportado(
                    ruta = destino,
                    filas = datos.totalFilas,
                    archivos = archivosZip.size,
                    archivosQueFaltaban = faltaban,
                    bytes = leerTamano(destino)
                )
            )
        } catch (e: Exception) {
            println("[ExportadorCopia] error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Nombre de una copia de seguridad automática, ordenable alfabéticamente por fecha.
     *
     * Lleva los milisegundos porque dos restauraciones seguidas caen en el mismo segundo: sin
     * ellos, la segunda copia pisaba a la primera y la red de seguridad se quedaba en una sola.
     */
    fun nombreCopiaAutomatica(millis: Long = ahoraMillis()): String {
        val momento = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        fun dos(valor: Int) = valor.toString().padStart(2, '0')
        fun tres(valor: Int) = valor.toString().padStart(3, '0')
        return "copia-${momento.year}${dos(momento.monthNumber)}${dos(momento.dayOfMonth)}" +
            "-${dos(momento.hour)}${dos(momento.minute)}${dos(momento.second)}" +
            "-${tres(momento.nanosecond / 1_000_000)}.vpet"
    }

    private fun leerTamano(ruta: String): Long = com.example.virtualpetkmp.util.tamanoArchivo(ruta)
}

/**
 * Extensión con punto (".pdf") a partir de un nombre o de una ruta. Si no hay ninguna, la de
 * reserva: un PDF sin extensión no se podría abrir después.
 */
internal fun extensionDe(nombreORuta: String, porDefecto: String): String {
    val limpio = nombreORuta.substringBefore('?').trim()
    val punto = limpio.lastIndexOf('.')
    if (punto <= 0 || punto == limpio.length - 1) return ".$porDefecto"
    val extension = limpio.substring(punto + 1).lowercase()
    return if (extension.length in 1..5 && extension.all { it.isLetterOrDigit() }) ".$extension" else ".$porDefecto"
}
