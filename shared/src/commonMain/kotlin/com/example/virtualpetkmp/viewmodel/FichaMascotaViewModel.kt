package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class ResumenFicha(
    val vacunasVencidas: Int = 0,
    val totalVacunas: Int = 0,
    val totalRevisiones: Int = 0,
    val tratamientosActivos: Int = 0,
    val totalTratamientos: Int = 0,
    val ultimoPeso: Double? = null,
    val pesoAnterior: Double? = null,
    val pesosRecientes: List<Peso> = emptyList(),
    val totalInformes: Int = 0,
    val totalNotas: Int = 0
)

data class DatosFichaPdf(
    val vacunas: List<Vacuna> = emptyList(),
    val revisiones: List<Revision> = emptyList(),
    val tratamientos: List<Tratamiento> = emptyList(),
    val pesos: List<Peso> = emptyList(),
    val informes: List<Informe> = emptyList(),
    val notas: List<Nota> = emptyList()
)

class FichaMascotaViewModel(
    private val mascotaId: Long,
    private val vacunaRepository: VacunaRepository,
    private val revisionRepository: RevisionRepository,
    private val tratamientoRepository: TratamientoRepository,
    private val pesoRepository: PesoRepository,
    private val informeRepository: InformeRepository,
    private val notaRepository: NotaRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _resumen = MutableStateFlow(ResumenFicha())
    val resumen: StateFlow<ResumenFicha> = _resumen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        cargarResumen()
    }

    fun cargarResumen() {
        scope.launch {
            _isLoading.value = true
            val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val vacunas = vacunaRepository.getVacunasByMascotaId(mascotaId)
            val revisiones = revisionRepository.getRevisionesByMascotaId(mascotaId)
            val tratamientos = tratamientoRepository.getTratamientosByMascotaId(mascotaId)
            val pesos = pesoRepository.getPesosByMascotaId(mascotaId)
            val informes = informeRepository.getInformesByMascotaId(mascotaId)
            val notas = notaRepository.getNotasByMascotaId(mascotaId)

            val pesosOrdenados = pesos.sortedByDescending { it.fecha }

            _resumen.value = ResumenFicha(
                vacunasVencidas = vacunas.count { it.fechaProximaDosis < hoy },
                totalVacunas = vacunas.size,
                totalRevisiones = revisiones.size,
                tratamientosActivos = tratamientos.count {
                    it.fechaInicio <= hoy && (it.fechaFin == null || it.fechaFin >= hoy)
                },
                totalTratamientos = tratamientos.size,
                ultimoPeso = pesosOrdenados.getOrNull(0)?.peso,
                pesoAnterior = pesosOrdenados.getOrNull(1)?.peso,
                pesosRecientes = pesos,
                totalInformes = informes.size,
                totalNotas = notas.size
            )
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

        return DatosFichaPdf(
            vacunas = vacunas,
            revisiones = revisiones,
            tratamientos = tratamientos,
            pesos = pesos,
            informes = informes,
            notas = notas
        )
    }
}
