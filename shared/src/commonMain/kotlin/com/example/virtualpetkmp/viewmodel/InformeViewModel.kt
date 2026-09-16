package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.data.InformeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class InformeViewModel(
    private val repository: InformeRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _informes = MutableStateFlow<List<Informe>>(emptyList())
    val informes: StateFlow<List<Informe>> = _informes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadInformes()
    }

    fun loadInformes() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _informes.value = repository.getInformesByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar los informes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addInforme(
        tipo: String,
        descripcion: String?,
        fecha: LocalDate,
        nombreArchivo: String,
        rutaArchivo: String
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertInforme(
                mascotaId,
                tipo,
                descripcion,
                fecha,
                nombreArchivo,
                rutaArchivo
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadInformes()
            }
            _isLoading.value = false
        }
    }

    fun updateInforme(
        id: Long,
        tipo: String,
        descripcion: String?,
        fecha: LocalDate
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updateInforme(
                id,
                tipo,
                descripcion,
                fecha
            )
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadInformes()
            }
            _isLoading.value = false
        }
    }

    fun deleteInforme(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deleteInforme(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadInformes()
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
