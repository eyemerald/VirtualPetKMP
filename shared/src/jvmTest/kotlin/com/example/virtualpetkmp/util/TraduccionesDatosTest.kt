package com.example.virtualpetkmp.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Traducción de los valores que viven en la base de datos.
 *
 * Lo que se comprueba aquí no es solo que se traduzca, sino **lo que NO se toca**: el valor
 * guardado tiene que seguir siendo el español, porque hay código que compara con él para
 * rellenar el formulario. Si esta capa escribiera en la base de datos, las mascotas ya
 * registradas perderían su raza.
 */
class TraduccionesDatosTest {

    @Test
    fun `en espanol se devuelve el valor guardado sin tocar`() {
        assertEquals("Perro", TraduccionesDatos.etiqueta("Perro", Idioma.ESPANOL))
        assertEquals("Labrador Retriever", TraduccionesDatos.etiqueta("Labrador Retriever", Idioma.ESPANOL))
        assertEquals("Pipeta", TraduccionesDatos.etiqueta("Pipeta", Idioma.ESPANOL))
    }

    @Test
    fun `las especies y razas mas comunes se traducen`() {
        assertEquals("Dog", TraduccionesDatos.etiqueta("Perro", Idioma.INGLES))
        assertEquals("Cat", TraduccionesDatos.etiqueta("Gato", Idioma.INGLES))
        assertEquals("German Shepherd", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.INGLES))
        assertEquals("Hund", TraduccionesDatos.etiqueta("Perro", Idioma.ALEMAN))
        assertEquals("Chien", TraduccionesDatos.etiqueta("Perro", Idioma.FRANCES))
        assertEquals("Cão", TraduccionesDatos.etiqueta("Perro", Idioma.PORTUGUES))
        assertEquals("Cane", TraduccionesDatos.etiqueta("Perro", Idioma.ITALIANO))
    }

    @Test
    fun `un valor escrito a mano por el usuario se deja tal cual`() {
        // El usuario puede escribir su propia raza: no debe quedarse en blanco ni convertirse
        // en un texto genérico.
        assertEquals("Cruce de galgo", TraduccionesDatos.etiqueta("Cruce de galgo", Idioma.INGLES))
        assertEquals("Cruce de galgo", TraduccionesDatos.etiqueta("Cruce de galgo", Idioma.ALEMAN))
    }

    @Test
    fun `especie y raza se componen juntas y traducidas`() {
        assertEquals(
            "Perro - Labrador Retriever",
            TraduccionesDatos.especieYRaza("Perro", "Labrador Retriever", Idioma.ESPANOL)
        )
        assertEquals(
            "Dog - Labrador Retriever",
            TraduccionesDatos.especieYRaza("Perro", "Labrador Retriever", Idioma.INGLES)
        )
        assertEquals(
            "Hund - Deutscher Schäferhund",
            TraduccionesDatos.especieYRaza("Perro", "Pastor Alemán", Idioma.ALEMAN)
        )
    }

    @Test
    fun `si no hay raza solo se muestra la especie`() {
        // Ocurre cuando la especie es "Otro" y el usuario no escribió raza.
        assertEquals("Perro", TraduccionesDatos.especieYRaza("Perro", "", Idioma.ESPANOL))
        assertEquals("Dog", TraduccionesDatos.especieYRaza("Perro", "", Idioma.INGLES))
    }

    @Test
    fun `todas las especies tienen su entrada en los cinco idiomas`() {
        // Lo importante es que la entrada EXISTA. Que el texto cambie o no respecto al español
        // no sirve como comprobación: hay palabras idénticas en varios idiomas ("Gato" y "Ave"
        // en portugués, "Roedor" también) y eso no es un olvido.
        val especies = listOf("Perro", "Gato", "Ave", "Conejo", "Roedor", "Reptil")
        val idiomas = listOf(
            Idioma.INGLES,
            Idioma.PORTUGUES,
            Idioma.FRANCES,
            Idioma.ALEMAN,
            Idioma.ITALIANO
        )

        idiomas.forEach { idioma ->
            especies.forEach { especie ->
                assertTrue(
                    TraduccionesDatos.tieneTraduccion(especie, idioma),
                    "falta la traducción de '$especie' en $idioma"
                )
            }
        }
    }

    @Test
    fun `las traducciones definidas nunca se quedan en espanol por descuido`() {
        // Se comprueban solo los idiomas donde hay una forma claramente distinta, para que el
        // test siga cazando olvidos sin marcar como error las palabras que coinciden.
        assertEquals("Perro", TraduccionesDatos.etiqueta("Perro", Idioma.ESPANOL))
        listOf(
            Idioma.INGLES to "Dog",
            Idioma.PORTUGUES to "Cão",
            Idioma.FRANCES to "Chien",
            Idioma.ALEMAN to "Hund",
            Idioma.ITALIANO to "Cane"
        ).forEach { (idioma, esperado) ->
            assertEquals(esperado, TraduccionesDatos.etiqueta("Perro", idioma), "en $idioma")
        }

        // Y las razas compuestas, que son donde más fácil es dejarse una.
        assertEquals("German Shepherd", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.INGLES))
        assertEquals("Deutscher Schäferhund", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.ALEMAN))
        assertEquals("Berger allemand", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.FRANCES))
        assertEquals("Pastor Alemão", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.PORTUGUES))
        assertEquals("Pastore tedesco", TraduccionesDatos.etiqueta("Pastor Alemán", Idioma.ITALIANO))
    }

    @Test
    fun `los tipos de preventivo y el sexo tambien se traducen`() {
        assertEquals("Spot-on treatment", TraduccionesDatos.etiqueta("Pipeta", Idioma.INGLES))
        assertEquals("Entwurmung", TraduccionesDatos.etiqueta("Desparasitación interna", Idioma.ALEMAN))
        assertEquals("Male", TraduccionesDatos.etiqueta("Macho", Idioma.INGLES))
        assertEquals("Femelle", TraduccionesDatos.etiqueta("Hembra", Idioma.FRANCES))
    }
}
