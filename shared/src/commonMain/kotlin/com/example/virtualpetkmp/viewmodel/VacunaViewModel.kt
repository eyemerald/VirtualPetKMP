package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.data.VacunaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class VacunaViewModel(
    private val repository: VacunaRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _vacunas = MutableStateFlow<List<Vacuna>>(emptyList())
    val vacunas: StateFlow<List<Vacuna>> = _vacunas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadVacunas()
    }

    fun loadVacunas() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _vacunas.value = repository.getVacunasByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar las vacunas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addVacuna(
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertVacuna(
                mascotaId,
                nombre,
                fechaAplicacion,
                fechaProximaDosis,
                veterinario,
                lote
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadVacunas()
            }
            _isLoading.value = false
        }
    }

    fun updateVacuna(
        id: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updateVacuna(
                id,
                nombre,
                fechaAplicacion,
                fechaProximaDosis,
                veterinario,
                lote
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadVacunas()
            }
            _isLoading.value = false
        }
    }

    fun deleteVacuna(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deleteVacuna(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadVacunas()
            }
            _isLoading.value = false
        }
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
