package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MascotaViewModel(private val repository: MascotaRepository) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadMascotas()
    }

    fun loadMascotas() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _mascotas.value = repository.getAllMascotas()
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar mascotas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveMascota(mascota: Mascota, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = if (mascota.id == null) {
                    repository.insertMascota(mascota)
                } else {
                    repository.updateMascota(mascota)
                }
                
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message
                } else {
                    loadMascotas()
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMascota(id: Long, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.deleteMascota(id)
                loadMascotas()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Error al borrar mascota: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
