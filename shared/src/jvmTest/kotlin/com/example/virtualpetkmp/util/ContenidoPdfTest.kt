package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.pdf_color
import virtualpetkmp.shared.generated.resources.pdf_microchip
import virtualpetkmp.shared.generated.resources.pdf_peso_actual
import virtualpetkmp.shared.generated.resources.pdf_peso_ideal

/**
 * Lo que se escribe en el PDF.
 *
 * Esta es la parte que de verdad puede estar mal: el dibujo del documento se ve al abrirlo, pero
 * que falte la dosis de un tratamiento, que un tratamiento terminado salga como activo o que las
 * próximas dosis vayan desordenadas no da ningún error — simplemente el PDF miente.
 */
class ContenidoPdfTest {

    private val formato = Localizacion.desdeRegion("es-ES")
    private val hoy = LocalDate.parse("2026-05-20")

    private fun mascota(
        color: String = "Marrón",
        microchip: String? = "123456789",
        pesoIdeal: Double? = null
    ) = Mascota(
        id = 1L,
        nombre = "Luna",
        especie = "perro",
        raza = "mestizo",
        fechaNacimiento = LocalDate.parse("2020-03-15"),
        sexo = "hembra",
        color = color,
        microchip = microchip,
        pesoIdeal = pesoIdeal
    )

    private fun tratamiento(
        medicamento: String = "Amoxicilina",
        dosis: String? = "1 comprimido",
        frecuencia: String? = "cada 12 h",
        inicio: String = "2026-05-10",
        fin: String? = "2026-05-30"
    ) = Tratamiento(
        id = null,
        mascotaId = 1L,
        nombreMedicamento = medicamento,
        dosis = dosis,
        frecuencia = frecuencia,
        fechaInicio = LocalDate.parse(inicio),
        fechaFin = fin?.let { LocalDate.parse(it) }
    )

    private fun vacuna(nombre: String, proxima: String) = Vacuna(
        id = null,
        mascotaId = 1L,
        nombre = nombre,
        fechaAplicacion = LocalDate.parse("2025-01-10"),
        fechaProximaDosis = LocalDate.parse(proxima),
        veterinario = "Dra. Ruiz",
        lote = "L-1"
    )

    private fun preventivo(nombre: String, proxima: String) = Preventivo(
        id = null,
        mascotaId = 1L,
        tipo = "interna",
        nombre = nombre,
        fechaAplicacion = LocalDate.parse("2025-02-01"),
        fechaProximaDosis = LocalDate.parse(proxima),
        veterinario = null,
        lote = null
    )

    private fun peso(fecha: String, kilos: Double, notas: String? = null) = Peso(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse(fecha),
        peso = kilos,
        notas = notas
    )

    // --- Tratamientos ---------------------------------------------------------------------

    @Test
    fun `la dosis va en la fila del tratamiento, en segunda posicion`() {
        val fila = filaTratamientoPdf(tratamiento(), formato)

        assertEquals("Amoxicilina", fila[0])
        assertEquals("1 comprimido", fila[1], "la dosis es el dato que faltaba en el PDF")
        assertEquals("cada 12 h", fila[2])
        assertEquals(5, fila.size)
    }

    @Test
    fun `un tratamiento sin dosis no deja el hueco vacio`() {
        val fila = filaTratamientoPdf(tratamiento(dosis = null), formato)

        assertEquals(SIN_DATO_PDF, fila[1])
    }

    @Test
    fun `una dosis en blanco cuenta como sin dato`() {
        val fila = filaTratamientoPdf(tratamiento(dosis = "   "), formato)

        assertEquals(SIN_DATO_PDF, fila[1])
    }

    @Test
    fun `un tratamiento en curso esta activo y uno terminado no`() {
        val enCurso = tratamiento(inicio = "2026-05-10", fin = "2026-05-30")
        val terminado = tratamiento(inicio = "2026-01-01", fin = "2026-02-01")

        val activos = tratamientosActivosPdf(listOf(enCurso, terminado), hoy)
        val pasados = tratamientosTerminadosPdf(listOf(enCurso, terminado), hoy)

        assertEquals(listOf(enCurso), activos)
        assertEquals(listOf(terminado), pasados)
    }

