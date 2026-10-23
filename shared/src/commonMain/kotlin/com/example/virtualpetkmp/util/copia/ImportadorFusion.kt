package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.CarpetasApp
import com.example.virtualpetkmp.util.borrarArchivo
import com.example.virtualpetkmp.util.crearCarpeta
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.extraerDeZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Qué hacer con una mascota del paquete cuyo nombre ya existe en este dispositivo. */
enum class DecisionConflicto {
    /** Importarla como otra mascota, con un nombre libre ("Luna (2)"). Es la opción por defecto. */
    RENOMBRAR,

    /** Es la misma mascota creada a mano en los dos sitios: quedarse con sus datos, manteniendo la local. */
    SOBRESCRIBIR,

    /** No traerla (ni ella ni su historial). */
    OMITIR
}

/** Una mascota del paquete que choca por nombre con una de este dispositivo. */
data class ConflictoMascota(
    val uuidPaquete: String,
    val nombrePaquete: String,
    val uuidLocal: String,
    val nombreLocal: String
)

/** Lo que pasaría si se aplicara el paquete. Se enseña antes de tocar nada. */
data class PlanFusion(
    val vista: VistaPreviaPaquete,
    val mascotasNuevas: Int,
    val mascotasAModificar: Int,
    val mascotasIguales: Int,
    val filasNuevas: Int,
    val filasAModificar: Int,
    val filasIguales: Int,
    val conflictos: List<ConflictoMascota>
) {
    val hayCambios: Boolean get() = mascotasNuevas + mascotasAModificar + filasNuevas + filasAModificar > 0
}

/** Lo que se ha hecho de verdad al aplicar el plan. */
data class ResultadoFusion(
    val mascotasNuevas: Int,
    val mascotasActualizadas: Int,
    val filasNuevas: Int,
    val filasActualizadas: Int,
    val archivosCopiados: Int,
    val conflictosOmitidos: Int,
    /** Copia de seguridad que se hizo antes de fusionar, para poder volver atrás a mano. */
    val copiaPrevia: String?
)

/**
 * Fusiona un paquete `.vpet` con lo que ya hay en este dispositivo.
 *
 * **Es lo contrario de restaurar:** no reemplaza nada. Cada fila se reconoce por su `uuid` y:
 *
 * - si no existe aquí, se **añade**;
 * - si existe y la del paquete es **más nueva** (por `actualizadoEn`), se actualiza;
 * - si existe y es más vieja (o igual), se deja como está: importar un paquete antiguo **no revierte**
 *   nada.
 *
 * **Nunca se borra nada**, y por eso la fusión es apta para "llevo la misma mascota en el móvil y en
 * el ordenador": los dos lados acaban con la unión de lo que cada uno tenía.
 *
 * El único caso que la máquina no puede decidir sola es un **nombre repetido con identidad distinta**:
 * dos mascotas creadas por separado en cada dispositivo se llaman igual pero no son la misma, así que
 * se pregunta ([DecisionConflicto]).
 *
 * Todo se aplica en **una transacción**: o entra el paquete entero o no entra nada.
 */
