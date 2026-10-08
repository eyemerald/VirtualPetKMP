package com.example.virtualpetkmp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.virtualpetkmp.data.DatabaseFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Por defecto Android RECORTA la ventana en el lado del recorte de la cámara: en
        // horizontal eso deja una banda sin pintar de ~44 dp junto al borde. Con
        // LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES la app ocupa toda la pantalla y el
        // recorte se trata como área de sistema, que el contenido ya respeta con los insets.
        window.attributes = window.attributes.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

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