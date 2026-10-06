package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    contenidoMascotas: @Composable () -> Unit,
    contenidoVeterinarios: @Composable () -> Unit
) {
    var tabActual by rememberSaveable { mutableStateOf(0) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "VirtualPetKMP",
                    modifier = Modifier.size(80.dp)
                )
            }

            TabRow(selectedTabIndex = tabActual) {
                Tab(
                    selected = tabActual == 0,
                    onClick = { tabActual = 0 },
                    text = { Text("Mascotas") }
                )
                Tab(
                    selected = tabActual == 1,
                    onClick = { tabActual = 1 },
                    text = { Text("Veterinarios") }
                )
            }
            when (tabActual) {
                0 -> contenidoMascotas()
                1 -> contenidoVeterinarios()
            }
        }
    }
}