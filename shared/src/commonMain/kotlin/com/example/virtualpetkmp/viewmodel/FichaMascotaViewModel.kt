package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.util.ProgramadorNotificaciones
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class ResumenFicha(
    val vacunasVencidas: Int = 0,
    val vacunasProximas: Int = 0,
    val totalVacunas: Int = 0,
    val preventivosVencidos: Int = 0,
    val preventivosProximos: Int = 0,
    val totalPreventivos: Int = 0,
    val totalRevisiones: Int = 0,
    val tratamientosActivos: Int = 0,
    val totalTratamientos: Int = 0,
    val ultimoPeso: Double? = null,
    val pesoAnterior: Double? = null,
    val pesosRecientes: List<Peso> = emptyList(),
    val totalPesos: Int = 0,
    val totalInformes: Int = 0,
    val totalNotas: Int = 0,
    /** Resumen legible del primer tratamiento activo (nombre + pauta), para la tarjeta hub. */
    val tratamientoActivo: String? = null,
    /** Texto de la última nota registrada, para la tarjeta hub. */
    val ultimaNota: String? = null
) {
    /** Vacunas que no están vencidas ni a punto de vencer. */
    val vacunasAlDia: Int get() = (totalVacunas - vacunasVencidas - vacunasProximas).coerceAtLeast(0)

    /** Preventivos que no están vencidos ni a punto de vencer. */
    val preventivosAlDia: Int get() = (totalPreventivos - preventivosVencidos - preventivosProximos).coerceAtLeast(0)

    /** Registros que agrupa la tarjeta "Salud y seguimiento". */
    val totalSaludYSeguimiento: Int get() = totalRevisiones + totalTratamientos + totalInformes
}

data class DatosFichaPdf(
    val vacunas: List<Vacuna> = emptyList(),
    val revisiones: List<Revision> = emptyList(),
    val tratamientos: List<Tratamiento> = emptyList(),
    val pesos: List<Peso> = emptyList(),
    val informes: List<Informe> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val preventivos: List<Preventivo> = emptyList()
)

