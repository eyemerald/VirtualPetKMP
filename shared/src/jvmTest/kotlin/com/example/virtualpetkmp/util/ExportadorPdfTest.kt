package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import io.github.rikoappdev.composepdf.PdfDocumentSpec
import io.github.rikoappdev.composepdf.preview.PreviewText
import io.github.rikoappdev.composepdf.preview.previewPages
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.ExperimentalResourceApi
import virtualpetkmp.shared.generated.resources.Res

/**
 * El PDF de verdad: no lo que se decide (eso está en `ContenidoPdfTest`) sino lo que acaba
 * dentro del documento y lo que sale al renderizarlo.
 *
 * La librería `compose-pdf` sabe recorrer un documento sin pintarlo (`previewPages`) y devuelve
 * los textos ya compuestos, así que aquí se puede afirmar que la dosis de un tratamiento está en
 * la ficha. Es la comprobación que faltaba: el PDF se podía generar "bien" y salir sin el dato
 * más importante.
 */
class ExportadorPdfTest {

    private val formato = Localizacion.desdeRegion("es-ES")

    /**
     * La ficha se genera con un día fijo: si el documento leyera el reloj, el test diría cosas
     * distintas según el día en que se ejecute (un tratamiento "en curso" hoy dejaría de estarlo
     * dentro de una semana) y acabaría fallando sin que nadie hubiera tocado nada.
     */
    private val hoy = LocalDate.parse("2026-05-20")

    private val mascota = Mascota(
        id = 1L,
        nombre = "Luna",
        especie = "perro",
        raza = "mestizo",
        fechaNacimiento = LocalDate.parse("2020-03-15"),
        sexo = "hembra",
        color = "Marrón",
        microchip = "123456789",
        pesoIdeal = 25.0
    )

    private val tratamiento = Tratamiento(
        id = null,
        mascotaId = 1L,
        nombreMedicamento = "Amoxicilina",
        dosis = "1 comprimido",
        frecuencia = "cada 12 h",
        fechaInicio = LocalDate.parse("2026-05-18"),
        fechaFin = LocalDate.parse("2026-05-25")
    )

    private val tratamientoTerminado = Tratamiento(
        id = null,
        mascotaId = 1L,
        nombreMedicamento = "Metacam",
        dosis = "0,3 ml",
        frecuencia = "una vez al día",
        fechaInicio = LocalDate.parse("2026-01-10"),
        fechaFin = LocalDate.parse("2026-01-20")
    )

    private val vacuna = Vacuna(
        id = null,
        mascotaId = 1L,
        nombre = "Rabia",
        fechaAplicacion = LocalDate.parse("2025-06-01"),
        fechaProximaDosis = LocalDate.parse("2026-06-01"),
        veterinario = "Dra. Ruiz",
        lote = "L-778"
    )

    private val preventivo = Preventivo(
        id = null,
        mascotaId = 1L,
        tipo = "interna",
        nombre = "Pipeta",
        fechaAplicacion = LocalDate.parse("2026-04-01"),
        fechaProximaDosis = LocalDate.parse("2026-05-25"),
        veterinario = "Dra. Ruiz",
        lote = "P-12"
    )

    private val revision = Revision(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse("2026-03-02"),
        motivo = "Revisión anual",
        diagnostico = "Todo correcto",
        notas = "Repetir análisis en 6 meses",
        veterinario = "Dra. Ruiz"
    )

    private val pesos = listOf(
        Peso(null, 1L, LocalDate.parse("2026-01-01"), 22.0, null),
        Peso(null, 1L, LocalDate.parse("2026-05-01"), 24.5, "tras el pienso nuevo")
    )

    private val informe = Informe(
        id = null,
        mascotaId = 1L,
        tipo = "Analítica",
        descripcion = "Hemograma completo",
        fecha = LocalDate.parse("2026-03-02"),
        nombreArchivo = "analitica.pdf",
        rutaArchivo = "/tmp/analitica.pdf"
    )

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun textos(version: VersionPdf): List<String> {
        val documento: PdfDocumentSpec = ExportadorPdf().documentoFichaMascota(
            mascota = mascota,
            vacunas = listOf(vacuna),
            preventivos = listOf(preventivo),
            revisiones = listOf(revision),
            tratamientos = listOf(tratamiento, tratamientoTerminado),
            pesos = pesos,
            informes = listOf(informe),
            notas = listOf(Nota(id = null, mascotaId = 1L, texto = "Es alérgica a las pulgas")),
            version = version,
            localizacion = formato,
            hoy = hoy
        )

        val fuente = Res.readBytes("files/fonts/noto_sans_variable.ttf")
        assertTrue(fuente.isNotEmpty(), "sin fuente no se puede medir el texto")

        return documento.previewPages(fuente, fuente)
            .flatMap { it.ops }
            .filterIsInstance<PreviewText>()
            .map { it.text }
    }