    @Test
    fun `el primer y el ultimo dia del tratamiento cuentan como activos`() {
        val empiezaHoy = tratamiento(inicio = "2026-05-20", fin = "2026-06-01")
        val acabaHoy = tratamiento(inicio = "2026-05-01", fin = "2026-05-20")

        val activos = tratamientosActivosPdf(listOf(empiezaHoy, acabaHoy), hoy)

        assertEquals(2, activos.size)
    }

    @Test
    fun `un tratamiento sin fecha de fin es cronico y sigue activo`() {
        val cronico = tratamiento(inicio = "2025-01-01", fin = null)

        assertEquals(listOf(cronico), tratamientosActivosPdf(listOf(cronico), hoy))
    }

    @Test
    fun `un tratamiento que empieza manana no esta activo todavia`() {
        val futuro = tratamiento(inicio = "2026-05-21", fin = "2026-06-01")

        assertTrue(tratamientosActivosPdf(listOf(futuro), hoy).isEmpty())
        assertEquals(listOf(futuro), tratamientosTerminadosPdf(listOf(futuro), hoy))
    }

    // --- Próximas dosis -------------------------------------------------------------------

    @Test
    fun `las proximas dosis mezclan vacunas y preventivos en orden de fecha`() {
        val proximas = proximasDosisPdf(
            vacunas = listOf(vacuna("Rabia", "2026-06-15")),
            preventivos = listOf(preventivo("Pipeta", "2026-05-25")),
            hoy = hoy
        )

        assertEquals(listOf("Pipeta", "Rabia"), proximas.map { it.descripcion })
        assertEquals(
            listOf("2026-05-25", "2026-06-15"),
            proximas.map { it.fecha.toString() }
        )
    }

    @Test
    fun `las dosis ya pasadas no se anuncian`() {
        val proximas = proximasDosisPdf(
            vacunas = listOf(vacuna("Rabia", "2026-01-01")),
            preventivos = listOf(preventivo("Pipeta", "2026-05-19")),
            hoy = hoy
        )

        assertTrue(proximas.isEmpty())
    }

    @Test
    fun `una dosis que toca hoy si se anuncia`() {
        val proximas = proximasDosisPdf(
            vacunas = listOf(vacuna("Rabia", "2026-05-20")),
            preventivos = emptyList(),
            hoy = hoy
        )

        assertEquals(1, proximas.size)
    }

    // --- Peso -----------------------------------------------------------------------------

    @Test
    fun `el resumen de peso da primero, ultimo e ideal`() {
        val resumen = resumenDePesoPdf(
            pesos = listOf(
                peso("2026-05-01", 24.0),
                peso("2026-01-01", 22.0),
                peso("2026-03-01", 23.0)
            ),
            pesoIdeal = 25.0,
            formato = formato
        )

        assertEquals(3, resumen.size, "sin mínimos ni máximos intermedios que aporten algo")
        assertTrue(resumen[0].valor.startsWith("22,00"), "el primero es el más antiguo: ${resumen[0].valor}")
        assertTrue(resumen[1].valor.startsWith("24,00"), "el último es el más reciente: ${resumen[1].valor}")
        assertEquals("25,00 kg", resumen[2].valor)
    }

    @Test
    fun `el minimo y el maximo solo salen si no son los extremos`() {
        val resumen = resumenDePesoPdf(
            pesos = listOf(
                peso("2026-01-01", 24.0),
                peso("2026-02-01", 20.0),
                peso("2026-03-01", 27.0),
                peso("2026-04-01", 22.0)
            ),
            pesoIdeal = null,
            formato = formato
        )

        assertEquals(4, resumen.size)
        assertTrue(resumen[2].valor.startsWith("20,00"), "el mínimo: ${resumen[2].valor}")
        assertTrue(resumen[3].valor.startsWith("27,00"), "el máximo: ${resumen[3].valor}")
    }

