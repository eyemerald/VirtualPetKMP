package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.data.PreventivoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class PreventivoViewModel(
    private val repository: PreventivoRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _preventivos = MutableStateFlow<List<Preventivo>>(emptyList())
    val preventivos: StateFlow<List<Preventivo>> = _preventivos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPreventivos()
    }

    fun loadPreventivos() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _preventivos.value = repository.getPreventivosByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los preventivos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPreventivo(
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertPreventivo(
                mascotaId,
                tipo,
                nombre,
                fechaAplicacion,
                fechaProximaDosis,
                veterinario,
                lote
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPreventivos()
            }
            _isLoading.value = false
        }
    }

    fun updatePreventivo(
        id: Long,
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updatePreventivo(
                id,
                tipo,
                nombre,
                fechaAplicacion,
                fechaProximaDosis,
                veterinario,
                lote
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPreventivos()
            }
            _isLoading.value = false
        }
    }

    fun deletePreventivo(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deletePreventivo(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadPreventivos()
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
