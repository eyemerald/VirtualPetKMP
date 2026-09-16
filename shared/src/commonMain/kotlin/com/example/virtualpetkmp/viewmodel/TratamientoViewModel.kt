package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.data.TratamientoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TratamientoViewModel(
    private val repository: TratamientoRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _tratamientos = MutableStateFlow<List<Tratamiento>>(emptyList())
    val tratamientos: StateFlow<List<Tratamiento>> = _tratamientos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadTratamientos()
    }

    fun loadTratamientos() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _tratamientos.value = repository.getTratamientosByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los tratamientos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTratamiento(
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertTratamiento(
                mascotaId,
                nombreMedicamento,
                dosis,
                frecuencia,
                fechaInicio,
                fechaFin
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadTratamientos()
            }
            _isLoading.value = false
        }
    }

    fun updateTratamiento(
        id: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updateTratamiento(
                id,
                nombreMedicamento,
                dosis,
                frecuencia,
                fechaInicio,
                fechaFin
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadTratamientos()
            }
            _isLoading.value = false
        }
    }

    fun deleteTratamiento(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deleteTratamiento(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadTratamientos()
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
