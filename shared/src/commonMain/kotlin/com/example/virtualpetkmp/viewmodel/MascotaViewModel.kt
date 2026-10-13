package com.example.virtualpetkmp.viewmodel

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.util.ErrorAppException
import com.example.virtualpetkmp.util.textoErrorApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.error_borrar_mascota
import virtualpetkmp.shared.generated.resources.error_cargar_mascotas
import virtualpetkmp.shared.generated.resources.error_desconocido
import virtualpetkmp.shared.generated.resources.error_guardar_foto
import virtualpetkmp.shared.generated.resources.error_guardar_foto_corto
import virtualpetkmp.shared.generated.resources.error_guardar_peso_ideal
import virtualpetkmp.shared.generated.resources.error_guardar_peso_ideal_corto

/**
 * Traduce el fallo de un repositorio al idioma activo.
 *
 * Los repositorios devuelven [ErrorAppException] con un motivo (`ErrorApp`) cuando el fallo es
 * de negocio, y cualquier otra excepción cuando es técnico. Aquí se decide el texto: por eso
 * este ViewModel no guarda mensajes en español.
 */
private suspend fun textoDeFallo(e: Throwable?): String = when (e) {
    is ErrorAppException -> textoErrorApp(e.error)
    else -> e?.message ?: getString(Res.string.error_desconocido)
}

/**
 * Estado de la lista de mascotas: los datos, si está cargando y el último error.
 *
 * Es el único ViewModel que vive durante toda la sesión (se crea en [App]), así que también
 * es quien mantiene el **último peso de cada mascota**, que la lista necesita para dibujar el
 * indicador ▲/▼ frente al peso ideal sin consultar la base de datos mascota a mascota.
 *
 * @param pesoRepository es opcional a propósito: si no se pasa, la lista funciona igual y
 *   simplemente no muestra el indicador de peso. Así el ViewModel sigue siendo usable en
 *   tests y en pantallas que no necesitan pesos.
 * @param dispatcher hilo en el que corre el estado. Se puede cambiar solo para los tests:
 *   `Dispatchers.Main` no existe en jvmTest. En la app se deja el valor por defecto.
 */
class MascotaViewModel(
    private val repository: MascotaRepository,
    private val pesoRepository: PesoRepository? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(dispatcher + Job())

    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas.asStateFlow()

    /**
     * Último peso de cada mascota (por id), para el indicador ▲/▼ frente al peso ideal.
     * Vacío si no hay repositorio de pesos o si ninguna tiene pesajes.
     */
    private val _ultimoPesoPorMascota = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val ultimoPesoPorMascota: StateFlow<Map<Long, Double>> = _ultimoPesoPorMascota.asStateFlow()

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
                // Los pesos se leen aparte: si fallara esta consulta, la lista de mascotas
                // sigue mostrándose (solo se perdería el indicador de peso).
                _ultimoPesoPorMascota.value = try {
                    pesoRepository?.getUltimoPesoDeCadaMascota().orEmpty()
                } catch (e: Exception) {
                    emptyMap()
                }
            } catch (e: Exception) {
                _errorMessage.value = getString(Res.string.error_cargar_mascotas, detalleDe(e))
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
                    _errorMessage.value = textoDeFallo(result.exceptionOrNull())
                } else {
                    loadMascotas()
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = textoDeFallo(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cambia solo la foto de una mascota ya guardada y recarga la lista para que el
     * cambio se refleje en todas las pantallas que la observan.
     */
    fun updateFoto(id: Long, rutaFoto: String?) {
        scope.launch {
            _errorMessage.value = null
            try {
                val result = repository.updateFoto(id, rutaFoto)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.let { textoDeFallo(it) }
                        ?: getString(Res.string.error_guardar_foto_corto)
                } else {
                    loadMascotas()
                }
            } catch (e: Exception) {
                _errorMessage.value = getString(Res.string.error_guardar_foto, detalleDe(e))
            }
        }
    }

    /**
     * Cambia solo el peso ideal de una mascota ya guardada y recarga la lista. Se llama
     * desde la hoja de Peso, para poder fijarlo o quitarlo sin abrir el formulario entero.
     * `null` lo deja sin configurar.
     */
    fun updatePesoIdeal(id: Long, pesoIdeal: Double?) {
        scope.launch {
            _errorMessage.value = null
            try {
                val result = repository.updatePesoIdeal(id, pesoIdeal)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.let { textoDeFallo(it) }
                        ?: getString(Res.string.error_guardar_peso_ideal_corto)
                } else {
                    loadMascotas()
                }
            } catch (e: Exception) {
                _errorMessage.value = getString(Res.string.error_guardar_peso_ideal, detalleDe(e))
            }
        }
    }

    fun deleteMascota(id: Long, onSuccess: () -> Unit) {        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.deleteMascota(id)
                loadMascotas()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = getString(Res.string.error_borrar_mascota, detalleDe(e))
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

/** Detalle técnico del fallo para los mensajes "No se pudo hacer X: <detalle>". */
private fun detalleDe(e: Throwable): String = e.message ?: e::class.simpleName ?: "?"