class ImportadorFusion(
    private val db: VirtualPetDatabase,
    private val carpetas: CarpetasApp,
    private val exportador: ExportadorCopia
) {

    /** Lee el paquete y calcula el plan sin escribir ni extraer nada. */
    suspend fun analizar(paquete: String): Result<PlanFusion> = withContext(Dispatchers.IO) {
        try {
            val datos = LectorPaquete.leer(paquete)
            Result.success(planificar(paquete, datos))
        } catch (e: ErrorCopiaException) {
            Result.failure(e)
        } catch (e: Exception) {
            println("[ImportadorFusion] no se pudo analizar: ${e.message}")
            Result.failure(ErrorCopiaException(ErrorCopia.PAQUETE_ILEGIBLE))
        }
    }

    /**
     * Aplica el paquete fusionándolo.
     *
     * @param decisiones qué hacer con cada conflicto de nombre, por `uuid` de la mascota del paquete.
     *   Las que no vengan se tratan como [DecisionConflicto.RENOMBRAR].
     */
    suspend fun aplicar(
        paquete: String,
        decisiones: Map<String, DecisionConflicto> = emptyMap()
    ): Result<ResultadoFusion> = withContext(Dispatchers.IO) {
        val rutasEscritas = mutableListOf<String>()

        try {
            val datos = LectorPaquete.leer(paquete)

            // 1. Copia de seguridad de lo que hay ahora (por si hay que volver atrás a mano).
            crearCarpeta(carpetas.copias)
            val rutaCopia = rutaCopiaLibre(exportador.nombreCopiaAutomatica())
            val copiaPrevia = exportador.exportar(rutaCopia).getOrNull()?.ruta

            // 2. Estado local, para decidir fila a fila.
            val local = EstadoLocal(db)

            // 3. Mascotas: qué se añade, qué se actualiza y qué conflictos hay.
            val mascotasNuevas = mutableListOf<Pair<FilaMascota, String>>() // fila + nombre final
            val mascotasActualizar = mutableListOf<Pair<Long, FilaMascota>>()
            val destinoPorUuid = mutableMapOf<String, Long>() // uuid del paquete -> id local
            var mascotasIguales = 0
            var omitidas = 0
            // Nombres ya ocupados: los locales y los que se van añadiendo en esta misma fusión.
            val nombresUsados = local.mascotasPorNombre.keys.toMutableSet()

            datos.mascotas.forEach { mascota ->
                val filaLocal = local.mascotasPorUuid[mascota.uuid]
                if (filaLocal != null) {
                    destinoPorUuid[mascota.uuid] = filaLocal.id
                    if (mascota.actualizadoEn > filaLocal.actualizadoEn) {
                        mascotasActualizar += filaLocal.id to mascota
                    } else {
                        mascotasIguales++
                    }
                } else {
                    val localConMismoNombre = local.mascotasPorNombre[mascota.nombre.lowercase()]
                    if (localConMismoNombre == null) {
                        nombresUsados.add(mascota.nombre.lowercase())
                        mascotasNuevas += mascota to mascota.nombre
                    } else {
                        when (decisiones[mascota.uuid] ?: DecisionConflicto.RENOMBRAR) {
                            DecisionConflicto.RENOMBRAR ->
                                mascotasNuevas += mascota to nombreLibre(mascota.nombre, nombresUsados)

                            DecisionConflicto.SOBRESCRIBIR -> {
                                // Se queda la mascota local (su id y su uuid) con los datos del
                                // paquete: es "la misma" creada en los dos sitios.
                                destinoPorUuid[mascota.uuid] = localConMismoNombre.id
                                mascotasActualizar += localConMismoNombre.id to mascota
                            }

                            DecisionConflicto.OMITIR -> omitidas++
                        }
                    }
                }
            }

            // 4. Hijas: por uuid, y la mascota destino ya está resuelta arriba.
            val vacunasNuevas = datos.vacunas.filter { local.vacunas[it.uuid] == null }
            val vacunasActualizar = datos.vacunas.filter { fila ->
                local.vacunasEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val preventivosNuevos = datos.preventivos.filter { local.preventivos[it.uuid] == null }
            val preventivosActualizar = datos.preventivos.filter { fila ->
                local.preventivosEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val revisionesNuevas = datos.revisiones.filter { local.revisiones[it.uuid] == null }
            val revisionesActualizar = datos.revisiones.filter { fila ->
                local.revisionesEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val tratamientosNuevos = datos.tratamientos.filter { local.tratamientos[it.uuid] == null }
            val tratamientosActualizar = datos.tratamientos.filter { fila ->
                local.tratamientosEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val pesosNuevos = datos.pesos.filter { local.pesos[it.uuid] == null }
            val pesosActualizar = datos.pesos.filter { fila ->
                local.pesosEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val informesNuevos = datos.informes.filter { local.informes[it.uuid] == null }
            val informesActualizar = datos.informes.filter { fila ->
                local.informesEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val notasNuevas = datos.notas.filter { local.notas[it.uuid] == null }
            val notasActualizar = datos.notas.filter { fila ->
                local.notasEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }
            val veterinariosNuevos = datos.veterinarios.filter { local.veterinarios[it.uuid] == null }
            val veterinariosActualizar = datos.veterinarios.filter { fila ->
                local.veterinariosEn[fila.uuid]?.let { fila.actualizadoEn > it } == true
            }

            // 5. Adjuntos que hay que escribir: los de las filas nuevas, y los que falten en disco en
            //    las filas que se van a actualizar.
            val fotoLocal = mutableMapOf<String, String>()
            val informeLocal = mutableMapOf<String, String>()

            mascotasNuevas.forEach { (mascota, _) ->
                extractarFoto(paquete, mascota, fotoLocal, rutasEscritas)
            }
            mascotasActualizar.forEach { (idLocal, mascota) ->
                val rutaActual = local.fotoDeMascota[idLocal]
                if (rutaActual == null || !existeArchivo(rutaActual)) {
                    extractarFoto(paquete, mascota, fotoLocal, rutasEscritas)
                }
            }
            informesNuevos.forEach { informe ->
                extractarInforme(paquete, informe, informeLocal, rutasEscritas)
            }
            informesActualizar.forEach { informe ->
                val rutaActual = local.rutaDeInforme[informe.uuid]
                if (rutaActual == null || !existeArchivo(rutaActual)) {
                    extractarInforme(paquete, informe, informeLocal, rutasEscritas)
                }
            }

            // 6. Una transacción para todo.
            try {
                db.transaction {
                    mascotasNuevas.forEach { (mascota, nombreFinal) ->
                        db.mascotasQueries.insert(
                            nombre = nombreFinal,
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
                        db.mascotasQueries.selectAll().executeAsList()
                            .firstOrNull { it.uuid == mascota.uuid }
                            ?.let { destinoPorUuid[mascota.uuid] = it.id }
                    }

                    mascotasActualizar.forEach { (idLocal, mascota) ->
                        db.mascotasQueries.update(
                            nombre = mascota.nombre,
                            especie = mascota.especie,
                            raza = mascota.raza,
                            fechaNacimiento = mascota.fechaNacimiento,
                            sexo = mascota.sexo,
                            color = mascota.color,
                            microchip = mascota.microchip,
                            foto = fotoLocal[mascota.uuid] ?: local.fotoDeMascota[idLocal],
                            peso_ideal = mascota.pesoIdeal,
                            actualizadoEn = mascota.actualizadoEn,
                            id = idLocal
                        )
                    }

                    vacunasNuevas.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.vacunasQueries.insert(
                                mascotaId = mascotaId,
                                nombre = fila.nombre,
                                fechaAplicacion = fila.fechaAplicacion,
                                fechaProximaDosis = fila.fechaProximaDosis,
                                veterinario = fila.veterinario,
                                lote = fila.lote,
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    vacunasActualizar.forEach { fila ->
                        local.vacunas[fila.uuid]?.let { idLocal ->
                            db.vacunasQueries.update(
                                nombre = fila.nombre,
                                fechaAplicacion = fila.fechaAplicacion,
                                fechaProximaDosis = fila.fechaProximaDosis,
                                veterinario = fila.veterinario,
                                lote = fila.lote,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    preventivosNuevos.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.preventivosQueries.insert(
                                mascotaId = mascotaId,
                                tipo = fila.tipo,
                                nombre = fila.nombre,
                                fechaAplicacion = fila.fechaAplicacion,
                                fechaProximaDosis = fila.fechaProximaDosis,
                                veterinario = fila.veterinario,
                                lote = fila.lote,
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    preventivosActualizar.forEach { fila ->
                        local.preventivos[fila.uuid]?.let { idLocal ->
                            db.preventivosQueries.update(
                                tipo = fila.tipo,
                                nombre = fila.nombre,
                                fechaAplicacion = fila.fechaAplicacion,
                                fechaProximaDosis = fila.fechaProximaDosis,
                                veterinario = fila.veterinario,
                                lote = fila.lote,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    revisionesNuevas.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.revisionesQueries.insert(
                                mascotaId = mascotaId,
                                fecha = fila.fecha,
                                motivo = fila.motivo,
                                diagnostico = fila.diagnostico,
                                notas = fila.notas,
                                veterinario = fila.veterinario,
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    revisionesActualizar.forEach { fila ->
                        local.revisiones[fila.uuid]?.let { idLocal ->
                            db.revisionesQueries.update(
                                fecha = fila.fecha,
                                motivo = fila.motivo,
                                diagnostico = fila.diagnostico,
                                notas = fila.notas,
                                veterinario = fila.veterinario,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    tratamientosNuevos.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.tratamientosQueries.insert(
                                mascotaId = mascotaId,
                                nombreMedicamento = fila.nombreMedicamento,
                                dosis = fila.dosis,
                                frecuencia = fila.frecuencia,
                                fechaInicio = fila.fechaInicio,
                                fechaFin = fila.fechaFin,
                                hora = fila.hora,
                                notificar = fila.notificar.toLong(),
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    tratamientosActualizar.forEach { fila ->
                        local.tratamientos[fila.uuid]?.let { idLocal ->
                            db.tratamientosQueries.update(
                                nombreMedicamento = fila.nombreMedicamento,
                                dosis = fila.dosis,
                                frecuencia = fila.frecuencia,
                                fechaInicio = fila.fechaInicio,
                                fechaFin = fila.fechaFin,
                                hora = fila.hora,
                                notificar = fila.notificar.toLong(),
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    pesosNuevos.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.pesosQueries.insert(
                                mascotaId = mascotaId,
                                fecha = fila.fecha,
                                peso = fila.peso,
                                notas = fila.notas,
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    pesosActualizar.forEach { fila ->
                        local.pesos[fila.uuid]?.let { idLocal ->
                            db.pesosQueries.update(
                                fecha = fila.fecha,
                                peso = fila.peso,
                                notas = fila.notas,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    informesNuevos.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.informesQueries.insert(
                                mascotaId = mascotaId,
                                tipo = fila.tipo,
                                descripcion = fila.descripcion,
                                fecha = fila.fecha,
                                nombreArchivo = fila.nombreArchivo,
                                rutaArchivo = informeLocal[fila.uuid].orEmpty(),
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    informesActualizar.forEach { fila ->
                        local.informes[fila.uuid]?.let { idLocal ->
                            db.informesQueries.update(
                                tipo = fila.tipo,
                                descripcion = fila.descripcion,
                                fecha = fila.fecha,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                            // La consulta de arriba no toca la ruta del archivo: si el paquete trae
                            // uno y aquí faltaba, se reapunta para que el documento vuelva a abrirse.
                            informeLocal[fila.uuid]?.let { rutaNueva ->
                                db.informesQueries.updateArchivo(rutaArchivo = rutaNueva, id = idLocal)
                            }
                        }
                    }

                    notasNuevas.forEach { fila ->
                        destinoPorUuid[fila.mascotaUuid]?.let { mascotaId ->
                            db.notasQueries.insert(
                                mascotaId = mascotaId,
                                texto = fila.texto,
                                uuid = fila.uuid,
                                actualizadoEn = fila.actualizadoEn
                            )
                        }
                    }
                    notasActualizar.forEach { fila ->
                        local.notas[fila.uuid]?.let { idLocal ->
                            db.notasQueries.update(
                                texto = fila.texto,
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }

                    veterinariosNuevos.forEach { fila ->
                        db.veterinariosQueries.insert(
                            nombreClinica = fila.nombreClinica,
                            nombreVeterinario = fila.nombreVeterinario,
                            telefono = fila.telefono,
                            direccion = fila.direccion,
                            esUrgencias = fila.esUrgencias.toLong(),
                            uuid = fila.uuid,
                            actualizadoEn = fila.actualizadoEn
                        )
                    }
                    veterinariosActualizar.forEach { fila ->
                        local.veterinarios[fila.uuid]?.let { idLocal ->
                            db.veterinariosQueries.update(
                                nombreClinica = fila.nombreClinica,
                                nombreVeterinario = fila.nombreVeterinario,
                                telefono = fila.telefono,
                                direccion = fila.direccion,
                                esUrgencias = fila.esUrgencias.toLong(),
                                actualizadoEn = fila.actualizadoEn,
                                id = idLocal
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                rutasEscritas.forEach { borrarArchivo(it) }
                println("[ImportadorFusion] fallo guardando: ${e.message}")
                return@withContext Result.failure(ErrorCopiaException(ErrorCopia.ERROR_GUARDANDO))
            }

            Result.success(
                ResultadoFusion(
                    mascotasNuevas = mascotasNuevas.size,
                    mascotasActualizadas = mascotasActualizar.size,
                    filasNuevas = mascotasNuevas.size + vacunasNuevas.size + preventivosNuevos.size +
                        revisionesNuevas.size + tratamientosNuevos.size + pesosNuevos.size +
                        informesNuevos.size + notasNuevas.size + veterinariosNuevos.size,
                    filasActualizadas = mascotasActualizar.size + vacunasActualizar.size +
                        preventivosActualizar.size + revisionesActualizar.size +
                        tratamientosActualizar.size + pesosActualizar.size +
                        informesActualizar.size + notasActualizar.size + veterinariosActualizar.size,
                    archivosCopiados = rutasEscritas.size,
                    conflictosOmitidos = omitidas,
                    copiaPrevia = copiaPrevia
                )
            )
        } catch (e: ErrorCopiaException) {
            rutasEscritas.forEach { borrarArchivo(it) }
            Result.failure(e)
        } catch (e: Exception) {
            rutasEscritas.forEach { borrarArchivo(it) }
            println("[ImportadorFusion] error inesperado: ${e.message}")
            Result.failure(ErrorCopiaException(ErrorCopia.ERROR_GUARDANDO))
        }
    }

    // --- Plan ---------------------------------------------------------------------------------

    private fun planificar(paquete: String, datos: PaqueteDatos): PlanFusion {
        val local = EstadoLocal(db)

        var mascotasNuevas = 0
        var mascotasAModificar = 0
        var mascotasIguales = 0
        val conflictos = mutableListOf<ConflictoMascota>()

        datos.mascotas.forEach { mascota ->
            val existente = local.mascotasPorUuid[mascota.uuid]
            when {
                existente != null -> if (mascota.actualizadoEn > existente.actualizadoEn) {
                    mascotasAModificar++
                } else {
                    mascotasIguales++
                }

                else -> {
                    val mismoNombre = local.mascotasPorNombre[mascota.nombre.lowercase()]
                    if (mismoNombre == null) {
                        mascotasNuevas++
                    } else {
                        conflictos += ConflictoMascota(
                            uuidPaquete = mascota.uuid,
                            nombrePaquete = mascota.nombre,
                            uuidLocal = mismoNombre.uuid,
                            nombreLocal = mismoNombre.nombre
                        )
                    }
                }
            }
        }

        fun nuevasDe(uuids: Map<String, Long>, filas: List<Pair<String, Long>>): Int =
            filas.count { uuids[it.first] == null }

        fun modificadasDe(momentos: Map<String, Long>, filas: List<Pair<String, Long>>): Int =
            filas.count { (uuid, momento) -> momentos[uuid]?.let { momento > it } == true }

        val vacunas = datos.vacunas.map { it.uuid to it.actualizadoEn }
        val preventivos = datos.preventivos.map { it.uuid to it.actualizadoEn }
        val revisiones = datos.revisiones.map { it.uuid to it.actualizadoEn }
        val tratamientos = datos.tratamientos.map { it.uuid to it.actualizadoEn }
        val pesos = datos.pesos.map { it.uuid to it.actualizadoEn }
        val informes = datos.informes.map { it.uuid to it.actualizadoEn }
        val notas = datos.notas.map { it.uuid to it.actualizadoEn }
        val veterinarios = datos.veterinarios.map { it.uuid to it.actualizadoEn }

        val filasNuevas = nuevasDe(local.vacunas, vacunas) + nuevasDe(local.preventivos, preventivos) +
            nuevasDe(local.revisiones, revisiones) + nuevasDe(local.tratamientos, tratamientos) +
            nuevasDe(local.pesos, pesos) + nuevasDe(local.informes, informes) +
            nuevasDe(local.notas, notas) + nuevasDe(local.veterinarios, veterinarios) + mascotasNuevas

        val filasAModificar = modificadasDe(local.vacunasEn, vacunas) +
            modificadasDe(local.preventivosEn, preventivos) +
            modificadasDe(local.revisionesEn, revisiones) +
            modificadasDe(local.tratamientosEn, tratamientos) +
            modificadasDe(local.pesosEn, pesos) + modificadasDe(local.informesEn, informes) +
            modificadasDe(local.notasEn, notas) + modificadasDe(local.veterinariosEn, veterinarios) +
            mascotasAModificar

        return PlanFusion(
            vista = LectorPaquete.vistaPrevia(paquete, datos),
            mascotasNuevas = mascotasNuevas,
            mascotasAModificar = mascotasAModificar,
            mascotasIguales = mascotasIguales,
            filasNuevas = filasNuevas,
            filasAModificar = filasAModificar,
            filasIguales = datos.totalFilas - filasNuevas - filasAModificar,
            conflictos = conflictos
        )
    }

    // --- Adjuntos -----------------------------------------------------------------------------

    private fun extractarFoto(
        paquete: String,
        mascota: FilaMascota,
        destino: MutableMap<String, String>,
        escritas: MutableList<String>
    ) {
        val entrada = mascota.foto ?: return
        val ruta = "${carpetas.fotos}/${mascota.uuid}${extensionDe(entrada, "jpg")}"
        if (extraerDeZip(paquete, entrada, ruta)) {
            destino[mascota.uuid] = ruta
            escritas += ruta
        }
    }

    private fun extractarInforme(
        paquete: String,
        informe: FilaInforme,
        destino: MutableMap<String, String>,
        escritas: MutableList<String>
    ) {
        val entrada = informe.archivo ?: return
        val nombreSeguro = informe.nombreArchivo
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "informe${extensionDe(entrada, "pdf")}" }
        val ruta = "${carpetas.informes}/${informe.uuid}_$nombreSeguro"
        if (extraerDeZip(paquete, entrada, ruta)) {
            destino[informe.uuid] = ruta
            escritas += ruta
        }
    }

    private fun rutaCopiaLibre(nombre: String): String {
        var candidato = "${carpetas.copias}/$nombre"
        var intento = 2
        while (existeArchivo(candidato) && intento < 100) {
            candidato = "${carpetas.copias}/${nombre.removeSuffix(".vpet")}-$intento.vpet"
            intento++
        }
        return candidato
    }
}
