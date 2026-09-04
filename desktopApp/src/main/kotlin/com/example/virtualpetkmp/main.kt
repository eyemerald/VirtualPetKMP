package com.example.virtualpetkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.virtualpetkmp.data.DatabaseFactory

fun main() = application {
    val databaseFactory = DatabaseFactory()
    Window(
        onCloseRequest = ::exitApplication,
        title = "VirtualPetKMP",
    ) {
        App(databaseFactory)
    }
}