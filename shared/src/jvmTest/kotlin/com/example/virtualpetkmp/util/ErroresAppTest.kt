package com.example.virtualpetkmp.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprueba que los motivos de error que devuelve la capa de datos se convierten en texto.
 *
 * El entorno de test corre en español, así que se puede fijar el texto esperado. Lo importante
 * que se verifica es que **no se filtre el nombre del enum** a la pantalla: eso pasaría si
 * alguien olvidara una rama del `when`.
 */
class ErroresAppTest {

    @Test
    fun `cada motivo tiene su texto, no el nombre del enum`() = runBlocking {
        ErrorApp.entries.forEach { motivo ->
            val texto = textoErrorApp(motivo)

            assertTrue(texto.isNotBlank(), "el motivo $motivo se quedó sin texto")
            assertTrue(texto != motivo.name, "el motivo $motivo salió sin traducir: [$texto]")
        }
    }

    @Test
    fun `el nombre duplicado se explica en el idioma activo`() = runBlocking {
        assertEquals(
            "Ya existe una mascota con ese nombre",
            textoErrorApp(ErrorApp.NOMBRE_DUPLICADO)
        )
    }

    @Test
    fun `el error de dominio lleva su motivo y no un texto escrito a mano`() {
        val error = ErrorAppException(ErrorApp.ID_REQUERIDO)

        assertEquals(ErrorApp.ID_REQUERIDO, error.error)
        // La excepción no debe llevar el mensaje de cara al usuario: eso lo pone la interfaz.
        assertEquals("ID_REQUERIDO", error.message)
    }
}
