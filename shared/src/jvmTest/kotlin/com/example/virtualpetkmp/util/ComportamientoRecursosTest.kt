package com.example.virtualpetkmp.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getPluralString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.exportar_ok_guardado
import virtualpetkmp.shared.generated.resources.ficha_vencidas

/**
 * Caracteriza cómo resuelve Compose Multiplatform los recursos, porque tiene dos
 * comportamientos que no son evidentes y que ya nos han mordido una vez:
 *
 * 1. **Una cadena normal SÍ sustituye sus argumentos** (`%1$s`, `%2$s`).
 * 2. **Un plural NO los sustituye**, aunque se le pasen. Solo elige la forma según la cantidad.
 *
 * Por eso las pastillas de la ficha montan el número aparte del plural. Si algún día se
 * "arregla" ese código pasando el número al recurso, este test lo avisa: no es un descuido,
 * es cómo funciona la librería en esta versión.
 */
class ComportamientoRecursosTest {

    @Test
    fun `una cadena con marcadores si sustituye los argumentos`() = runBlocking {
        val texto = getString(Res.string.exportar_ok_guardado, "/tmp/ficha.pdf")

        assertTrue(texto.contains("/tmp/ficha.pdf"), "el argumento no se sustituyó: [$texto]")
        assertTrue(!texto.contains("%1\$s"), "quedó el marcador sin sustituir: [$texto]")
    }

    @Test
    fun `una cadena sin argumentos se devuelve tal cual`() = runBlocking {
        // El valor depende del idioma del entorno de test, así que no se fija el texto: lo que
        // importa es que no venga vacío ni con marcadores.
        val texto = getString(Res.string.accion_cancelar)

        assertTrue(texto.isNotBlank())
        assertTrue(!texto.contains("%"), "no debería tener marcadores: [$texto]")
    }

    @Test
    fun `un plural elige la forma por la cantidad`() = runBlocking {
        // El test corre en español, así que las formas son "vencida" / "vencidas".
        val singular = getPluralString(Res.plurals.ficha_vencidas, 1)
        val plural = getPluralString(Res.plurals.ficha_vencidas, 2)

        assertTrue(singular.isNotBlank() && plural.isNotBlank())
        assertTrue(singular != plural, "singular y plural no deberían coincidir: [$singular]")
    }

    @Test
    fun `un plural NO sustituye argumentos, por eso el numero va aparte`() = runBlocking {
        // Se le pasa el 2 como argumento y aun así el texto sale sin sustituir: la librería
        // ignora los argumentos en los plurales. De ahí que el número se componga en el código.
        val conArgumento = getPluralString(Res.plurals.ficha_vencidas, 2, 2)

        assertEquals(
            getPluralString(Res.plurals.ficha_vencidas, 2),
            conArgumento,
            "si esto cambia, la librería empezó a sustituir y se puede simplificar el código"
        )
    }
}
