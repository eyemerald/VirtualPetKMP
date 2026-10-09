package com.example.virtualpetkmp.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprueba que **todos los idiomas tienen las mismas claves** que el idioma por defecto.
 *
 * Es la red de seguridad de la internacionalización: añadir un texto a `values/strings.xml` y
 * olvidarlo en los demás idiomas no rompe la compilación (el sistema cae al idioma por defecto
 * y se ve en inglés sin avisar), así que el olvido se detecta aquí.
 *
 * Se leen los XML a mano en vez de usar los recursos generados porque lo que se quiere verificar
 * es justamente la **paridad de los ficheros fuente**, incluidos los idiomas cuyo texto no se
 * puede cargar sin cambiar la configuración regional del entorno de test.
 */
class ParidadTraduccionesTest {

    /** Idiomas soportados: el código de la carpeta `values-<código>`. El inglés es el defecto. */
    private val idiomas = listOf("es", "pt", "fr", "de", "it")

    private fun fichero(codigo: String?): File {
        val relativa = if (codigo == null) {
            "src/commonMain/composeResources/values/strings.xml"
        } else {
            "src/commonMain/composeResources/values-$codigo/strings.xml"
        }
        // El directorio de trabajo del test es el del módulo (`shared/`), pero se admiten las
        // dos raíces posibles para que el test no dependa de cómo se lance Gradle.
        val candidatos = listOf(File(relativa), File("../shared/$relativa"))
        return candidatos.firstOrNull { it.exists() }
            ?: error("No se encontró el catálogo de textos: $relativa")
    }

