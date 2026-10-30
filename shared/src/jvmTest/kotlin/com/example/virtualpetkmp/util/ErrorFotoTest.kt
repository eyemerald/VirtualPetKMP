package com.example.virtualpetkmp.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Los errores del selector de foto llegan a la pantalla **traducidos**.
 *
 * El selector es código de plataforma (`actual`), y ahí no se pueden leer los recursos de Compose:
 * antes devolvía frases en español escritas a mano y el usuario las veía tal cual. Ahora devuelve un
 * [ErrorFoto] y la ficha lo convierte con [claveDeErrorFoto]; este test comprueba esa conversión:
 * que **todos** los motivos tienen texto, que ninguno se queda sin rama y que no repiten el de otro.
 */
class ErrorFotoTest {

    @Test
    fun `todos los motivos tienen texto en el idioma activo`() = runBlocking {
        ErrorFoto.entries.forEach { motivo ->
            val texto = org.jetbrains.compose.resources.getString(claveDeErrorFoto(motivo))

            assertTrue(texto.isNotBlank(), "el motivo $motivo se quedó sin texto")
            assertTrue(texto != motivo.name, "el motivo $motivo salió sin traducir: [$texto]")
        }
    }

    @Test
    fun `cada motivo tiene su propia clave`() {
        val claves = ErrorFoto.entries.map { claveDeErrorFoto(it).key }

        assertEquals(
            ErrorFoto.entries.size,
            claves.toSet().size,
            "dos motivos comparten la misma clave de texto: $claves"
        )
    }

    @Test
    fun `los textos no llevan detalles tecnicos ni nombres de clase`() = runBlocking {
        ErrorFoto.entries.forEach { motivo ->
            val texto = org.jetbrains.compose.resources.getString(claveDeErrorFoto(motivo))

            assertTrue(
                !texto.contains("Exception") && !texto.contains("desconocido") && !texto.contains(":"),
                "el texto de $motivo parece llevar el detalle técnico: [$texto]"
            )
        }
    }

    @Test
    fun `copiar y abrir el selector son motivos distintos`() {
        assertTrue(claveDeErrorFoto(ErrorFoto.COPIAR) != claveDeErrorFoto(ErrorFoto.ABRIR_SELECTOR))
        assertTrue(claveDeErrorFoto(ErrorFoto.LEER) != claveDeErrorFoto(ErrorFoto.COPIAR))
    }
}
