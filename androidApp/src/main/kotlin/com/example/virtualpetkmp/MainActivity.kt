package com.example.virtualpetkmp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.virtualpetkmp.data.DatabaseFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val databaseFactory = DatabaseFactory(this)

            // Pedir permiso de notificaciones solo en Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcherPermiso = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { concedido ->
                    // No hacemos nada con el resultado: si concede, podrá recibir notificaciones;
                    // si no, la app sigue funcionando sin notificaciones.
                }

                LaunchedEffect(Unit) {
                    launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            App(databaseFactory)
        }
    }
}