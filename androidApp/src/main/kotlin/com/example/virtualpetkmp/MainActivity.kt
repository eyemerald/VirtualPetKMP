package com.example.virtualpetkmp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.virtualpetkmp.data.DatabaseFactory

class MainActivity : ComponentActivity() {

    /**
     * Última petición de navegación que llegó desde una notificación.
     *
     * Es estado de Compose y no un simple campo porque la pantalla tiene que reaccionar **en
     * caliente**: si la app ya estaba abierta y el usuario toca otro aviso, no se recrea la
     * activity (es `singleTop`), así que el aviso llega por [onNewIntent] y hay que empujarlo a
     * la interfaz.
     */
    private var solicitud by mutableStateOf<SolicitudNavegacion?>(null)

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

        leerAviso(intent)

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

            App(databaseFactory, solicitud)
        }
    }

    /**
     * La app ya estaba abierta y se tocó otra notificación: no se reinicia, solo se navega.
     *
     * `setIntent` es necesario para que un `getIntent()` posterior (por ejemplo al girar el móvil)
     * devuelva lo mismo que procesó `onNewIntent`.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerAviso(intent)
    }

    /**
     * Traduce los extras del aviso de tratamiento a una petición de navegación.
     *
     * Los extras se **borran** después de leerlos: el intent se conserva entre giros de pantalla y,
     * si no se limpiaran, girar el móvil volvería a llevar al usuario a la ficha de la mascota
     * aunque estuviera en otra pantalla.
     */
    private fun leerAviso(intent: Intent?) {
        if (intent == null) return

        val mascotaId = intent.getLongExtra(EXTRA_AVISO_MASCOTA_ID, -1L)
        val tratamientoId = intent.getLongExtra(EXTRA_AVISO_TRATAMIENTO_ID, -1L)
        if (mascotaId <= 0L) return

        intent.removeExtra(EXTRA_AVISO_MASCOTA_ID)
        intent.removeExtra(EXTRA_AVISO_TRATAMIENTO_ID)

        solicitud = SolicitudNavegacion(
            mascotaId = mascotaId,
            abrirTratamientos = tratamientoId > 0L
        )
    }

    private companion object {
        /** Los mismos nombres que usa `NotificacionReceiver` al montar el intent. */
        const val EXTRA_AVISO_MASCOTA_ID = "aviso_mascota_id"
        const val EXTRA_AVISO_TRATAMIENTO_ID = "aviso_tratamiento_id"
    }
}
