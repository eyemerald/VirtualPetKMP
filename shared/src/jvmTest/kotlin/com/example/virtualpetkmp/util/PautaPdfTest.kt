package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Tratamiento
import kotlinx.datetime.LocalDate
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.pdf_horas
import virtualpetkmp.shared.generated.resources.pdf_veces_al_dia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La pauta de un tratamiento en el PDF: cuántas tomas al día y a qué horas.
 *
 * Se prueba la función que decide **qué líneas** salen, no el dibujo: lo que puede estar mal es que
 * un tratamiento de tres tomas salga sin horas, o que un tratamiento antiguo saque una línea de
 * horas vacía que nadie entiende.
 */
class PautaPdfTest {

    @Test
    fun `un tratamiento con horas saca las dos lineas`() {
        val pauta = pautaTratamientoPdf(
            tratamiento(vecesAlDia = 3, horas = "08:00,14:00,22:00")
        )

        assertEquals(2, pauta.size, "faltan lineas de pauta: $pauta")
        assertEquals(Res.string.pdf_veces_al_dia, pauta[0].etiqueta)
        assertEquals("3", pauta[0].valor)
        assertEquals(Res.string.pdf_horas, pauta[1].etiqueta)
        assertEquals("08:00 · 14:00 · 22:00", pauta[1].valor)
    }

    @Test
    fun `un tratamiento sin horas no saca la linea de horas`() {
        val pauta = pautaTratamientoPdf(tratamiento(vecesAlDia = 2, horas = null))

        assertEquals(1, pauta.size, "sin horas solo sale la linea de veces al dia: $pauta")
        assertEquals(Res.string.pdf_veces_al_dia, pauta[0].etiqueta)
        assertEquals("2", pauta[0].valor)
    }

    @Test
    fun `un tratamiento de antes de la 1_14 no saca ninguna linea de pauta`() {
        // Sin vecesAlDia y sin horas: la tabla de siempre ya enseña su frecuencia y su hora.
        assertTrue(pautaTratamientoPdf(tratamiento(vecesAlDia = null, horas = null)).isEmpty())
    }

    @Test
    fun `las horas se escriben normalizadas y separadas por punto medio`() {
        // "8:5" es como lo escribe una persona y el lector de horas lo acepta; en el PDF tiene que
        // salir ya con dos cifras, que es el formato de toda la app.
        val pauta = pautaTratamientoPdf(tratamiento(vecesAlDia = 2, horas = "8:5,20:30"))

        assertEquals("08:05 · 20:30", pauta.single { it.etiqueta == Res.string.pdf_horas }.valor)
    }

    @Test
    fun `una hora que no se puede leer se descarta sin arrastrar a las demas`() {
        val pauta = pautaTratamientoPdf(tratamiento(vecesAlDia = 2, horas = "25:00,20:30"))

        assertEquals("20:30", pauta.single { it.etiqueta == Res.string.pdf_horas }.valor)
    }

    private fun tratamiento(vecesAlDia: Int?, horas: String?) = Tratamiento(
        id = 1L,
        mascotaId = 1L,
        nombreMedicamento = "Amoxicilina",
        dosis = "1 comprimido",
        frecuencia = "con comida",
        fechaInicio = LocalDate(2026, 1, 1),
        hora = "08:00",
        notificar = true,
        vecesAlDia = vecesAlDia,
        horas = horas
    )
}
