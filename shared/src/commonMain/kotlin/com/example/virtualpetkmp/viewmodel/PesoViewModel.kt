package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.data.PesoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class PesoViewModel(
    private val repository: PesoRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _pesos = MutableStateFlow<List<Peso>>(emptyList())
    val pesos: StateFlow<List<Peso>> = _pesos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPesos()
    }

    fun loadPesos() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _pesos.value = repository.getPesosByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los pesos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPeso(fecha: LocalDate, peso: Double, notas: String?) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertPeso(mascotaId, fecha, peso, notas)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPesos()
            }
            _isLoading.value = false
        }
    }

    fun updatePeso(id: Long, fecha: LocalDate, peso: Double, notas: String?) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updatePeso(id, fecha, peso, notas)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPesos()
            }
            _isLoading.value = false
        }
    }

    fun deletePeso(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deletePeso(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPesos()
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
