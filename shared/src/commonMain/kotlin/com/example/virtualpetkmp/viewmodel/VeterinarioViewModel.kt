package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.data.VeterinarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VeterinarioViewModel(private val repository: VeterinarioRepository) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _veterinarios = MutableStateFlow<List<Veterinario>>(emptyList())
    val veterinarios: StateFlow<List<Veterinario>> = _veterinarios.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadVeterinarios()
    }

    fun loadVeterinarios() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _veterinarios.value = repository.getAllVeterinarios()
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar veterinarios: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveVeterinario(veterinario: Veterinario, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = if (veterinario.id == null) {
                    repository.insertVeterinario(veterinario)
                } else {
                    repository.updateVeterinario(veterinario)
                }

                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message
                } else {
                    loadVeterinarios()
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteVeterinario(id: Long, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.deleteVeterinario(id)
                loadVeterinarios()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Error al borrar veterinario: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }
}