class FichaMascotaViewModel(
    private val mascotaId: Long,
    private val vacunaRepository: VacunaRepository,
    private val revisionRepository: RevisionRepository,
    private val tratamientoRepository: TratamientoRepository,
    private val pesoRepository: PesoRepository,
    private val informeRepository: InformeRepository,
    private val notaRepository: NotaRepository,
    private val preventivoRepository: PreventivoRepository,
    /** Programador de avisos: la ficha es ahora quien da de alta vacunas y preventivos. */
    private val programador: ProgramadorNotificaciones? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _resumen = MutableStateFlow(ResumenFicha())
    val resumen: StateFlow<ResumenFicha> = _resumen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Listas completas de la mascota, expuestas para las hojas modales del dashboard.
    private val _tratamientos = MutableStateFlow<List<Tratamiento>>(emptyList())
    val tratamientos: StateFlow<List<Tratamiento>> = _tratamientos.asStateFlow()

    private val _informes = MutableStateFlow<List<Informe>>(emptyList())
    val informes: StateFlow<List<Informe>> = _informes.asStateFlow()

    private val _revisiones = MutableStateFlow<List<Revision>>(emptyList())
    val revisiones: StateFlow<List<Revision>> = _revisiones.asStateFlow()

    private val _vacunas = MutableStateFlow<List<Vacuna>>(emptyList())
    val vacunas: StateFlow<List<Vacuna>> = _vacunas.asStateFlow()

    private val _preventivos = MutableStateFlow<List<Preventivo>>(emptyList())
    val preventivos: StateFlow<List<Preventivo>> = _preventivos.asStateFlow()

    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()

    private val _pesos = MutableStateFlow<List<Peso>>(emptyList())
    val pesos: StateFlow<List<Peso>> = _pesos.asStateFlow()

    init {
        cargarResumen()
    }

    fun cargarResumen() {
        scope.launch {
            _isLoading.value = true
            val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val hoyMas15 = kotlinx.datetime.LocalDate.fromEpochDays(hoy.toEpochDays() + 15)
            val hoyMas5 = kotlinx.datetime.LocalDate.fromEpochDays(hoy.toEpochDays() + 5)

            val vacunas = vacunaRepository.getVacunasByMascotaId(mascotaId)
            val revisiones = revisionRepository.getRevisionesByMascotaId(mascotaId)
            val tratamientos = tratamientoRepository.getTratamientosByMascotaId(mascotaId)
            val pesos = pesoRepository.getPesosByMascotaId(mascotaId)
            val informes = informeRepository.getInformesByMascotaId(mascotaId)
            val notas = notaRepository.getNotasByMascotaId(mascotaId)
            val preventivos = preventivoRepository.getPreventivosByMascotaId(mascotaId)

            val pesosOrdenados = pesos.sortedByDescending { it.fecha }

            val tratamientoActivoActual = tratamientos.firstOrNull {
                it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy)
            }

            _resumen.value = ResumenFicha(
                vacunasVencidas = vacunas.count { it.fechaProximaDosis < hoy },
                vacunasProximas = vacunas.count { it.fechaProximaDosis >= hoy && it.fechaProximaDosis <= hoyMas15 },
                totalVacunas = vacunas.size,
                preventivosVencidos = preventivos.count { it.fechaProximaDosis < hoy },
                preventivosProximos = preventivos.count { it.fechaProximaDosis >= hoy && it.fechaProximaDosis <= hoyMas5 },
                totalPreventivos = preventivos.size,
                totalRevisiones = revisiones.size,
                tratamientosActivos = tratamientos.count {
                    it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy)
                },
                totalTratamientos = tratamientos.size,
                ultimoPeso = pesosOrdenados.getOrNull(0)?.peso,
                pesoAnterior = pesosOrdenados.getOrNull(1)?.peso,
                pesosRecientes = pesos,
                totalPesos = pesos.size,
                totalInformes = informes.size,
                totalNotas = notas.size,
                tratamientoActivo = tratamientoActivoActual?.let { tratamiento ->
                    listOfNotNull(
                        tratamiento.nombreMedicamento,
                        tratamiento.dosis,
                        tratamiento.frecuencia
                    ).joinToString(" · ")
                },
                ultimaNota = notas.firstOrNull()?.texto
            )

            _tratamientos.value = tratamientos
            _informes.value = informes
            _revisiones.value = revisiones
            _vacunas.value = vacunas
            _preventivos.value = preventivos
            _notas.value = notas
            _pesos.value = pesos

            // Reprograma los avisos de esta mascota: al abrir su ficha se cancelan y se
            // vuelven a programar sus dosis pendientes, que es lo que mantenía antes el
            // bloque de arranque de la app.
            programador?.let { prog ->
                vacunas.forEach { vacuna ->
                    vacuna.id?.let { id ->
                        ReprogramadorNotificaciones.cancelar(prog, id)
                        ReprogramadorNotificaciones.programarVacuna(prog, vacuna)
                    }
                }
                preventivos.forEach { preventivo ->
                    preventivo.id?.let { id ->
                        ReprogramadorNotificaciones.cancelar(prog, id)
                        ReprogramadorNotificaciones.programarPreventivo(prog, preventivo)
                    }
                }
            }

            _isLoading.value = false
        }
    }

    suspend fun obtenerDatosPdf(): DatosFichaPdf {
        val vacunas = vacunaRepository.getVacunasByMascotaId(mascotaId)
        val revisiones = revisionRepository.getRevisionesByMascotaId(mascotaId)
        val tratamientos = tratamientoRepository.getTratamientosByMascotaId(mascotaId)
        val pesos = pesoRepository.getPesosByMascotaId(mascotaId)
        val informes = informeRepository.getInformesByMascotaId(mascotaId)
        val notas = notaRepository.getNotasByMascotaId(mascotaId)
        val preventivos = preventivoRepository.getPreventivosByMascotaId(mascotaId)

        return DatosFichaPdf(
            vacunas = vacunas,
            revisiones = revisiones,
            tratamientos = tratamientos,
            pesos = pesos,
            informes = informes,
            notas = notas,
            preventivos = preventivos
        )
    }

    /** Añade una nota desde la hoja modal y recarga el resumen. */
    fun agregarNota(texto: String) {
        if (texto.isBlank()) return
        scope.launch {
            notaRepository.insertNota(mascotaId = mascotaId, texto = texto)
            cargarResumen()
        }
    }

    /** Elimina una nota desde la hoja modal y recarga el resumen. */
    fun borrarNota(id: Long) {
        scope.launch {
            notaRepository.deleteNota(id)
            cargarResumen()
        }
    }

    /** Alta de vacuna desde la hoja de la ficha, programando su aviso. */
    fun agregarVacuna(
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        if (nombre.isBlank()) return
        scope.launch {
            val resultado = vacunaRepository.insertVacuna(
                mascotaId = mascotaId,
                nombre = nombre,
                fechaAplicacion = fechaAplicacion,
                fechaProximaDosis = fechaProximaDosis,
                veterinario = veterinario,
                lote = lote
            )
            programador?.let { prog ->
                resultado.getOrNull()?.let { id ->
                    ReprogramadorNotificaciones.programarVacuna(
                        prog,
                        Vacuna(
                            id = id,
                            mascotaId = mascotaId,
                            nombre = nombre,
                            fechaAplicacion = fechaAplicacion,
                            fechaProximaDosis = fechaProximaDosis,
                            veterinario = veterinario,
                            lote = lote
                        )
                    )
                }
            }
            cargarResumen()
        }
    }

    /** Alta de preventivo desde la hoja de la ficha, programando su aviso. */
    fun agregarPreventivo(
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate
    ) {
        if (nombre.isBlank()) return
        scope.launch {
            val resultado = preventivoRepository.insertPreventivo(
                mascotaId = mascotaId,
                tipo = tipo,
                nombre = nombre,
                fechaAplicacion = fechaAplicacion,
                fechaProximaDosis = fechaProximaDosis,
                veterinario = null,
                lote = null
            )
            programador?.let { prog ->
                resultado.getOrNull()?.let { id ->
                    ReprogramadorNotificaciones.programarPreventivo(
                        prog,
                        Preventivo(
                            id = id,
                            mascotaId = mascotaId,
                            tipo = tipo,
                            nombre = nombre,
                            fechaAplicacion = fechaAplicacion,
                            fechaProximaDosis = fechaProximaDosis,
                            veterinario = null,
                            lote = null
                        )
                    )
                }
            }
            cargarResumen()
        }
    }

    /** Alta de tratamiento desde la hoja de la ficha. */
    fun agregarTratamiento(
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?
    ) {
        if (nombreMedicamento.isBlank()) return
        scope.launch {
            tratamientoRepository.insertTratamiento(
                mascotaId = mascotaId,
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin
            )
            cargarResumen()
        }
    }

    /** Borra una vacuna desde la hoja de la ficha y cancela su aviso. */
    fun borrarVacuna(id: Long) {
        scope.launch {
            vacunaRepository.deleteVacuna(id)
            programador?.let { prog -> ReprogramadorNotificaciones.cancelar(prog, id) }
            cargarResumen()
        }
    }

    /** Borra un preventivo desde la hoja de la ficha y cancela su aviso. */
    fun borrarPreventivo(id: Long) {
        scope.launch {
            preventivoRepository.deletePreventivo(id)
            programador?.let { prog -> ReprogramadorNotificaciones.cancelar(prog, id) }
            cargarResumen()
        }
    }

    /** Borra un tratamiento desde la hoja de la ficha. */
    fun borrarTratamiento(id: Long) {
        scope.launch {
            tratamientoRepository.deleteTratamiento(id)
            cargarResumen()
        }
    }

    /** Edición de una vacuna desde la hoja de la ficha, reprogramando su aviso. */
    fun actualizarVacuna(
        id: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        if (nombre.isBlank()) return
        scope.launch {
            vacunaRepository.updateVacuna(
                id = id,
                nombre = nombre,
                fechaAplicacion = fechaAplicacion,
                fechaProximaDosis = fechaProximaDosis,
                veterinario = veterinario,
                lote = lote
            )
            programador?.let { prog ->
                ReprogramadorNotificaciones.cancelar(prog, id)
                ReprogramadorNotificaciones.programarVacuna(
                    prog,
                    Vacuna(
                        id = id,
                        mascotaId = mascotaId,
                        nombre = nombre,
                        fechaAplicacion = fechaAplicacion,
                        fechaProximaDosis = fechaProximaDosis,
                        veterinario = veterinario,
                        lote = lote
                    )
                )
            }
            cargarResumen()
        }
    }

    /** Edición de un preventivo desde la hoja de la ficha, reprogramando su aviso. */
    fun actualizarPreventivo(
        id: Long,
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate
    ) {
        if (nombre.isBlank()) return
        scope.launch {
            preventivoRepository.updatePreventivo(
                id = id,
                tipo = tipo,
                nombre = nombre,
                fechaAplicacion = fechaAplicacion,
                fechaProximaDosis = fechaProximaDosis,
                veterinario = null,
                lote = null
            )
            programador?.let { prog ->
                ReprogramadorNotificaciones.cancelar(prog, id)
                ReprogramadorNotificaciones.programarPreventivo(
                    prog,
                    Preventivo(
                        id = id,
                        mascotaId = mascotaId,
                        tipo = tipo,
                        nombre = nombre,
                        fechaAplicacion = fechaAplicacion,
                        fechaProximaDosis = fechaProximaDosis,
                        veterinario = null,
                        lote = null
                    )
                )
            }
            cargarResumen()
        }
    }

    /** Edición de un tratamiento desde la hoja de la ficha. */
    fun actualizarTratamiento(
        id: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?
    ) {
        if (nombreMedicamento.isBlank()) return
        scope.launch {
            tratamientoRepository.updateTratamiento(
                id = id,
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin
            )
            cargarResumen()
        }
    }

    /** Alta de visita desde la hoja de la ficha. */
    fun agregarRevision(
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ) {
        if (motivo.isBlank()) return
        scope.launch {
            revisionRepository.insertRevision(
                mascotaId = mascotaId,
                fecha = fecha,
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario
            )
            cargarResumen()
        }
    }

    /** Edición de una visita desde la hoja de la ficha. */
    fun actualizarRevision(
        id: Long,
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ) {
        if (motivo.isBlank()) return
        scope.launch {
            revisionRepository.updateRevision(
                id = id,
                fecha = fecha,
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario
            )
            cargarResumen()
        }
    }

    /** Borra una visita desde la hoja de la ficha. */
    fun borrarRevision(id: Long) {
        scope.launch {
            revisionRepository.deleteRevision(id)
            cargarResumen()
        }
    }

    /** Edición del texto de una nota (la hoja permite verla y modificarla). */
    fun actualizarNota(id: Long, texto: String) {
        if (texto.isBlank()) return
        scope.launch {
            notaRepository.updateNota(id, texto)
            cargarResumen()
        }
    }

    /** Alta de informe desde la hoja de la ficha (con archivo ya copiado por el selector). */
    fun agregarInforme(
        tipo: String,
        descripcion: String?,
        fecha: LocalDate,
        nombreArchivo: String,
        rutaArchivo: String
    ) {
        if (tipo.isBlank() || nombreArchivo.isBlank()) return
        scope.launch {
            informeRepository.insertInforme(
                mascotaId = mascotaId,
                tipo = tipo,
                descripcion = descripcion,
                fecha = fecha,
                nombreArchivo = nombreArchivo,
                rutaArchivo = rutaArchivo
            )
            cargarResumen()
        }
    }

    /** Edición de los datos de un informe (el archivo adjunto no se cambia). */
    fun actualizarInforme(id: Long, tipo: String, descripcion: String?, fecha: LocalDate) {
        if (tipo.isBlank()) return
        scope.launch {
            informeRepository.updateInforme(id, tipo, descripcion, fecha)
            cargarResumen()
        }
    }

    /** Borra un informe desde la hoja de la ficha. */
    fun borrarInforme(id: Long) {
        scope.launch {
            informeRepository.deleteInforme(id)
            cargarResumen()
        }
    }

    /** Alta de un pesaje desde la hoja de la ficha. */
    fun agregarPeso(fecha: LocalDate, kilos: Double, notas: String?) {
        if (kilos <= 0.0) return
        scope.launch {
            pesoRepository.insertPeso(mascotaId = mascotaId, fecha = fecha, peso = kilos, notas = notas)
            cargarResumen()
        }
    }

    /** Edición de un pesaje desde la hoja de la ficha. */
    fun actualizarPeso(id: Long, fecha: LocalDate, kilos: Double, notas: String?) {
        if (kilos <= 0.0) return
        scope.launch {
            pesoRepository.updatePeso(id = id, fecha = fecha, peso = kilos, notas = notas)
            cargarResumen()
        }
    }

    /** Borra un pesaje desde la hoja de la ficha. */
    fun borrarPeso(id: Long) {
        scope.launch {
            pesoRepository.deletePeso(id)
            cargarResumen()
        }
    }
}
