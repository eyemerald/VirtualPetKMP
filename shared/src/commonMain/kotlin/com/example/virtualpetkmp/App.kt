package com.example.virtualpetkmp

import androidx.compose.runtime.*
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.VacunaRepository
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
import com.example.virtualpetkmp.viewmodel.RevisionViewModel
import com.example.virtualpetkmp.viewmodel.TratamientoViewModel
import com.example.virtualpetkmp.viewmodel.InformeViewModel
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel
import com.example.virtualpetkmp.ui.theme.VirtualPetTheme

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
    val viewModel = remember { MascotaViewModel(repository) }
    val veterinarioViewModel = remember { VeterinarioViewModel(veterinarioRepository) }

    var currentScreen by remember { mutableStateOf("list") }
    var selectedMascotaId by remember { mutableStateOf<Long?>(null) }
    var selectedVeterinarioId by remember { mutableStateOf<Long?>(null) }
    var pantallaVeterinarios by remember { mutableStateOf("lista") }

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
                                    notaRepository = notaRepository
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
                            val vacunaViewModel = remember(mascotaId) { VacunaViewModel(vacunaRepository, mascotaId) }
                            VacunasScreen(
                                viewModel = vacunaViewModel,
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
