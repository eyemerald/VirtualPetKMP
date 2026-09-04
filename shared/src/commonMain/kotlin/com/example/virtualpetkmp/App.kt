package com.example.virtualpetkmp

import androidx.compose.runtime.*
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.ui.MascotaFormScreen
import com.example.virtualpetkmp.ui.MascotaListScreen
import com.example.virtualpetkmp.viewmodel.MascotaViewModel

@Composable
fun App(databaseFactory: DatabaseFactory) {
    val database = remember { databaseFactory.createDatabase() }
    val repository = remember { MascotaRepository(database) }
    val viewModel = remember { MascotaViewModel(repository) }

    var currentScreen by remember { mutableStateOf("list") }
    var selectedMascotaId by remember { mutableStateOf<Long?>(null) }

    when (currentScreen) {
        "list" -> {
            MascotaListScreen(
                viewModel = viewModel,
                onMascotaClick = { id ->
                    selectedMascotaId = id
                    currentScreen = "form"
                },
                onAddMascota = {
                    selectedMascotaId = null
                    currentScreen = "form"
                }
            )
        }
        "form" -> {
            MascotaFormScreen(
                viewModel = viewModel,
                mascotaId = selectedMascotaId,
                onBack = {
                    currentScreen = "list"
                    selectedMascotaId = null
                }
            )
        }
    }
}