package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.borrarArchivo
import com.example.virtualpetkmp.util.crearCarpeta
import com.example.virtualpetkmp.util.entradasDeZip
import com.example.virtualpetkmp.util.extraerDeZip
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.leerTextoDeZip
import com.example.virtualpetkmp.util.listarArchivos
import com.example.virtualpetkmp.util.nombreDeArchivo
import com.example.virtualpetkmp.util.sha256
import com.example.virtualpetkmp.util.tamanoDeEntradaZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Restaura un paquete `.vpet` **reemplazando** todo el contenido de la app.
 *
 * Es la mitad conservadora del traspaso: sirve para volver a un estado anterior o para dejar un
 * móvil nuevo como estaba el viejo. No fusiona nada —para eso está la fase de sincronización—, así
 * que lo que hubiera antes en este dispositivo desaparece **después de** guardarse solo en una
 * copia automática.
 *
 * **Cómo se protege de quedarse a medias:**
 *
 * 1. Se lee y se valida el paquete entero antes de tocar nada, incluida la huella SHA-256 del JSON.
 * 2. Se hace una copia de seguridad de lo que hay ahora (y se conservan las tres últimas).
 * 3. Los adjuntos se escriben primero en sus carpetas definitivas; si algo falla ahí, la base de
 *    datos ni se ha tocado.
 * 4. El reemplazo de la base de datos va en **una sola transacción**: o entra todo, o no entra
 *    nada. Si falla, SQLDelight deshace y los archivos recién copiados se borran.
 */
