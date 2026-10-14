package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import io.github.rikoappdev.composepdf.PdfDocumentSpec
import io.github.rikoappdev.composepdf.preview.PreviewRect
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
 * los textos y los rectángulos ya compuestos, así que aquí se puede afirmar que la dosis de un
 * tratamiento está en la ficha y que una vacuna vencida lleva su etiqueta roja. Es la comprobación
 * que faltaba: el PDF se podía generar "bien" y salir sin el dato más importante.
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

    /** Aplicada hace un año y con la próxima dosis pasada: sale vencida. */
    private val vacunaVencida = Vacuna(
        id = null,
        mascotaId = 1L,
        nombre = "Rabia",
        fechaAplicacion = LocalDate.parse("2025-06-01"),
        fechaProximaDosis = LocalDate.parse("2026-05-01"),
        veterinario = "Dra. Ruiz",
        lote = "L-778"
    )

    /** Vence dentro de dos semanas: con el margen de 15 días de las vacunas, avisa. */
    private val vacunaProxima = Vacuna(
        id = null,
        mascotaId = 1L,
        nombre = "Polivalente",
        fechaAplicacion = LocalDate.parse("2025-06-01"),
        fechaProximaDosis = LocalDate.parse("2026-06-01"),
        veterinario = "Dra. Ruiz",
        lote = "L-779"
    )

    private val preventivo = Preventivo(
        id = null,
        mascotaId = 1L,
        tipo = "interna",
        nombre = "Pipeta",
        fechaAplicacion = LocalDate.parse("2026-04-01"),
        fechaProximaDosis = LocalDate.parse("2026-09-25"),
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

    private fun documento(
        version: VersionPdf,
        vacunas: List<Vacuna> = listOf(vacunaVencida, vacunaProxima),
        preventivos: List<Preventivo> = listOf(preventivo),
        revisiones: List<Revision> = listOf(revision),
        tratamientos: List<Tratamiento> = listOf(tratamiento, tratamientoTerminado),
        pesos: List<Peso> = this.pesos,
        notas: List<Nota> = listOf(Nota(null, 1L, "Es alérgica a las pulgas")),
        mascota: Mascota = this.mascota
    ): PdfDocumentSpec = runBlocking {
        ExportadorPdf().documentoFichaMascota(
            mascota = mascota,
            vacunas = vacunas,
            preventivos = preventivos,
            revisiones = revisiones,
            tratamientos = tratamientos,
            pesos = pesos,
            notas = notas,
            version = version,
            localizacion = formato,
            hoy = hoy
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun textosYColores(documento: PdfDocumentSpec): Pair<String, List<Int>> = runBlocking {
        val fuente = Res.readBytes("files/fonts/noto_sans_variable.ttf")
        assertTrue(fuente.isNotEmpty(), "sin fuente no se puede medir el texto")

        val paginas = documento.previewPages(fuente, fuente)
        val operaciones = paginas.flatMap { it.ops }
        val textos = operaciones.filterIsInstance<PreviewText>().map { it.text }
        // Los fondos de las pastillas y de los recuadros; se ignoran los que no tienen relleno.
        val colores = operaciones.filterIsInstance<PreviewRect>().mapNotNull { it.fillArgb }
        textos.joinToString("\n") to colores
    }

    private fun textoDe(version: VersionPdf): String = textosYColores(documento(version)).first

    // --- Ficha completa -------------------------------------------------------------------

    @Test
    fun `la ficha completa lleva la dosis del tratamiento activo`() {
        val todo = textoDe(VersionPdf.COMPLETA)

        assertTrue(todo.contains("Amoxicilina"), "falta el medicamento: [$todo]")
        assertTrue(todo.contains("1 comprimido"), "falta la dosis: [$todo]")
        assertTrue(todo.contains("cada 12 h"), "falta la frecuencia: [$todo]")
        // El rótulo de la columna, no solo el valor: sin él no se sabe qué es "1 comprimido".
        assertTrue(todo.contains("Dosis"), "falta el rótulo de la dosis: [$todo]")
    }

    @Test
    fun `la ficha completa lleva historial, vacunas, visitas y peso`() {
        val todo = textoDe(VersionPdf.COMPLETA)

        assertTrue(todo.contains("Historial de tratamientos"), "falta el historial: [$todo]")
        assertTrue(todo.contains("Metacam"), "falta el tratamiento terminado: [$todo]")
        assertTrue(todo.contains("Rabia"), "falta la vacuna: [$todo]")
        assertTrue(todo.contains("L-778"), "falta el lote: [$todo]")
        assertTrue(todo.contains("Pipeta"), "falta el preventivo: [$todo]")
        assertTrue(todo.contains("Repetir análisis en 6 meses"), "faltan las notas de la visita: [$todo]")
        assertTrue(todo.contains("24,50 kg"), "falta el peso: [$todo]")
        assertTrue(todo.contains("25,00 kg"), "falta el peso ideal: [$todo]")
        assertTrue(todo.contains("Luna"), "falta el nombre de la mascota: [$todo]")
    }

    @Test
    fun `la ficha completa no lleva la tabla de pesos ni la de informes`() {
        val todo = textoDe(VersionPdf.COMPLETA)

        // El histórico completo de pesos son páginas de números que en papel nadie lee.
        assertTrue(!todo.contains("tras el pienso nuevo"), "no debe salir la tabla de pesajes")
        // De un informe adjunto solo se podía enseñar el nombre del archivo: fuera la sección.
        assertTrue(!todo.contains("Informes y notas"), "la sección de informes y notas sobra")
        assertTrue(!todo.contains("Hemograma"), "no debe quedar rastro de los informes")
        // Lo que sí queda: el resumen del peso y el recuento, que remite a la app.
        assertTrue(todo.contains("Pesajes registrados: 2"), "falta el recuento de pesajes: [$todo]")
        assertTrue(todo.contains("Peso actual"), "falta el peso de hoy: [$todo]")
        assertTrue(todo.contains("Primero"), "falta el primer pesaje: [$todo]")
    }

    @Test
    fun `las vacunas y los preventivos salen con su estado`() {
        val (texto, colores) = textosYColores(documento(VersionPdf.COMPLETA))

        assertTrue(texto.contains("Vencida"), "falta la etiqueta de vencida: [$texto]")
        assertTrue(texto.contains("Vence pronto"), "falta la etiqueta de próxima: [$texto]")
        assertTrue(texto.contains("Al día"), "falta la etiqueta de al día: [$texto]")
        // Los colores de las pastillas, los mismos que usa la pantalla (rojo, ámbar y verde).
        assertTrue(ROJO in colores, "la pastilla de vencida no lleva su fondo rojo")
        assertTrue(AMBAR in colores, "la pastilla de vence pronto no lleva su fondo ámbar")
        assertTrue(VERDE in colores, "la pastilla de al día no lleva su fondo verde")
    }

    @Test
    fun `una seccion sin datos no se pinta`() {
        val (texto, _) = textosYColores(
            documento(
                version = VersionPdf.COMPLETA,
                vacunas = emptyList(),
                preventivos = emptyList(),
                revisiones = emptyList(),
                tratamientos = emptyList(),
                pesos = emptyList(),
                notas = emptyList()
            )
        )

        // Ni títulos ni textos de relleno: la ficha se queda en la cabecera y la identificación.
        assertTrue(texto.contains("Luna"), "la identificación sí va siempre: [$texto]")
        for (titulo in listOf(
            "Tratamientos activos",
            "Historial de tratamientos",
            "Vacunas",
            "Preventivos",
            "Revisiones veterinarias",
            "Peso",
            "Informes y notas",
            "A tener en cuenta"
        )) {
            assertTrue(!texto.contains(titulo), "no debería salir la sección [$titulo]: [$texto]")
        }
        for (relleno in listOf(
            "No hay tratamientos registrados",
            "No hay nada programado",
            "Sin datos",
            "—"
        )) {
            assertTrue(!texto.contains(relleno), "no debería salir el relleno [$relleno]: [$texto]")
        }
    }

    // --- Resumen del cuidador -------------------------------------------------------------

    @Test
    fun `el resumen lleva lo que hay que hacer y deja fuera el historial`() {
        val todo = textoDe(VersionPdf.RESUMIDA)

        assertTrue(todo.contains("Amoxicilina") && todo.contains("1 comprimido"))
        assertTrue(todo.contains("Pipeta"), "las próximas dosis van también en el resumen: [$todo]")
        assertTrue(todo.contains("Dosis"), "el resumen también dice cuánto dar: [$todo]")
        assertTrue(todo.contains("Es alérgica a las pulgas"), "las notas son para quien cuida: [$todo]")

        // Lo clínico es cosa del veterinario: diagnósticos e historial no van en esta hoja.
        assertTrue(!todo.contains("Historial de tratamientos"), "el resumen no lleva el historial")
        assertTrue(!todo.contains("Metacam"), "el resumen no lleva tratamientos terminados")
        assertTrue(!todo.contains("Todo correcto"), "el resumen no lleva diagnósticos")
        assertTrue(!todo.contains("Rabia"), "el resumen no lista las vacunas, solo las que tocan")
    }

    @Test
    fun `el resumen sin nada que hacer lo dice una vez`() {
        val (texto, _) = textosYColores(
            documento(
                version = VersionPdf.RESUMIDA,
                vacunas = emptyList(),
                preventivos = emptyList(),
                revisiones = emptyList(),
                tratamientos = emptyList(),
                pesos = emptyList(),
                notas = emptyList()
            )
        )

        assertTrue(
            texto.contains("No hay ningún tratamiento ni ninguna dosis programada"),
            "una hoja en blanco no tranquiliza a nadie: [$texto]"
        )
        assertTrue(!texto.contains("Qué hay que darle"), "sin tratamientos, esa sección no va")
        assertTrue(!texto.contains("Próximas dosis"), "sin dosis pendientes, esa sección no va")
    }

    // --- Render ---------------------------------------------------------------------------

    @Test
    fun `el PDF renderizado es un PDF`() = runBlocking {
        val pdf = ExportadorPdf().exportarFichaMascota(
            mascota = mascota,
            vacunas = listOf(vacunaVencida),
            preventivos = listOf(preventivo),
            revisiones = listOf(revision),
            tratamientos = listOf(tratamiento),
            pesos = pesos,
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

    private companion object {
        // Los mismos fondos que usa la pastilla de estado, y que la ficha en pantalla.
        const val VERDE = 0xFFDCE8DC.toInt()
        const val AMBAR = 0xFFFFB74D.toInt()
        const val ROJO = 0xFFFFDAD6.toInt()
    }
}