    @Test
    fun `sin pesajes no hay resumen de peso`() {
        assertTrue(resumenDePesoPdf(emptyList(), pesoIdeal = 25.0, formato = formato).isEmpty())
    }

    // --- Identificación -------------------------------------------------------------------

    @Test
    fun `la identificacion lleva los datos de la mascota y omite lo que no esta`() = runBlocking {
        val datos = datosDeLaMascotaPdf(
            mascota = mascota(microchip = null, pesoIdeal = null),
            edad = "6 años y 2 meses",
            ultimoPeso = null,
            formato = formato
        )

        val valores = datos.map { it.valor }
        assertTrue(valores.contains("Luna"))
        assertTrue(valores.contains("6 años y 2 meses"))
        assertTrue(valores.contains("15/03/2020"))
        assertFalse(
            datos.any { getString(it.etiqueta) == getString(Res.string.pdf_microchip) },
            "sin microchip no debe salir la fila"
        )
        assertFalse(
            datos.any { getString(it.etiqueta) == getString(Res.string.pdf_peso_actual) },
            "sin pesajes no hay peso actual"
        )
    }

    @Test
    fun `la identificacion incluye color, microchip y peso cuando los hay`() = runBlocking {
        val datos = datosDeLaMascotaPdf(
            mascota = mascota(microchip = "999", pesoIdeal = 25.0),
            edad = "6 años",
            ultimoPeso = 24.5,
            formato = formato
        )

        val etiquetas = datos.map { getString(it.etiqueta) }
        assertTrue(etiquetas.contains(getString(Res.string.pdf_color)))
        assertTrue(etiquetas.contains(getString(Res.string.pdf_microchip)))
        assertTrue(etiquetas.contains(getString(Res.string.pdf_peso_actual)))
        assertTrue(etiquetas.contains(getString(Res.string.pdf_peso_ideal)))
    }

    @Test
    fun `un color en blanco no deja una fila vacia`() = runBlocking {
        val datos = datosDeLaMascotaPdf(
            mascota = mascota(color = "  "),
            edad = "6 años",
            ultimoPeso = null,
            formato = formato
        )

        assertFalse(datos.any { getString(it.etiqueta) == getString(Res.string.pdf_color) })
    }

    // --- Revisiones -----------------------------------------------------------------------

    @Test
    fun `la fila de una revision incluye las notas`() {
        val fila = filaRevisionPdf(
            Revision(
                id = null,
                mascotaId = 1L,
                fecha = LocalDate.parse("2026-04-02"),
                motivo = "Revisión anual",
                diagnostico = "Todo correcto",
                notas = "Repetir análisis en 6 meses",
                veterinario = "Dra. Ruiz"
            ),
            formato
        )

        assertContentEquals(
            listOf("02/04/2026", "Revisión anual", "Todo correcto", "Repetir análisis en 6 meses", "Dra. Ruiz"),
            fila
        )
    }

    // --- Nombre del archivo ---------------------------------------------------------------

    @Test
    fun `el nombre del archivo quita lo que no vale en un sistema de archivos`() {
        assertEquals("ficha-Luna.pdf", nombreArchivoPdf("Luna"))
        assertEquals("ficha-Rocky-Balboa.pdf", nombreArchivoPdf("Rocky Balboa"))
        assertEquals("ficha-Nino.pdf", nombreArchivoPdf("Niño"))
        assertEquals("ficha-mascota.pdf", nombreArchivoPdf("   "))
        assertEquals("ficha-mascota.pdf", nombreArchivoPdf("***"))
    }

    @Test
    fun `un nombre larguisimo se recorta`() {
        val nombre = nombreArchivoPdf("a".repeat(200))

        assertTrue(nombre.length <= 50, "nombre demasiado largo: ${nombre.length}")
        assertTrue(nombre.startsWith("ficha-"))
        assertTrue(nombre.endsWith(".pdf"))
    }
}