    private val patronClave = Regex("""<(?:string|plurals)\s+name="([^"]+)"""")

    private fun claves(codigo: String?): List<String> =
        patronClave.findAll(fichero(codigo).readText()).map { it.groupValues[1] }.toList()

    /** Nombre y valor de cada `<string>` de un catálogo. */
    private fun valores(codigo: String?): Map<String, String> =
        Regex("""<string\s+name="([^"]+)"[^>]*>([^<]*)</string>""")
            .findAll(fichero(codigo).readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `todos los idiomas declarados existen`() {
        idiomas.forEach { codigo ->
            assertTrue(
                fichero(codigo).exists(),
                "falta el catálogo de textos del idioma '$codigo' (values-$codigo/strings.xml)"
            )
        }
    }

    @Test
    fun `ninguna clave esta repetida en el mismo catalogo`() {
        // Una clave duplicada hace fallar la generación de recursos con un error poco claro
        // ("Duplicated key"), así que se comprueba aquí, con el nombre de la clave delante.
        listOf(null).plus(idiomas).forEach { codigo ->
            val todas = claves(codigo)
            val repetidas = todas.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

            assertTrue(
                repetidas.isEmpty(),
                "'${codigo ?: "en"}' tiene claves repetidas: ${repetidas.sorted()}"
            )
        }
    }

    @Test
    fun `todos los idiomas tienen exactamente las mismas claves que el idioma por defecto`() {
        val referencia = claves(null)

        idiomas.forEach { codigo ->
            val propias = claves(codigo)
            val faltan = referencia - propias.toSet()
            val sobran = propias.toSet() - referencia.toSet()

            assertTrue(
                faltan.isEmpty(),
                "a '$codigo' le faltan ${faltan.size} textos: ${faltan.sorted().take(10)}"
            )
            assertTrue(
                sobran.isEmpty(),
                "'$codigo' tiene ${sobran.size} textos que ya no existen: ${sobran.sorted().take(10)}"
            )
            // También el orden: mantener las claves alineadas hace que los ficheros se puedan
            // comparar de un vistazo al revisar una traducción.
            assertEquals(referencia, propias, "el orden de las claves de '$codigo' no coincide")
        }
    }

    @Test
    fun `ninguna traduccion se queda con un marcador de posicion cambiado`() {
        fun marcadores(codigo: String?): Map<String, List<String>> {
            val texto = fichero(codigo).readText()
            return Regex("""<string\s+name="([^"]+)"[^>]*>([^<]*)</string>""")
                .findAll(texto)
                .associate { coincidencia ->
                    val clave = coincidencia.groupValues[1]
                    val valor = coincidencia.groupValues[2]
                    clave to Regex("%\\d+\\\$s").findAll(valor).map { it.value }.toList()
                }
                .filterValues { it.isNotEmpty() }
        }

        val referencia = marcadores(null)

        idiomas.forEach { codigo ->
            val propias = marcadores(codigo)
            referencia.forEach { (clave, esperados) ->
                assertEquals(
                    esperados,
                    propias[clave],
                    "los marcadores de '$clave' no coinciden en '$codigo'"
                )
            }
        }
    }

    @Test
    fun `los plurales no llevan el numero dentro, porque la libreria no lo sustituye`() {
        // Si alguien mete un "%d" en un plural, en pantalla sale el literal "%d": el número se
        // compone aparte en Kotlin. Ver ComportamientoRecursosTest para el porqué.
        val texto = fichero(null).readText()
        val bloques = Regex("""<plurals\s+name="[^"]+">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(texto)
            .map { it.groupValues[1] }

        bloques.forEach { bloque ->
            assertTrue(
                !bloque.contains("%d") && !bloque.contains("%1\$s"),
                "un plural tiene un marcador de posición: $bloque"
            )
        }
    }

    @Test
    fun `ningun idioma es una copia del ingles sin traducir`() {
        val ingles = valores(null)

        idiomas.forEach { codigo ->
            val propias = valores(codigo)
            val iguales = ingles.count { (clave, texto) -> propias[clave] == texto }
            val total = ingles.size

            // Unos pocos textos se escriben igual en varios idiomas ("OK", "PDF", "%1$s"...)
            // y no son un olvido, pero una mayoría idéntica sí lo sería.
            assertTrue(
                iguales < total / 3,
                "'$codigo' parece una copia del inglés: $iguales de $total textos sin traducir"
            )
        }
    }

    @Test
    fun `los ejemplos de peso usan el separador decimal de su idioma`() {
        // El peso se guarda siempre en kilos, pero el ejemplo que se enseña al escribir tiene
        // que usar la coma o el punto según el idioma, o el usuario copia el formato equivocado.
        val conComa = listOf("es", "pt", "fr", "de", "it")

        assertEquals("12.4", valores(null)["form_ejemplo_peso"], "el inglés escribe el decimal con punto")
        conComa.forEach { codigo ->
            assertEquals(
                "12,4",
                valores(codigo)["form_ejemplo_peso"],
                "'$codigo' debería mostrar el ejemplo de peso con coma decimal"
            )
        }
    }

    @Test
    fun `ninguna pista de fecha lleva el formato escrito a mano`() {
        // El formato que se le pide al usuario depende de la región (MM/DD en inglés, DD/MM en
        // el resto), así que va siempre como argumento %1$s. Un "DD/MM/AAAA" incrustado en un
        // idioma sería mentira en cuanto cambiara la región.
        val sospechosos = listOf("DD/MM", "MM/DD", "YYYY", "AAAA", "JJ/MM", "TT/MM", "GG/MM")

        listOf(null) .plus(idiomas).forEach { codigo ->
            valores(codigo).forEach { (clave, texto) ->
                val incrustado = sospechosos.any { texto.contains(it) }
                assertTrue(
                    !incrustado,
                    "'${codigo ?: "en"}/$clave' lleva el formato de fecha escrito a mano: [$texto]"
                )
            }
        }

        // Y la única pista de fecha del catálogo sí recibe el formato por argumento.
        assertEquals(1, valores(null)["peso_formato_fechas"]?.split("%1\$s")?.size?.minus(1))
        idiomas.forEach { codigo ->
            assertEquals(
                1,
                valores(codigo)["peso_formato_fechas"]?.split("%1\$s")?.size?.minus(1),
                "'$codigo' debería recibir el formato de fecha como argumento"
            )
        }
    }
}