    @Test
    fun `la ficha completa lleva la dosis del tratamiento activo`() = runBlocking {
        val todo = textos(VersionPdf.COMPLETA).joinToString("\n")

        assertTrue(todo.contains("Amoxicilina"), "falta el medicamento: [$todo]")
        assertTrue(todo.contains("1 comprimido"), "falta la dosis: [$todo]")
        assertTrue(todo.contains("cada 12 h"), "falta la frecuencia: [$todo]")
        // El rótulo de la columna, no solo el valor: sin él no se sabe qué es "1 comprimido".
        assertTrue(todo.contains("Dosis"), "falta el rótulo de la dosis: [$todo]")
    }

    @Test
    fun `la ficha completa lleva historial, vacunas, visitas, peso e informes`() = runBlocking {
        val todo = textos(VersionPdf.COMPLETA).joinToString("\n")

        assertTrue(todo.contains("Historial de tratamientos"), "falta el historial: [$todo]")
        assertTrue(todo.contains("Metacam"), "falta el tratamiento terminado: [$todo]")
        assertTrue(todo.contains("Rabia"), "falta la vacuna: [$todo]")
        assertTrue(todo.contains("L-778"), "falta el lote: [$todo]")
        assertTrue(todo.contains("Pipeta"), "falta el preventivo: [$todo]")
        assertTrue(todo.contains("Repetir análisis en 6 meses"), "faltan las notas de la visita: [$todo]")
        assertTrue(todo.contains("24,50 kg"), "falta el peso: [$todo]")
        assertTrue(todo.contains("25,00 kg"), "falta el peso ideal: [$todo]")
        assertTrue(todo.contains("analitica.pdf"), "falta el informe adjunto: [$todo]")
        assertTrue(todo.contains("Es alérgica a las pulgas"), "falta la nota: [$todo]")
        assertTrue(todo.contains("Luna"), "falta el nombre de la mascota: [$todo]")
    }

    @Test
    fun `el resumen lleva lo que hay que hacer y deja fuera el historial`() = runBlocking {
        val todo = textos(VersionPdf.RESUMIDA).joinToString("\n")

        assertTrue(todo.contains("Amoxicilina") && todo.contains("1 comprimido"))
        assertTrue(todo.contains("Pipeta"), "las próximas dosis van también en el resumen: [$todo]")
        assertTrue(todo.contains("Dosis"), "el resumen también dice cuánto dar: [$todo]")

        // Lo clínico es cosa del veterinario: diagnósticos e historial no van en esta hoja.
        assertTrue(!todo.contains("Historial de tratamientos"), "el resumen no lleva el historial")
        assertTrue(!todo.contains("Metacam"), "el resumen no lleva tratamientos terminados")
        assertTrue(!todo.contains("Todo correcto"), "el resumen no lleva diagnósticos")
    }

    @Test
    fun `el PDF renderizado es un PDF`() = runBlocking {
        val pdf = ExportadorPdf().exportarFichaMascota(
            mascota = mascota,
            vacunas = listOf(vacuna),
            preventivos = listOf(preventivo),
            revisiones = listOf(revision),
            tratamientos = listOf(tratamiento),
            pesos = pesos,
            informes = listOf(informe),
            notas = emptyList(),
            version = VersionPdf.COMPLETA,
            localizacion = formato,
            hoy = hoy
        )

        val cabecera = pdf.take(5).map { it.toInt().toChar() }.joinToString("")
        assertTrue(cabecera.startsWith("%PDF"), "no empieza por %PDF: [$cabecera]")
        assertTrue(pdf.size > 2000, "un PDF con contenido no puede pesar ${pdf.size} bytes")
        // El logo va como imagen embebida: si no apareciera, la cabecera saldría sin marca.
        assertTrue(pdf.decodeToString().contains("/Image"), "el logo no se ha incrustado en el PDF")
    }
}
