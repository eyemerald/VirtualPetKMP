package com.example.virtualpetkmp

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.VeterinarioRepository
import com.example.virtualpetkmp.ui.MascotaFormScreen
import com.example.virtualpetkmp.ui.MascotaListScreen
import com.example.virtualpetkmp.ui.FichaMascotaScreen
import com.example.virtualpetkmp.ui.NotasScreen
import com.example.virtualpetkmp.ui.PesosScreen
import com.example.virtualpetkmp.ui.VacunasScreen
import com.example.virtualpetkmp.ui.PreventivosScreen
import com.example.virtualpetkmp.ui.RevisionesScreen
import com.example.virtualpetkmp.ui.TratamientosScreen
import com.example.virtualpetkmp.ui.InformesScreen
import com.example.virtualpetkmp.ui.VeterinariosListScreen
import com.example.virtualpetkmp.ui.VeterinarioFormScreen
import com.example.virtualpetkmp.ui.MainScreen
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import com.example.virtualpetkmp.viewmodel.NotaViewModel
import com.example.virtualpetkmp.viewmodel.PesoViewModel
import com.example.virtualpetkmp.viewmodel.VacunaViewModel
import com.example.virtualpetkmp.viewmodel.PreventivoViewModel
import com.example.virtualpetkmp.viewmodel.RevisionViewModel
import com.example.virtualpetkmp.viewmodel.TratamientoViewModel
import com.example.virtualpetkmp.viewmodel.InformeViewModel
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel
import com.example.virtualpetkmp.ui.theme.VirtualPetTheme
import com.example.virtualpetkmp.util.rememberProgramadorNotificaciones
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    // Al arrancar la app, reprogramar todas las notificaciones
    LaunchedEffect(Unit) {
        try {
            // Cancelar y reprogramar todas las vacunas
            val todasVacunas = vacunaRepository.getAllVacunas()
            todasVacunas.forEach { vacuna ->
                ReprogramadorNotificaciones.cancelar(programador, vacuna.id ?: return@forEach)
                ReprogramadorNotificaciones.programarVacuna(programador, vacuna)
            }

            // Cancelar y reprogramar todos los preventivos
            val todosPreventivos = preventivoRepository.getAllPreventivos()
            todosPreventivos.forEach { preventivo ->
                ReprogramadorNotificaciones.cancelar(programador, preventivo.id ?: return@forEach)
                ReprogramadorNotificaciones.programarPreventivo(programador, preventivo)
            }
        } catch (e: Exception) {
            println("Error reprogramando notificaciones al arrancar: ${e.message}")
        }
    }

    var currentScreen by rememberSaveable { mutableStateOf("list") }
    var selectedMascotaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedVeterinarioId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pantallaVeterinarios by rememberSaveable { mutableStateOf("lista") }

    val mascotas by viewModel.mascotas.collectAsState()

    VirtualPetTheme {
        MainScreen(
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
                        val mascotaId = selectedMascotaId
                        val mascota = mascotas.find { it.id == mascotaId }
                        if (mascotaId != null && mascota != null) {
                            val fichaViewModel = remember(mascotaId) {
                                FichaMascotaViewModel(
                                    mascotaId = mascotaId,
                                    vacunaRepository = vacunaRepository,
                                    revisionRepository = revisionRepository,
                                    tratamientoRepository = tratamientoRepository,
                                    pesoRepository = pesoRepository,
                                    informeRepository = informeRepository,
                                    notaRepository = notaRepository,
                                    preventivoRepository = preventivoRepository
                                )
                            }
                            FichaMascotaScreen(
                                mascota = mascota,
                                viewModel = fichaViewModel,
                                onBack = {
                                    currentScreen = "list"
                                    selectedMascotaId = null
                                },
                                onModificar = {
                                    currentScreen = "form"
                                },
                                onNavegar = { destino ->
                                    currentScreen = destino
                                }
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
                    "notas" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val notaViewModel = remember(mascotaId) { NotaViewModel(notaRepository, mascotaId) }
                            NotasScreen(
                                viewModel = notaViewModel,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "pesos" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val pesoViewModel = remember(mascotaId) { PesoViewModel(pesoRepository, mascotaId) }
                            PesosScreen(
                                viewModel = pesoViewModel,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "vacunas" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val vacunaViewModel = remember(mascotaId) { VacunaViewModel(vacunaRepository, mascotaId, programador) }
                            val mascota = mascotas.find { it.id == mascotaId }
                            val nombreMascota = mascota?.nombre ?: ""
                            VacunasScreen(
                                viewModel = vacunaViewModel,
                                nombreMascota = nombreMascota,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "preventivos" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val preventivoViewModel = remember(mascotaId) { PreventivoViewModel(preventivoRepository, mascotaId, programador) }
                            val mascota = mascotas.find { it.id == mascotaId }
                            val nombreMascota = mascota?.nombre ?: ""
                            PreventivosScreen(
                                viewModel = preventivoViewModel,
                                nombreMascota = nombreMascota,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "revisiones" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val revisionViewModel = remember(mascotaId) { RevisionViewModel(revisionRepository, mascotaId) }
                            RevisionesScreen(
                                viewModel = revisionViewModel,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "tratamientos" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val tratamientoViewModel = remember(mascotaId) { TratamientoViewModel(tratamientoRepository, mascotaId) }
                            TratamientosScreen(
                                viewModel = tratamientoViewModel,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
                    }
                    "informes" -> {
                        val mascotaId = selectedMascotaId
                        if (mascotaId != null) {
                            val informeViewModel = remember(mascotaId) { InformeViewModel(informeRepository, mascotaId) }
                            InformesScreen(
                                viewModel = informeViewModel,
                                onBack = {
                                    currentScreen = "ficha"
                                }
                            )
                        }
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
