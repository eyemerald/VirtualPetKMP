package com.example.virtualpetkmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtualpetkmp.util.cargarImagenDesdeRuta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_foto_de
import virtualpetkmp.shared.generated.resources.a11y_foto_de_cambiar
import virtualpetkmp.shared.generated.resources.a11y_foto_mascota
import virtualpetkmp.shared.generated.resources.mascota_cambiar_foto

/** Tamaño por defecto del avatar (ficha y formulario). */
val TAMANO_AVATAR_FICHA: Dp = 80.dp

/**
 * Foto de la mascota en un cuadrado de esquinas redondeadas (tipo "squircle"). Si no hay
 * foto (o no se puede leer) muestra un marcador con la inicial de su nombre, para que el
 * hueco nunca quede vacío.
 *
 * No lleva ningún icono ni adorno encima: cuando [onCambiarFoto] no es null, todo el
 * cuadrado es pulsable para abrir las opciones de foto.
 *
 * @param onCambiarFoto si no es null, el cuadrado completo abre el selector de foto.
 */
@Composable
fun AvatarMascota(
    nombre: String,
    rutaFoto: String?,
    modifier: Modifier = Modifier,
    tamano: Dp = TAMANO_AVATAR_FICHA,
    onCambiarFoto: (() -> Unit)? = null
) {
    // Se recarga la imagen cada vez que cambia la ruta (por ejemplo al elegir otra foto).
    var imagen by remember(rutaFoto) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember(rutaFoto) { mutableStateOf(!rutaFoto.isNullOrBlank()) }

    LaunchedEffect(rutaFoto) {
        cargando = !rutaFoto.isNullOrBlank()
        imagen = withContext(Dispatchers.IO) { cargarImagenDesdeRuta(rutaFoto) }
        cargando = false
    }

    val tieneFoto = !rutaFoto.isNullOrBlank()
    val puedeCambiar = onCambiarFoto != null
    // Descripción para lectores de pantalla: cambia según haya foto y se pueda cambiar.
    val descripcionFoto = when {
        nombre.isBlank() -> stringResource(Res.string.a11y_foto_mascota)
        puedeCambiar -> stringResource(Res.string.a11y_foto_de_cambiar, nombre)
        else -> stringResource(Res.string.a11y_foto_de, nombre)
    }
    val etiquetaCambiarFoto = stringResource(Res.string.mascota_cambiar_foto)
    val forma = RoundedCornerShape(percent = 28)

    Box(
        modifier = modifier
            .size(tamano)
            .clip(forma)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = forma
            )
            .let { base ->
                if (onCambiarFoto != null) {
                    base.clickable(onClickLabel = etiquetaCambiarFoto, role = Role.Button, onClick = onCambiarFoto)
                } else {
                    base
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val foto = imagen
        when {
            foto != null -> Image(
                bitmap = foto,
                contentDescription = descripcionFoto,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Mientras se decodifica (o si falló la lectura) se ve el marcador.
            cargando || !tieneFoto -> MarcadorInicial(nombre = nombre, tamano = tamano)
        }
    }
}

@Composable
private fun MarcadorInicial(nombre: String, tamano: Dp) {
    val inicial = nombre.trim().take(1).uppercase()
    if (inicial.isBlank()) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(tamano * 0.4f)
        )
        return
    }

    Text(
        text = inicial,
        fontSize = (tamano.value * 0.36f).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}
