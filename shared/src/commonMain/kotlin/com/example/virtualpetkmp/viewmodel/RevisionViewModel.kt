package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.data.RevisionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class RevisionViewModel(
    private val repository: RevisionRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _revisiones = MutableStateFlow<List<Revision>>(emptyList())
    val revisiones: StateFlow<List<Revision>> = _revisiones.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadRevisiones()
    }

    fun loadRevisiones() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _revisiones.value = repository.getRevisionesByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar las revisiones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addRevision(
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertRevision(
                mascotaId,
                fecha,
                motivo,
                diagnostico,
                notas,
                veterinario
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadRevisiones()
            }
            _isLoading.value = false
        }
    }

    fun updateRevision(
        id: Long,
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updateRevision(
                id,
                fecha,
                motivo,
                diagnostico,
                notas,
                veterinario
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadRevisiones()
            }
            _isLoading.value = false
        }
    }

    fun deleteRevision(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deleteRevision(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadRevisiones()
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
