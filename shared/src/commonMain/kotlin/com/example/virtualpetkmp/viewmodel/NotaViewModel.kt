package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.data.NotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotaViewModel(
    private val repository: NotaRepository,
    private val mascotaId: Long
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadNotas()
    }

    fun loadNotas() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _notas.value = repository.getNotasByMascotaId(mascotaId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar notas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addNota(texto: String) {
        if (texto.isBlank()) {
            _errorMessage.value = "Escribe el texto de la nota antes de añadirla"
            return
        }
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.insertNota(mascotaId, texto)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadNotas()
            }
            _isLoading.value = false
        }
    }

    fun updateNota(id: Long, texto: String) {
        if (texto.isBlank()) {
            _errorMessage.value = "El texto no puede estar vacío"
            return
        }
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.updateNota(id, texto)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadNotas()
            }
            _isLoading.value = false
        }
    }

    fun deleteNota(id: Long) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val resultado = repository.deleteNota(id)
            if (resultado.isFailure) {
                _errorMessage.value = resultado.exceptionOrNull()?.message
            } else {
                loadNotas()
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
