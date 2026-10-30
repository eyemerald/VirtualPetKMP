package com.example.virtualpetkmp.util

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.dialogo_elegir_archivo
import virtualpetkmp.shared.generated.resources.dialogo_elegir_copia
import virtualpetkmp.shared.generated.resources.dialogo_elegir_foto
import virtualpetkmp.shared.generated.resources.dialogo_filtro_copia
import virtualpetkmp.shared.generated.resources.dialogo_filtro_imagenes
import virtualpetkmp.shared.generated.resources.dialogo_guardar_copia
import virtualpetkmp.shared.generated.resources.dialogo_guardar_pdf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.StringResource

/**
 * Los textos de los diálogos del sistema del escritorio salen de los recursos de idioma.
 *
 * Estos textos los pinta el `JFileChooser` (una ventana del sistema, no Compose), así que se leen
 * con `getString` fuera de la composición. El test comprueba que **las siete claves resuelven** en
 * el idioma activo: un nombre de recurso mal escrito compilaría igual si se leyera por texto, y
 * aquí se vería como un diálogo sin título.
 */
class TextosDeDialogoTest {

    private val claves: List<Pair<StringResource, String>> = listOf(
        Res.string.dialogo_elegir_archivo to "Seleccionar archivo",
        Res.string.dialogo_guardar_pdf to "Guardar PDF como",
        Res.string.dialogo_elegir_copia to "Elegir copia de seguridad",
        Res.string.dialogo_guardar_copia to "Guardar copia de seguridad",
        Res.string.dialogo_elegir_foto to "Elegir foto de la mascota",
        Res.string.dialogo_filtro_imagenes to "Imágenes",
        Res.string.dialogo_filtro_copia to "Copia de Virtual Pet (*.vpet)"
    )

    @Test
    fun `los textos de los dialogos resuelven en el idioma activo`() = runBlocking {
        claves.forEach { (recurso, esperado) ->
            val texto = getString(recurso)

            assertTrue(texto.isNotBlank(), "la clave ${recurso.key} salió vacía")
            assertEquals(esperado, texto, "cambió el texto de ${recurso.key}")
        }
    }

    @Test
    fun `ninguno de los textos es una clave sin traducir`() = runBlocking {
        claves.forEach { (recurso, _) ->
            val texto = getString(recurso)

            assertTrue(
                !texto.contains("dialogo_") && !texto.startsWith("Res."),
                "el texto de ${recurso.key} parece la clave sin traducir: [$texto]"
            )
        }
    }
}
