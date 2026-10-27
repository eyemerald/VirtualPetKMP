package com.example.virtualpetkmp.ui

import com.example.virtualpetkmp.Mascota
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El buscador de la lista de mascotas.
 *
 * Es lógica pura, así que se prueba sin pantalla: lo que importa es que filtrar por nombre sea
 * predecible. En una protectora se busca escribiendo rápido y con el móvil, así que la comparación
 * no puede depender de mayúsculas ni de acentos.
 */
class BuscadorMascotasTest {

    private fun mascota(nombre: String) = Mascota(
        id = nombre.hashCode().toLong(),
        nombre = nombre,
        especie = "Perro",
        raza = "Mestizo",
        fechaNacimiento = LocalDate(2020, 1, 1),
        sexo = "Hembra",
        color = "Negro",
        microchip = null
    )

    private val mascotas = listOf(
        mascota("Luna"),
        mascota("Nilo"),
        mascota("Pepé"),
        mascota("Peque"),
        mascota("Ñoño")
    )

    @Test
    fun `sin busqueda devuelve todas y en el mismo orden`() {
        assertEquals(mascotas, filtrarMascotas(mascotas, ""))
        assertEquals(mascotas, filtrarMascotas(mascotas, "   "))
    }

    @Test
    fun `filtra por el nombre sin distinguir mayusculas`() {
        assertEquals(listOf("Luna"), filtrarMascotas(mascotas, "luna").map { it.nombre })
        assertEquals(listOf("Luna"), filtrarMascotas(mascotas, "LUNA").map { it.nombre })
    }

    @Test
    fun `encuentra escribiendo sin acentos y con ellos`() {
        assertEquals(listOf("Pepé"), filtrarMascotas(mascotas, "pepe").map { it.nombre })
        assertEquals(listOf("Pepé"), filtrarMascotas(mascotas, "PEPÉ").map { it.nombre })
    }

    @Test
    fun `la enne se encuentra escribiendo n`() {
        assertEquals(listOf("Ñoño"), filtrarMascotas(mascotas, "nono").map { it.nombre })
        assertEquals(listOf("Ñoño"), filtrarMascotas(mascotas, "ñoño").map { it.nombre })
    }

    @Test
    fun `busca por un trozo del nombre`() {
        val encontradas = filtrarMascotas(mascotas, "pe").map { it.nombre }
        assertEquals(listOf("Pepé", "Peque"), encontradas, "debería encontrar las dos que empiezan igual")
    }

    @Test
    fun `si no hay coincidencias devuelve vacio`() {
        assertTrue(filtrarMascotas(mascotas, "zzz").isEmpty())
    }

    @Test
    fun `los espacios de los extremos no cuentan`() {
        assertEquals(listOf("Nilo"), filtrarMascotas(mascotas, "  nilo  ").map { it.nombre })
    }

    @Test
    fun `sin acentos deja el texto en minusculas y sin tildes`() {
        assertEquals("pepe nono", sinAcentos("Pepé Ñoño"))
        assertEquals("agueda", sinAcentos("Ágüeda"))
    }
}
