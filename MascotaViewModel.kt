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
}
