package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las horas de las tomas: lo que se propone, lo que se valida y lo que acaba en las alarmas.
 *
 * Son funciones puras a propósito (sin formulario y sin base de datos) porque son la regla que
 * comparten la pantalla, el programador de alarmas y el PDF: si aquí está bien, los tres avisan
 * el mismo número de veces.
 */
class HorasDeTomaTest {

    // --- Horas propuestas ------------------------------------------------------------------

    @Test
    fun `se proponen tantas horas como tomas y todas son horas validas`() {
        (HorasDeToma.MINIMO..HorasDeToma.MAXIMO).forEach { veces ->
            val horas = HorasDeToma.horasPorDefecto(veces)

            assertEquals(veces, horas.size, "para $veces tomas se propusieron ${horas.size} horas")
            horas.forEach { hora ->
                assertTrue(
                    RecordatoriosTratamiento.leerHora(hora) != null,
                    "la hora propuesta no es valida: [$hora]"
                )
            }
            assertFalse(HorasDeToma.hayRepetidas(horas), "se propusieron horas repetidas: $horas")
        }
    }

    @Test
    fun `un numero de tomas fuera del rango se ajusta en vez de reventar`() {
        assertEquals(HorasDeToma.horasPorDefecto(1), HorasDeToma.horasPorDefecto(0))
        assertEquals(HorasDeToma.horasPorDefecto(6), HorasDeToma.horasPorDefecto(99))
    }

    // --- Leer y escribir -------------------------------------------------------------------

    @Test
    fun `el texto guardado se lee y se vuelve a escribir igual`() {
        val texto = "08:00,14:00,22:00"

        assertEquals(listOf("08:00", "14:00", "22:00"), HorasDeToma.leerHoras(texto))
        assertEquals(texto, HorasDeToma.escribirHoras(HorasDeToma.leerHoras(texto)))
    }

    @Test
    fun `una hora suelta se normaliza y una basura se descarta`() {
        // "8:30" es como la escribe una persona; "25:00" no es una hora.
        assertEquals(listOf("08:30", "14:00"), HorasDeToma.leerHoras("8:30, 25:00, 14:00"))
        assertEquals(emptyList(), HorasDeToma.leerHoras("no es una hora"))
        assertEquals(emptyList(), HorasDeToma.leerHoras(null))
        assertNull(HorasDeToma.escribirHoras(emptyList()))
    }

    @Test
    fun `al cambiar una hora se cambia solo esa`() {
        val texto = "08:00,14:00,22:00"

        assertEquals("08:00,15:30,22:00", HorasDeToma.conHoraEn(texto, 1, "15:30"))
        assertEquals("08:00,14:00,22:00", texto, "el texto original no puede cambiar")
    }

    @Test
    fun `ajustar recorta o rellena sin tocar las que ya habia`() {
        assertEquals(listOf("08:00", "14:00"), HorasDeToma.ajustar(listOf("08:00", "14:00", "22:00"), 2))
        assertEquals(listOf("08:00"), HorasDeToma.ajustar(emptyList(), 1))
        val cuatro = HorasDeToma.ajustar(listOf("08:00", "15:00"), 4)
        assertEquals(4, cuatro.size)
        assertEquals(listOf("08:00", "15:00"), cuatro.take(2), "se perdieron las horas que ya habia")
    }

    // --- Validación del formulario ---------------------------------------------------------

    @Test
    fun `un horario completo y sin repetidas es valido`() {
        assertEquals(
            HorasDeToma.ErrorHoras.NINGUNO,
            HorasDeToma.validar(listOf("08:00", "14:00", "22:00"), 3)
        )
    }

    @Test
    fun `dos tomas a la misma hora son un error`() {
        assertEquals(
            HorasDeToma.ErrorHoras.REPETIDAS,
            HorasDeToma.validar(listOf("08:00", "08:00", "22:00"), 3)
        )
    }

    @Test
    fun `una toma sin hora es un error`() {
        assertEquals(
            HorasDeToma.ErrorHoras.INCOMPLETAS,
            HorasDeToma.validar(listOf("08:00", "", "22:00"), 3)
        )
        assertEquals(
            HorasDeToma.ErrorHoras.INCOMPLETAS,
            HorasDeToma.validar(listOf("08:00"), 2)
        )
    }

    // --- Cuántas tomas tiene un tratamiento ------------------------------------------------

    @Test
    fun `un tratamiento de antes de la 1_14 tiene una sola toma a su hora`() {
        val antiguo = tratamiento(notificar = true, hora = "08:30")

        assertEquals(listOf("08:30"), HorasDeToma.horasDelTratamiento(antiguo))
    }

    @Test
    fun `un tratamiento con horas guardadas usa esas y no la columna suelta`() {
        val nuevo = tratamiento(
            notificar = true,
            hora = "08:00",
            vecesAlDia = 3,
            horas = "08:00,14:00,22:00"
        )

        assertEquals(listOf("08:00", "14:00", "22:00"), HorasDeToma.horasDelTratamiento(nuevo))
    }

    @Test
    fun `sin recordatorio no hay ninguna toma que avisar`() {
        assertEquals(
            emptyList(),
            HorasDeToma.horasDelTratamiento(tratamiento(notificar = false, hora = "08:30"))
        )
    }

    @Test
    fun `sin hora y sin horas no hay aviso`() {
        assertEquals(
            emptyList(),
            HorasDeToma.horasDelTratamiento(tratamiento(notificar = true, hora = null))
        )
    }

    private fun tratamiento(
        notificar: Boolean,
        hora: String?,
        vecesAlDia: Int? = null,
        horas: String? = null
    ) = Tratamiento(
        id = 1L,
        mascotaId = 1L,
        nombreMedicamento = "Amoxicilina",
        fechaInicio = LocalDate(2026, 1, 1),
        hora = hora,
        notificar = notificar,
        vecesAlDia = vecesAlDia,
        horas = horas
    )
}
