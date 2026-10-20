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
import com.example.virtualpetkmp.util.Localizacion
import com.example.virtualpetkmp.util.ProgramadorNotificaciones
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
import com.example.virtualpetkmp.util.detalleDeError
import com.example.virtualpetkmp.util.ErrorAppException
import com.example.virtualpetkmp.util.textoDeFallo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.error_borrar_dato
import virtualpetkmp.shared.generated.resources.error_cargar_ficha
import virtualpetkmp.shared.generated.resources.error_guardar_dato

/**
 * Resumen de una mascota para la pantalla de ficha: los contadores y avisos ya calculados.
 *
 * La tarjeta de cada bloque muestra **un solo dato**, no un recuento de todo: por eso hay
 * campos para lo que reclama atención (vencidas, próximas, activos) y también para los
 * totales, que se usan al entrar en la hoja correspondiente.
 */
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
    val notas: List<Nota> = emptyList(),
    val preventivos: List<Preventivo> = emptyList()
)

/**
 * Estado de la ficha de UNA mascota: su resumen, sus listas y los avisos.
 *
 * Es el ViewModel más grande de la app porque la ficha es un panel que lo abarca todo: peso,
 * vacunas, preventivos, revisiones, tratamientos, informes y notas.
 *
 * Dos ideas que conviene tener claras antes de tocarlo:
 *
 * 1. **Se crea uno por mascota** (desde `App`, con `remember(mascotaId)`) y se comparte entre
 *    la pantalla y todas sus hojas modales. Por eso el resumen y las listas son coherentes:
 *    hay una sola fuente de verdad mientras la ficha está abierta.
 * 2. **Al cargar el resumen se reprograman los avisos** de esa mascota: se cancelan y se
 *    vuelven a programar sus dosis pendientes. Es lo que mantiene las notificaciones al día
 *    sin necesidad de un servicio en segundo plano.
 *
 * Los cálculos de estado (vencida / próxima / al día, tratamiento activo) se hacen **aquí**, a
 * partir de la fecha de hoy, no en los repositorios: la base de datos guarda fechas, no
 * conclusiones.
 *
 * @param programador si es `null`, la ficha funciona igual pero sin tocar notificaciones. Así
 *   el ViewModel se puede probar sin Android por medio.
 */
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
    private val programador: ProgramadorNotificaciones? = null,
    /**
     * Nombre de la mascota, para el texto de los recordatorios de tratamiento ("Luna · Oticoear").
     * Se pasa ya resuelto porque el aviso se monta fuera de la composición, donde no se puede
     * consultar el listado de mascotas.
     */
    private val nombreMascota: String = "",
    /**
     * Configuración regional activa. La necesita el texto de las **notificaciones**, que se
     * monta fuera de la composición y tiene que escribir la fecha como la espera el usuario.
     */
    private val localizacion: Localizacion = Localizacion.desdeRegion("es-ES"),
    /**
     * Hilo en el que corre el estado. Se puede cambiar solo para los tests: `Dispatchers.Main`
     * no existe en jvmTest. En la app se deja el valor por defecto.
     */
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    /**
     * `SupervisorJob` y no `Job` a propósito: con un `Job` normal, una sola excepción sin
     * capturar cancela el scope entero y **todas** las operaciones siguientes se vuelven
     * invisibles —la ficha deja de guardar nada y no dice por qué—. Con un supervisor, un fallo
     * se queda en su corrutina y las demás siguen funcionando.
     */
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _resumen = MutableStateFlow(ResumenFicha())
    val resumen: StateFlow<ResumenFicha> = _resumen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Último error de una operación, ya traducido, para que la ficha lo enseñe.
     *
     * Antes no existía: las escrituras llamaban al repositorio, tiraban el `Result` y recargaban
     * como si todo hubiera ido bien, así que un fallo no se veía por ninguna parte.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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

    /** Serializa los recálculos del resumen: ver [cargarResumen]. */
    private val mutexResumen = Mutex()

    init {
        cargarResumen()
    }

    /**
     * Relee todo lo de esta mascota y recalcula el resumen.
     *
     * Va envuelto en `try`/`catch` porque las lecturas no devuelven `Result`: si una consulta
     * falla, la excepción subiría hasta el scope y —con el `Job` que había antes— lo dejaría
     * cancelado, de modo que la ficha entera dejaría de responder sin decir nada.
     *
     * Los recálculos van **de uno en uno** ([mutexResumen]). Sin eso, dos recargas encadenadas
     * pueden cruzarse: la primera lee la base de datos, la segunda lee y escribe ya con el dato
     * nuevo, y la primera termina después **pisando** ese resultado bueno con el viejo. Se vio
     * como una nota recién guardada que no aparecía en la ficha hasta volver a entrar, y hacía
     * fallar un test una de cada cuatro veces.
     */
    fun cargarResumen() {
        scope.launch {
            _isLoading.value = true
            try {
                // La lectura va dentro del cerrojo y no antes: así el último recálculo en
                // ejecutarse es también el que ha leído los datos más recientes.
                mutexResumen.withLock { leerYRecalcular() }
            } catch (e: Exception) {
                _errorMessage.value = getString(Res.string.error_cargar_ficha, detalleDeError(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun leerYRecalcular() {
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
                        ReprogramadorNotificaciones.programarVacuna(prog, vacuna, localizacion)
                    }
                }
                preventivos.forEach { preventivo ->
                    preventivo.id?.let { id ->
                        ReprogramadorNotificaciones.cancelarPreventivo(prog, id)
                        ReprogramadorNotificaciones.programarPreventivo(prog, preventivo, localizacion)
                    }
                }
                // Los tratamientos se reprograman igual: cancelar y volver a programar deja el
                // recordatorio de cada día en su hora, y es lo que quita el aviso de un
                // tratamiento borrado o terminado sin tener que hacer nada especial.
                tratamientos.forEach { tratamiento ->
                    tratamiento.id?.let { id ->
                        ReprogramadorNotificaciones.cancelarTratamiento(prog, id)
                        ReprogramadorNotificaciones.programarTratamiento(prog, tratamiento, nombreMascota)
                    }
                }
            }
    }

    /** Quita el aviso de error de la pantalla. */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Ejecuta una escritura del repositorio convirtiendo en `Result` cualquier excepción.
     *
     * Los repositorios ya devuelven `Result`, pero envolverlos aquí evita que una excepción
     * inesperada (por ejemplo, cerrar la app a mitad) se escape y tumbe el scope.
     */
    private suspend fun <T> conResultado(operacion: suspend () -> Result<T>): Result<T> =
        try {
            operacion()
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Cierra una operación de escritura: si fue bien recarga la ficha y, si no, publica el error
     * para que se vea.
     *
     * Es el arreglo del fallo que tenía esta pantalla: antes se llamaba al repositorio, se
     * ignoraba el `Result` y se recargaba igual, así que un fallo al guardar una vacuna o un
     * tratamiento no se veía por ningún lado y el usuario creía que se había guardado.
     */
    private suspend fun cerrarOperacion(resultado: Result<*>, claveError: StringResource) {
        val fallo = resultado.exceptionOrNull()
        if (fallo != null) {
            _errorMessage.value = textoDeError(claveError, fallo)
        } else {
            cargarResumen()
        }
    }

    /**
     * Texto del error: los motivos de dominio ya saben traducirse solos; para el resto se usa el
     * mensaje de la pantalla más el detalle técnico, que es lo que permite diagnosticar.
     */
    private suspend fun textoDeError(clave: StringResource, fallo: Throwable): String =
        if (fallo is ErrorAppException) {
            textoDeFallo(fallo)
        } else {
            getString(clave, detalleDeError(fallo))
        }

    /**
     * Todo lo que necesita el PDF de esta mascota.
     *
     * Los informes **no** se leen: el PDF ya no los lista (solo podía enseñar el nombre del
     * archivo, y eso no le sirve a nadie), así que consultarlos sería una consulta de más.
     */
    suspend fun obtenerDatosPdf(): DatosFichaPdf {
        val vacunas = vacunaRepository.getVacunasByMascotaId(mascotaId)
        val revisiones = revisionRepository.getRevisionesByMascotaId(mascotaId)
        val tratamientos = tratamientoRepository.getTratamientosByMascotaId(mascotaId)
        val pesos = pesoRepository.getPesosByMascotaId(mascotaId)
        val notas = notaRepository.getNotasByMascotaId(mascotaId)
        val preventivos = preventivoRepository.getPreventivosByMascotaId(mascotaId)

        return DatosFichaPdf(
            vacunas = vacunas,
            revisiones = revisiones,
            tratamientos = tratamientos,
            pesos = pesos,
            notas = notas,
            preventivos = preventivos
        )
    }

    /** Añade una nota desde la hoja modal y recarga el resumen. */
    fun agregarNota(texto: String) {
        if (texto.isBlank()) return
        scope.launch {
            val resultado = conResultado {
                notaRepository.insertNota(mascotaId = mascotaId, texto = texto)
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Elimina una nota desde la hoja modal y recarga el resumen. */
    fun borrarNota(id: Long) {
        scope.launch {
            val resultado = conResultado { notaRepository.deleteNota(id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
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
            val resultado = conResultado {
                vacunaRepository.insertVacuna(
                    mascotaId = mascotaId,
                    nombre = nombre,
                    fechaAplicacion = fechaAplicacion,
                    fechaProximaDosis = fechaProximaDosis,
                    veterinario = veterinario,
                    lote = lote
                )
            }
            // El aviso solo se programa si la vacuna llegó a guardarse: si falló, el id no
            // existe y programar una notificación para una vacuna fantasma sería peor.
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
                        ),
                        localizacion
                    )
                }
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
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
            val resultado = conResultado {
                preventivoRepository.insertPreventivo(
                    mascotaId = mascotaId,
                    tipo = tipo,
                    nombre = nombre,
                    fechaAplicacion = fechaAplicacion,
                    fechaProximaDosis = fechaProximaDosis,
                    veterinario = null,
                    lote = null
                )
            }
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
                        ),
                        localizacion
                    )
                }
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Alta de tratamiento desde la hoja de la ficha. */
    fun agregarTratamiento(
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?,
        hora: String? = null,
        notificar: Boolean = false
    ) {
        if (nombreMedicamento.isBlank()) return
        scope.launch {
            val resultado = conResultado {
                tratamientoRepository.insertTratamiento(
                    mascotaId = mascotaId,
                    nombreMedicamento = nombreMedicamento,
                    dosis = dosis,
                    frecuencia = frecuencia,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    hora = hora,
                    notificar = notificar
                )
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Borra una vacuna desde la hoja de la ficha y cancela su aviso. */
    fun borrarVacuna(id: Long) {
        scope.launch {
            // El aviso se cancela pase lo que pase: si el borrado falló, la vacuna sigue ahí y
            // su notificación también debe seguir; si funcionó, ya no tiene sentido.
            val resultado = conResultado { vacunaRepository.deleteVacuna(id) }
            programador?.let { prog -> ReprogramadorNotificaciones.cancelar(prog, id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
        }
    }

    /** Borra un preventivo desde la hoja de la ficha y cancela su aviso. */
    fun borrarPreventivo(id: Long) {
        scope.launch {
            val resultado = conResultado { preventivoRepository.deletePreventivo(id) }
            programador?.let { prog -> ReprogramadorNotificaciones.cancelarPreventivo(prog, id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
        }
    }

    /** Borra un tratamiento desde la hoja de la ficha y cancela su recordatorio. */
    fun borrarTratamiento(id: Long) {
        scope.launch {
            val resultado = conResultado { tratamientoRepository.deleteTratamiento(id) }
            // El aviso se cancela aquí porque, al borrar, la recarga del resumen ya no ve este
            // tratamiento y no podría quitarlo: la alarma seguiría sonando por un medicamento
            // que ya no existe.
            programador?.let { prog -> ReprogramadorNotificaciones.cancelarTratamiento(prog, id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
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
            val resultado = conResultado {
                vacunaRepository.updateVacuna(
                    id = id,
                    nombre = nombre,
                    fechaAplicacion = fechaAplicacion,
                    fechaProximaDosis = fechaProximaDosis,
                    veterinario = veterinario,
                    lote = lote
                )
            }
            // Solo se reprograma el aviso si el cambio llegó a guardarse: si no, la notificación
            // estaría avisando de una fecha que la base de datos no tiene.
            if (resultado.isSuccess) {
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
                        ),
                        localizacion
                    )
                }
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
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
            val resultado = conResultado {
                preventivoRepository.updatePreventivo(
                    id = id,
                    tipo = tipo,
                    nombre = nombre,
                    fechaAplicacion = fechaAplicacion,
                    fechaProximaDosis = fechaProximaDosis,
                    veterinario = null,
                    lote = null
                )
            }
            if (resultado.isSuccess) {
                programador?.let { prog ->
                    ReprogramadorNotificaciones.cancelarPreventivo(prog, id)
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
                        ),
                        localizacion
                    )
                }
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Edición de un tratamiento desde la hoja de la ficha. */
    fun actualizarTratamiento(
        id: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?,
        hora: String? = null,
        notificar: Boolean = false
    ) {
        if (nombreMedicamento.isBlank()) return
        scope.launch {
            val resultado = conResultado {
                tratamientoRepository.updateTratamiento(
                    id = id,
                    nombreMedicamento = nombreMedicamento,
                    dosis = dosis,
                    frecuencia = frecuencia,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    hora = hora,
                    notificar = notificar
                )
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
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
            val resultado = conResultado {
                revisionRepository.insertRevision(
                    mascotaId = mascotaId,
                    fecha = fecha,
                    motivo = motivo,
                    diagnostico = diagnostico,
                    notas = notas,
                    veterinario = veterinario
                )
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
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
            val resultado = conResultado { notaRepository.updateNota(id, texto) }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
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
            val resultado = conResultado {
                informeRepository.insertInforme(
                    mascotaId = mascotaId,
                    tipo = tipo,
                    descripcion = descripcion,
                    fecha = fecha,
                    nombreArchivo = nombreArchivo,
                    rutaArchivo = rutaArchivo
                )
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Edición de los datos de un informe (el archivo adjunto no se cambia). */
    fun actualizarInforme(id: Long, tipo: String, descripcion: String?, fecha: LocalDate) {
        if (tipo.isBlank()) return
        scope.launch {
            val resultado = conResultado { informeRepository.updateInforme(id, tipo, descripcion, fecha) }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Borra un informe desde la hoja de la ficha. */
    fun borrarInforme(id: Long) {
        scope.launch {
            val resultado = conResultado { informeRepository.deleteInforme(id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
        }
    }

    /** Alta de un pesaje desde la hoja de la ficha. */
    fun agregarPeso(fecha: LocalDate, kilos: Double, notas: String?) {
        if (kilos <= 0.0) return
        scope.launch {
            val resultado = conResultado {
                pesoRepository.insertPeso(mascotaId = mascotaId, fecha = fecha, peso = kilos, notas = notas)
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Edición de un pesaje desde la hoja de la ficha. */
    fun actualizarPeso(id: Long, fecha: LocalDate, kilos: Double, notas: String?) {
        if (kilos <= 0.0) return
        scope.launch {
            val resultado = conResultado {
                pesoRepository.updatePeso(id = id, fecha = fecha, peso = kilos, notas = notas)
            }
            cerrarOperacion(resultado, Res.string.error_guardar_dato)
        }
    }

    /** Borra un pesaje desde la hoja de la ficha. */
    fun borrarPeso(id: Long) {
        scope.launch {
            val resultado = conResultado { pesoRepository.deletePeso(id) }
            cerrarOperacion(resultado, Res.string.error_borrar_dato)
        }
    }
}
