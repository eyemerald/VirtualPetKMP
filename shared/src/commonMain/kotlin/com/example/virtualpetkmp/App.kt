package com.example.virtualpetkmp

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.data.VeterinarioRepository
import com.example.virtualpetkmp.ui.FichaMascotaScreen
import com.example.virtualpetkmp.ui.MainScreen
import com.example.virtualpetkmp.ui.MascotaFormScreen
import com.example.virtualpetkmp.ui.MascotaListScreen
import com.example.virtualpetkmp.ui.VeterinarioFormScreen
import com.example.virtualpetkmp.ui.VeterinariosListScreen
import com.example.virtualpetkmp.ui.theme.VirtualPetTheme
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
import com.example.virtualpetkmp.util.rememberProgramadorNotificaciones
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel

/**
 * Raíz de la app.
 *
 * La navegación es mínima: lista de mascotas, ficha y formulario. Todo lo que antes eran
 * pantallas de detalle (vacunas, preventivos, revisiones, tratamientos, informes, notas y
 * peso) se abre ahora en hojas modales dentro de la ficha, así que sus rutas y sus
 * ViewModels ya no existen.
 */
@Composable
fun App(databaseFactory: DatabaseFactory) {
    val database = remember { databaseFactory.createDatabase() }
    val repository = remember { MascotaRepository(database) }
    val notaRepository = remember { NotaRepository(database) }
    val pesoRepository = remember { PesoRepository(database) }
    val vacunaRepository = remember { VacunaRepository(database) }
    val revisionRepository = remember { RevisionRepository(database) }
    val tratamientoRepository = remember { TratamientoRepository(database) }
    val informeRepository = remember { InformeRepository(database) }
    val veterinarioRepository = remember { VeterinarioRepository(database) }
    val preventivoRepository = remember { PreventivoRepository(database) }
    val viewModel = remember { MascotaViewModel(repository) }
    val veterinarioViewModel = remember { VeterinarioViewModel(veterinarioRepository) }
    val programador = rememberProgramadorNotificaciones()

    // Al arrancar se reprograman los avisos de todas las mascotas. Abrir la ficha de una
    // mascota también los reprograma, así que esto es la red de seguridad para el caso de
    // que el usuario no entre en ninguna ficha.
    LaunchedEffect(Unit) {
        try {
            vacunaRepository.getAllVacunas().forEach { vacuna ->
                vacuna.id?.let { id ->
                    ReprogramadorNotificaciones.cancelar(programador, id)
                    ReprogramadorNotificaciones.programarVacuna(programador, vacuna)
                }
            }
            preventivoRepository.getAllPreventivos().forEach { preventivo ->
                preventivo.id?.let { id ->
                    ReprogramadorNotificaciones.cancelar(programador, id)
                    ReprogramadorNotificaciones.programarPreventivo(programador, preventivo)
                }
            }
        } catch (e: Exception) {
            println("Error reprogramando notificaciones al arrancar: ${e.message}")
        }
    }

    var currentScreen by rememberSaveable { mutableStateOf("list") }
    var selectedMascotaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedVeterinarioId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pantallaVeterinarios by rememberSaveable { mutableStateOf("lista") }
    var dialogoExportarFicha by rememberSaveable { mutableStateOf(false) }

    val mascotas by viewModel.mascotas.collectAsState()
    val mascotaActual = mascotas.find { it.id == selectedMascotaId }

    // Un único ViewModel de ficha por mascota, compartido por todas las hojas: así el
    // resumen, las listas y los avisos son los mismos en toda la pantalla.
    val fichaViewModel = selectedMascotaId?.let { mascotaId ->
        remember(mascotaId) {
            FichaMascotaViewModel(
                mascotaId = mascotaId,
                vacunaRepository = vacunaRepository,
                revisionRepository = revisionRepository,
                tratamientoRepository = tratamientoRepository,
                pesoRepository = pesoRepository,
                informeRepository = informeRepository,
                notaRepository = notaRepository,
                preventivoRepository = preventivoRepository,
                programador = programador
            )
        }
    }

    // Iconos flotantes: solo en el detalle de una mascota. En la lista y en el formulario
    // no hacen falta (el formulario tiene su propio botón de guardar), y con la pestaña de
    // Veterinarios se ocultan solos porque MainScreen mira la pestaña activa.
    val enDetalleMascota = currentScreen == "ficha" && mascotaActual != null

    val volverAtras: (() -> Unit)? = when {
        currentScreen == "list" -> null
        currentScreen == "form" -> fun() {
            currentScreen = if (selectedMascotaId != null) "ficha" else "list"
        }
        else -> fun() {
            currentScreen = "list"
            selectedMascotaId = null
        }
    }

    VirtualPetTheme {
        MainScreen(
            mostrarAcciones = enDetalleMascota,
            onVolver = volverAtras,
            onCompartir = if (enDetalleMascota) {
                { dialogoExportarFicha = true }
            } else {
                null
            },
            contenidoMascotas = {
                when (currentScreen) {
                    "list" -> {
                        MascotaListScreen(
                            viewModel = viewModel,
                            onMascotaClick = { id ->
                                selectedMascotaId = id
                                currentScreen = "ficha"
                            },
                            onAddMascota = {
                                selectedMascotaId = null
                                currentScreen = "form"
                            }
                        )
                    }

                    "ficha" -> {
                        val mascota = mascotaActual
                        val ficha = fichaViewModel
                        if (mascota != null && ficha != null) {
                            FichaMascotaScreen(
                                mascota = mascota,
                                viewModel = ficha,
                                mascotaViewModel = viewModel,
                                mostrarDialogoExportar = dialogoExportarFicha,
                                onCambiarDialogoExportar = { visible -> dialogoExportarFicha = visible },
                                onModificar = { currentScreen = "form" }
                            )
                        }
                    }

                    "form" -> {
                        MascotaFormScreen(
                            viewModel = viewModel,
                            mascotaId = selectedMascotaId,
                            onBack = {
                                currentScreen = if (selectedMascotaId != null) "ficha" else "list"
                            }
                        )
                    }
                }
            },
            contenidoVeterinarios = {
                when (pantallaVeterinarios) {
                    "lista" -> {
                        VeterinariosListScreen(
                            viewModel = veterinarioViewModel,
                            onVeterinarioClick = { id ->
                                selectedVeterinarioId = id
                                pantallaVeterinarios = "form"
                            },
                            onAddVeterinario = {
                                selectedVeterinarioId = null
                                pantallaVeterinarios = "form"
                            }
                        )
                    }
                    "form" -> {
                        VeterinarioFormScreen(
                            viewModel = veterinarioViewModel,
                            veterinarioId = selectedVeterinarioId,
                            onBack = {
                                pantallaVeterinarios = "lista"
                                selectedVeterinarioId = null
                            }
                        )
                    }
                }
            }
        )
    }
}
