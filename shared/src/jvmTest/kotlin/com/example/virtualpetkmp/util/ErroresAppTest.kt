package com.example.virtualpetkmp.util

import java.io.ByteArrayOutputStream
import java.io.PrintStream
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

    // --- El texto que ve el usuario cuando falla algo que no es de dominio ---------------------

    @Test
    fun `una excepcion que no es de dominio sale con texto generico y sin su mensaje`() = runBlocking {
        val tecnico = "SQLITE_ERROR: no such column: rutaArchivo"

        val texto = textoDeFallo(IllegalStateException(tecnico))

        assertEquals("Ha ocurrido un error inesperado", texto)
        assertTrue(
            !texto.contains("SQLITE") && !texto.contains("columna") && !texto.contains(tecnico),
            "el mensaje técnico no puede llegar a la pantalla: [$texto]"
        )
    }

    @Test
    fun `el detalle tecnico no se pierde, va al registro`() = runBlocking {
        val tecnico = "Detalle que solo sirve para depurar"
        val salida = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val outOriginal = System.out
        val errOriginal = System.err

        try {
            System.setOut(PrintStream(salida, true, "UTF-8"))
            System.setErr(PrintStream(error, true, "UTF-8"))
            textoDeFallo(IllegalStateException(tecnico))
        } finally {
            System.setOut(outOriginal)
            System.setErr(errOriginal)
        }

        val registro = salida.toString("UTF-8") + error.toString("UTF-8")
        assertTrue(registro.contains(tecnico), "el detalle técnico tiene que quedar en el registro: [$registro]")
    }

    @Test
    fun `el error de dominio sigue saliendo traducido y no generico`() = runBlocking {
        val texto = textoDeFallo(ErrorAppException(ErrorApp.NOMBRE_DUPLICADO))

        assertEquals("Ya existe una mascota con ese nombre", texto)
        assertTrue(texto != "Ha ocurrido un error inesperado", "un error conocido no puede salir como inesperado")
    }

    @Test
    fun `sin excepcion tambien sale el texto generico`() = runBlocking {
        assertEquals("Ha ocurrido un error inesperado", textoDeFallo(null))
    }
}