class ImportadorCopia(
    private val db: VirtualPetDatabase,
    private val carpetas: CarpetasApp,
    private val exportador: ExportadorCopia,
    /** Cuántas copias automáticas se conservan. */
    private val copiasAConservar: Int = 3
) {

    /** Lee el paquete y cuenta lo que trae, sin tocar la base de datos ni extraer nada. */
    suspend fun vistaPrevia(paquete: String): Result<VistaPreviaPaquete> = withContext(Dispatchers.IO) {
        try {
            val datos = leerDatos(paquete)
            val adjuntos = entradasDeZip(paquete).filter { it.startsWith(PaqueteDatos.CARPETA_ARCHIVOS) }

            Result.success(
                VistaPreviaPaquete(
                    mascotas = datos.mascotas.size,
                    vacunas = datos.vacunas.size,
                    preventivos = datos.preventivos.size,
                    revisiones = datos.revisiones.size,
                    tratamientos = datos.tratamientos.size,
                    pesos = datos.pesos.size,
                    informes = datos.informes.size,
                    notas = datos.notas.size,
                    veterinarios = datos.veterinarios.size,
                    archivos = adjuntos.size,
                    bytesArchivos = adjuntos.sumOf { tamanoDeEntradaZip(paquete, it).coerceAtLeast(0) },
                    nombresMascotas = datos.mascotas.map { it.nombre },
                    versionApp = datos.versionApp,
                    creadoEn = datos.creadoEn
                )
            )
        } catch (e: ErrorCopiaException) {
            Result.failure(e)
        } catch (e: Exception) {
            println("[ImportadorCopia] no se pudo leer el paquete: ${e.message}")
            Result.failure(ErrorCopiaException(ErrorCopia.PAQUETE_ILEGIBLE))
        }
    }

    suspend fun restaurar(paquete: String): Result<ResultadoRestauracion> = withContext(Dispatchers.IO) {
        val rutasEscritas = mutableListOf<String>()

        try {
            val datos = leerDatos(paquete)

            // 1. Copia de seguridad de lo que hay ahora. Si no se puede escribir, se aborta: es
            //    preferible no restaurar a hacerlo sin red.
            crearCarpeta(carpetas.copias)
            val rutaCopiaPrevia = rutaLibre(carpetas.copias, exportador.nombreCopiaAutomatica())
            val copiaPrevia = exportador.exportar(rutaCopiaPrevia).getOrNull()?.ruta

            // 2. Adjuntos a su carpeta definitiva, antes de tocar la base de datos.
            val fotoLocal = mutableMapOf<String, String>()
            val informeLocal = mutableMapOf<String, String>()

            datos.mascotas.forEach { mascota ->
                val entrada = mascota.foto ?: return@forEach
                val destino = "${carpetas.fotos}/${mascota.uuid}${extensionDe(entrada, "jpg")}"
                if (extraerDeZip(paquete, entrada, destino)) {
                    rutasEscritas += destino
                    fotoLocal[mascota.uuid] = destino
                }
            }

            datos.informes.forEach { informe ->
                val entrada = informe.archivo ?: return@forEach
                val nombreSeguro = informe.nombreArchivo
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "informe${extensionDe(entrada, "pdf")}" }
                val destino = "${carpetas.informes}/${informe.uuid}_$nombreSeguro"
                if (extraerDeZip(paquete, entrada, destino)) {
                    rutasEscritas += destino
                    informeLocal[informe.uuid] = destino
                }
            }

            // 3. Base de datos: reemplazo completo en una transacción.
            var sinMascota = 0
            try {
                db.transaction {
                    db.informesQueries.deleteAll()
                    db.notasQueries.deleteAll()
                    db.pesosQueries.deleteAll()
                    db.tratamientosQueries.deleteAll()
                    db.revisionesQueries.deleteAll()
                    db.preventivosQueries.deleteAll()
                    db.vacunasQueries.deleteAll()
                    db.mascotasQueries.deleteAll()
                    db.veterinariosQueries.deleteAll()

                    // Mascotas primero: los hijos las necesitan, y el id nuevo se resuelve por uuid.
                    val idsPorUuid = mutableMapOf<String, Long>()
                    datos.mascotas.forEach { mascota ->
                        db.mascotasQueries.insert(
                            nombre = mascota.nombre,
                            especie = mascota.especie,
                            raza = mascota.raza,
                            fechaNacimiento = mascota.fechaNacimiento,
                            sexo = mascota.sexo,
                            color = mascota.color,
                            microchip = mascota.microchip,
                            foto = fotoLocal[mascota.uuid],
                            peso_ideal = mascota.pesoIdeal,
                            uuid = mascota.uuid,
                            actualizadoEn = mascota.actualizadoEn
                        )
                        // Se busca por uuid y no con last_insert_rowid(): así no depende del driver.
                        db.mascotasQueries.selectAll().executeAsList()
                            .firstOrNull { it.uuid == mascota.uuid }
                            ?.let { idsPorUuid[mascota.uuid] = it.id }
                    }

                    datos.vacunas.forEach { vacuna ->
                        val mascotaId = idsPorUuid[vacuna.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.vacunasQueries.insert(
                                mascotaId = mascotaId,
                                nombre = vacuna.nombre,
                                fechaAplicacion = vacuna.fechaAplicacion,
                                fechaProximaDosis = vacuna.fechaProximaDosis,
                                veterinario = vacuna.veterinario,
                                lote = vacuna.lote,
                                uuid = vacuna.uuid,
                                actualizadoEn = vacuna.actualizadoEn
                            )
                        }
                    }

                    datos.preventivos.forEach { preventivo ->
                        val mascotaId = idsPorUuid[preventivo.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.preventivosQueries.insert(
                                mascotaId = mascotaId,
                                tipo = preventivo.tipo,
                                nombre = preventivo.nombre,
                                fechaAplicacion = preventivo.fechaAplicacion,
                                fechaProximaDosis = preventivo.fechaProximaDosis,
                                veterinario = preventivo.veterinario,
                                lote = preventivo.lote,
                                uuid = preventivo.uuid,
                                actualizadoEn = preventivo.actualizadoEn
                            )
                        }
                    }

                    datos.revisiones.forEach { revision ->
                        val mascotaId = idsPorUuid[revision.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.revisionesQueries.insert(
                                mascotaId = mascotaId,
                                fecha = revision.fecha,
                                motivo = revision.motivo,
                                diagnostico = revision.diagnostico,
                                notas = revision.notas,
                                veterinario = revision.veterinario,
                                uuid = revision.uuid,
                                actualizadoEn = revision.actualizadoEn
                            )
                        }
                    }

                    datos.tratamientos.forEach { tratamiento ->
                        val mascotaId = idsPorUuid[tratamiento.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.tratamientosQueries.insert(
                                mascotaId = mascotaId,
                                nombreMedicamento = tratamiento.nombreMedicamento,
                                dosis = tratamiento.dosis,
                                frecuencia = tratamiento.frecuencia,
                                fechaInicio = tratamiento.fechaInicio,
                                fechaFin = tratamiento.fechaFin,
                                hora = tratamiento.hora,
                                notificar = tratamiento.notificar.toLong(),
                                uuid = tratamiento.uuid,
                                actualizadoEn = tratamiento.actualizadoEn
                            )
                        }
                    }

                    datos.pesos.forEach { peso ->
                        val mascotaId = idsPorUuid[peso.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.pesosQueries.insert(
                                mascotaId = mascotaId,
                                fecha = peso.fecha,
                                peso = peso.peso,
                                notas = peso.notas,
                                uuid = peso.uuid,
                                actualizadoEn = peso.actualizadoEn
                            )
                        }
                    }

                    datos.informes.forEach { informe ->
                        val mascotaId = idsPorUuid[informe.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.informesQueries.insert(
                                mascotaId = mascotaId,
                                tipo = informe.tipo,
                                descripcion = informe.descripcion,
                                fecha = informe.fecha,
                                nombreArchivo = informe.nombreArchivo,
                                rutaArchivo = informeLocal[informe.uuid].orEmpty(),
                                uuid = informe.uuid,
                                actualizadoEn = informe.actualizadoEn
                            )
                        }
                    }

                    datos.notas.forEach { nota ->
                        val mascotaId = idsPorUuid[nota.mascotaUuid]
                        if (mascotaId == null) {
                            sinMascota++
                        } else {
                            db.notasQueries.insert(
                                mascotaId = mascotaId,
                                texto = nota.texto,
                                uuid = nota.uuid,
                                actualizadoEn = nota.actualizadoEn
                            )
                        }
                    }

                    datos.veterinarios.forEach { veterinario ->
                        db.veterinariosQueries.insert(
                            nombreClinica = veterinario.nombreClinica,
                            nombreVeterinario = veterinario.nombreVeterinario,
                            telefono = veterinario.telefono,
                            direccion = veterinario.direccion,
                            esUrgencias = veterinario.esUrgencias.toLong(),
                            uuid = veterinario.uuid,
                            actualizadoEn = veterinario.actualizadoEn
                        )
                    }
                }
            } catch (e: Exception) {
                // La transacción ya se deshizo: se limpian los archivos recién copiados.
                rutasEscritas.forEach { borrarArchivo(it) }
                println("[ImportadorCopia] fallo guardando: ${e.message}")
                return@withContext Result.failure(ErrorCopiaException(ErrorCopia.ERROR_GUARDANDO))
            }

            retenerCopiasAntiguas()

            Result.success(
                ResultadoRestauracion(
                    mascotas = datos.mascotas.size,
                    filas = datos.totalFilas,
                    archivosCopiados = rutasEscritas.size,
                    // Adjuntos que el paquete declaraba y no se pudieron escribir.
                    archivosQueFaltaban = maxOf(
                        0,
                        (datos.mascotas.count { it.foto != null } - fotoLocal.size) +
                            (datos.informes.count { it.archivo != null } - informeLocal.size)
                    ),
                    filasSinMascota = sinMascota,
                    copiaPrevia = copiaPrevia
                )
            )
        } catch (e: ErrorCopiaException) {
            rutasEscritas.forEach { borrarArchivo(it) }
            Result.failure(e)
        } catch (e: Exception) {
            rutasEscritas.forEach { borrarArchivo(it) }
            println("[ImportadorCopia] error inesperado: ${e.message}")
            Result.failure(ErrorCopiaException(ErrorCopia.ERROR_GUARDANDO))
        }
    }

    /**
     * Nombre libre dentro de la carpeta de copias: si el mismo nombre ya existe (dos restauraciones
     * en el mismo milisegundo), se le añade un sufijo en vez de pisar la copia anterior.
     */
    private fun rutaLibre(carpeta: String, nombre: String): String {
        var candidato = "$carpeta/$nombre"
        var intento = 2
        while (existeArchivo(candidato) && intento < 100) {
            candidato = "$carpeta/${nombre.removeSuffix(".vpet")}-$intento.vpet"
            intento++
        }
        return candidato
    }

    /** Borra las copias automáticas más antiguas y deja solo [copiasAConservar]. */
    private fun retenerCopiasAntiguas() {
        val copias = listarArchivos(carpetas.copias)
            .filter { nombreDeArchivo(it).startsWith("copia-") && it.endsWith(".vpet") }
            .sorted()
        if (copias.size <= copiasAConservar) return
        copias.dropLast(copiasAConservar).forEach { borrarArchivo(it) }
    }

    /**
     * Comprueba que el fichero es un paquete nuestro y lo devuelve leído.
     *
     * Los errores se devuelven clasificados ([ErrorCopia]) y no como texto: el mensaje final lo
     * pone la interfaz en el idioma del usuario. La huella del manifiesto es lo que detecta un
     * paquete a medio copiar (un ZIP que se queda corto es justo el fallo que se lleva los datos).
     */
    private fun leerDatos(paquete: String): PaqueteDatos {
        if (!existeArchivo(paquete)) throw ErrorCopiaException(ErrorCopia.PAQUETE_ILEGIBLE)

        val entradas = entradasDeZip(paquete)
        val textoDatos = if (entradas.isEmpty()) {
            null
        } else {
            leerTextoDeZip(paquete, PaqueteDatos.ENTRADA_DATOS)
        }
        if (textoDatos == null) {
            throw ErrorCopiaException(
                if (entradas.isEmpty()) ErrorCopia.PAQUETE_ILEGIBLE else ErrorCopia.DATOS_INCOMPLETOS
            )
        }

        val textoManifiesto = leerTextoDeZip(paquete, PaqueteDatos.ENTRADA_MANIFIESTO)
        if (textoManifiesto != null) {
            val manifiesto = runCatching {
                PaqueteDatos.json.decodeFromString(ManifiestoPaquete.serializer(), textoManifiesto)
            }.getOrElse { throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS) }

            if (manifiesto.formato != PaqueteDatos.FORMATO) {
                throw ErrorCopiaException(ErrorCopia.NO_ES_DE_VIRTUALPET)
            }
            if (manifiesto.versionFormato > PaqueteDatos.VERSION_FORMATO) {
                throw ErrorCopiaException(ErrorCopia.FORMATO_DEMASIADO_NUEVO)
            }
            if (manifiesto.hashDatos.isNotBlank() &&
                sha256(textoDatos.toByteArray(Charsets.UTF_8)) != manifiesto.hashDatos
            ) {
                throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS)
            }
        }

        val datos = runCatching {
            PaqueteDatos.json.decodeFromString(PaqueteDatos.serializer(), textoDatos)
        }.getOrElse { throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS) }

        if (datos.formato != PaqueteDatos.FORMATO) throw ErrorCopiaException(ErrorCopia.NO_ES_DE_VIRTUALPET)
        if (datos.versionFormato > PaqueteDatos.VERSION_FORMATO) {
            throw ErrorCopiaException(ErrorCopia.FORMATO_DEMASIADO_NUEVO)
        }
        if (datos.mascotas.isEmpty() && datos.veterinarios.isEmpty()) {
            throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS)
        }

        return datos
    }
}
