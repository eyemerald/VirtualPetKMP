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
import virtualpetkmp.shared.generated.resources.pdf_primero

/**
 * Lo que se escribe en el PDF.
 *
 * Esta es la parte que de verdad puede estar mal: el dibujo del documento se ve al abrirlo, pero
 * que falte la dosis de un tratamiento, que un tratamiento terminado salga como activo, que una
 * vacuna vencida se pinte como "al día" o que el peso se repita dos veces en la misma hoja no da
 * ningún error — simplemente el PDF miente.
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
        // Uno de los tipos reales del formulario, para que el texto sea el de verdad.
        tipo = "Desparasitación",
        nombre = nombre,
        fechaAplicacion = LocalDate.parse("2025-02-01"),
        fechaProximaDosis = LocalDate.parse(proxima),
        veterinario = null,
        lote = null
    )

    private fun peso(fecha: String, kilos: Double) = Peso(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse(fecha),
        peso = kilos,
        notas = null
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

    // --- Vacunas y preventivos: el estado -------------------------------------------------

    @Test
    fun `una vacuna vencida se marca como vencida`() {
        val ficha = fichaVacunaPdf(vacuna("Rabia", "2026-05-19"), hoy, formato)

        assertEquals(EstadoDosis.VENCIDA, ficha.estado)
        assertEquals("Rabia", ficha.nombre)
        assertEquals("10/01/2025", ficha.aplicada)
        assertEquals("19/05/2026", ficha.proxima)
    }

    @Test
    fun `una vacuna que vence dentro del margen avisa`() {
        // La vacuna avisa con 15 días: del 20/05 al 04/06.
        assertEquals(EstadoDosis.PROXIMA, fichaVacunaPdf(vacuna("Rabia", "2026-06-04"), hoy, formato).estado)
        assertEquals(EstadoDosis.AL_DIA, fichaVacunaPdf(vacuna("Rabia", "2026-06-05"), hoy, formato).estado)
    }

    @Test
    fun `un preventivo usa su propio margen, mas corto que el de la vacuna`() {
        // Los dos avisan con márgenes distintos: la vacuna con 15 días (hasta el 04/06) y el
        // preventivo con 5 (hasta el 25/05). Una dosis del 28/05 en una vacuna ya avisa, y en un
        // preventivo todavía está al día: es justo lo que se vería mal si cada uno tuviera su
        // propia regla escrita a mano.
        assertEquals(EstadoDosis.PROXIMA, fichaVacunaPdf(vacuna("Rabia", "2026-05-28"), hoy, formato).estado)
        assertEquals(EstadoDosis.AL_DIA, fichaPreventivoPdf(preventivo("Pipeta", "2026-05-28"), hoy, formato).estado)

        // Y los bordes de cada margen.
        assertEquals(EstadoDosis.PROXIMA, fichaVacunaPdf(vacuna("Rabia", "2026-06-04"), hoy, formato).estado)
        assertEquals(EstadoDosis.AL_DIA, fichaVacunaPdf(vacuna("Rabia", "2026-06-05"), hoy, formato).estado)
        assertEquals(EstadoDosis.PROXIMA, fichaPreventivoPdf(preventivo("Pipeta", "2026-05-25"), hoy, formato).estado)
        assertEquals(EstadoDosis.AL_DIA, fichaPreventivoPdf(preventivo("Pipeta", "2026-05-26"), hoy, formato).estado)
    }

    @Test
    fun `el preventivo lleva su tipo traducido y la vacuna no`() {
        val conTipo = fichaPreventivoPdf(preventivo("Pipeta", "2026-06-30"), hoy, formato)
        val sinTipo = fichaVacunaPdf(vacuna("Rabia", "2026-06-30"), hoy, formato)

        assertEquals("Desparasitación", conTipo.tipo, "el tipo del preventivo se enseña")
        assertEquals("", sinTipo.tipo)
    }

    @Test
    fun `los datos que faltan de una dosis se marcan, no se dejan en blanco`() {
        val ficha = fichaPreventivoPdf(preventivo("Pipeta", "2026-06-30"), hoy, formato)

        assertEquals(SIN_DATO_PDF, ficha.veterinario)
        assertEquals(SIN_DATO_PDF, ficha.lote)
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
    fun `el resumen de peso abre con el peso actual, que es el ultimo pesaje`() {
        val resumen = resumenDePesoPdf(
            pesos = listOf(
                peso("2026-05-01", 24.0),
                peso("2026-01-01", 22.0),
                peso("2026-03-01", 23.0)
            ),
            pesoIdeal = 25.0,
            formato = formato
        )

        // Actual y primero, y ni mínimo ni máximo porque coinciden con los extremos del rango.
        assertEquals(3, resumen.size, "sin mínimos ni máximos intermedios que aporten algo")
        assertEquals("24,00 kg · 01/05/2026", resumen[0].valor, "el actual es el más reciente")
        assertEquals("22,00 kg · 01/01/2026", resumen[1].valor, "el primero es el más antiguo")
        assertEquals("25,00 kg", resumen[2].valor)
    }

    @Test
    fun `el peso actual no se repite como ultimo`() {
        val resumen = resumenDePesoPdf(
            pesos = listOf(peso("2026-01-01", 22.0), peso("2026-05-01", 24.0)),
            pesoIdeal = null,
            formato = formato
        )

        assertEquals(2, resumen.size)
        assertEquals("24,00 kg · 01/05/2026", resumen[0].valor)
        assertEquals("22,00 kg · 01/01/2026", resumen[1].valor)
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
        assertEquals("22,00 kg · 01/04/2026", resumen[0].valor, "el actual")
        assertEquals("24,00 kg · 01/01/2026", resumen[1].valor, "el primero")
        assertTrue(resumen[2].valor.startsWith("20,00"), "el mínimo: ${resumen[2].valor}")
        assertTrue(resumen[3].valor.startsWith("27,00"), "el máximo: ${resumen[3].valor}")
    }

    @Test
    fun `sin pesajes no hay resumen de peso`() {
        assertTrue(resumenDePesoPdf(emptyList(), pesoIdeal = 25.0, formato = formato).isEmpty())
    }

    @Test
    fun `el peso del cuidador es solo el actual y el ideal`() = runBlocking {
        val datos = pesoActualIdealPdf(
            pesos = listOf(peso("2026-01-01", 22.0), peso("2026-05-01", 24.0)),
            pesoIdeal = 25.0,
            formato = formato
        )

        assertEquals(2, datos.size, "sin primero, mínimo ni máximo: eso es del veterinario")
        assertEquals(getString(Res.string.pdf_peso_actual), getString(datos[0].etiqueta))
        assertEquals(getString(Res.string.pdf_peso_ideal), getString(datos[1].etiqueta))
        assertEquals("24,00 kg · 01/05/2026", datos[0].valor)
    }

    @Test
    fun `sin pesajes el cuidador ve el peso ideal pero no un peso actual inventado`() = runBlocking {
        val datos = pesoActualIdealPdf(pesos = emptyList(), pesoIdeal = 25.0, formato = formato)

        assertEquals(1, datos.size)
        assertEquals(getString(Res.string.pdf_peso_ideal), getString(datos[0].etiqueta))
    }

    // --- Identificación -------------------------------------------------------------------

    @Test
    fun `la identificacion lleva los datos de la mascota y omite lo que no esta`() = runBlocking {
        val datos = datosDeLaMascotaPdf(
            mascota = mascota(microchip = null),
            edad = "6 años y 2 meses",
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
    }

    @Test
    fun `la identificacion no lleva el peso, que tiene su propia seccion`() = runBlocking {
        val datos = datosDeLaMascotaPdf(mascota(), edad = "6 años", formato = formato)

        assertFalse(
            datos.any { getString(it.etiqueta) == getString(Res.string.pdf_peso_actual) },
            "el peso actual se enseña en la sección de peso, no dos veces en la misma hoja"
        )
        assertFalse(datos.any { getString(it.etiqueta) == getString(Res.string.pdf_peso_ideal) })
        assertFalse(datos.any { getString(it.etiqueta) == getString(Res.string.pdf_primero) })
    }

    @Test
    fun `la identificacion incluye el color cuando lo hay`() = runBlocking {
        val datos = datosDeLaMascotaPdf(mascota(microchip = "999"), edad = "6 años", formato = formato)

        assertTrue(datos.any { getString(it.etiqueta) == getString(Res.string.pdf_color) })
        assertTrue(datos.any { getString(it.etiqueta) == getString(Res.string.pdf_microchip) })
    }

    @Test
    fun `un color en blanco no deja una fila vacia`() = runBlocking {
        val datos = datosDeLaMascotaPdf(mascota(color = "  "), edad = "6 años", formato = formato)

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
