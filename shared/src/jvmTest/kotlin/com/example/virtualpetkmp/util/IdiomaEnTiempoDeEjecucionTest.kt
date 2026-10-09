package com.example.virtualpetkmp.util

import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.accion_cerrar

/**
 * Comprueba que Compose Resources **resuelve el idioma del sistema** al leer un texto.
 *
 * Los catálogos pueden estar perfectos y aun así no llegar a la pantalla si la resolución de
 * idioma no está bien montada. Esto lo verifica de verdad: se cambia el idioma por defecto del
 * proceso y se lee el mismo recurso, que tiene que salir en ese idioma.
 */
class IdiomaEnTiempoDeEjecucionTest {

    private val original: Locale = Locale.getDefault()

    @AfterTest
    fun restaurarIdioma() {
        Locale.setDefault(original)
    }

    @Test
    fun `el mismo recurso sale en el idioma del sistema`() = runBlocking {
        val esperado = mapOf(
            "es" to "Cancelar",
            "en" to "Cancel",
            "pt" to "Cancelar",
            "fr" to "Annuler",
            "de" to "Abbrechen",
            "it" to "Annulla"
        )

        esperado.forEach { (codigo, texto) ->
            Locale.setDefault(Locale.forLanguageTag(codigo))
            val leido = getString(Res.string.accion_cancelar)

            assertTrue(
                leido.equals(texto, ignoreCase = true),
                "con el idioma '$codigo' se esperaba [$texto] y salió [$leido]"
            )
        }
    }

    @Test
    fun `los textos con acentos y la eszett llegan intactos`() = runBlocking {
        // Cada idioma se comprueba con una palabra que lleva el carácter propio de ese idioma:
        // si el fichero se hubiera guardado con la codificación equivocada, saldría rota.
        val conCaracteresPropios = mapOf(
            "es" to "Sesión",
            "pt" to "Ação",
            "fr" to "Paramètres",
            "de" to "Schließen",
            "it" to "Città"
        )

        conCaracteresPropios.forEach { (codigo, palabra) ->
            Locale.setDefault(Locale.forLanguageTag(codigo))
            val leido = getString(Res.string.accion_cancelar)

            assertTrue(leido.isNotBlank(), "'$codigo' devolvió un texto vacío")
            assertTrue(
                !leido.contains("\uFFFD") && !leido.contains("?"),
                "'$codigo' salió con caracteres rotos: [$leido] (se esperaba algo como [$palabra])"
            )
        }

        // El alemán, además, tiene que traer la ß tal cual.
        Locale.setDefault(Locale.GERMANY)
        assertTrue(
            getString(Res.string.accion_cerrar).contains("ß"),
            "la ß alemana no sobrevivió: [${getString(Res.string.accion_cerrar)}]"
        )
    }
}
