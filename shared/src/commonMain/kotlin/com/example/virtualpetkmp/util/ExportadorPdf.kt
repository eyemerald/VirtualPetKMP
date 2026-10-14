package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import io.github.rikoappdev.composepdf.Color
import io.github.rikoappdev.composepdf.ContainerScope
import io.github.rikoappdev.composepdf.FontWeight
import io.github.rikoappdev.composepdf.HorizontalAlignment
import io.github.rikoappdev.composepdf.PageConfig
import io.github.rikoappdev.composepdf.PhotoFit
import io.github.rikoappdev.composepdf.PdfColumn
import io.github.rikoappdev.composepdf.PdfDocumentSpec
import io.github.rikoappdev.composepdf.TableScope
import io.github.rikoappdev.composepdf.TextAlign
import io.github.rikoappdev.composepdf.TextStyle
import io.github.rikoappdev.composepdf.VerticalAlignment
import io.github.rikoappdev.composepdf.dp
import io.github.rikoappdev.composepdf.pdfDocument
import io.github.rikoappdev.composepdf.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.pdf_a_tener_en_cuenta
import virtualpetkmp.shared.generated.resources.pdf_aplicada
import virtualpetkmp.shared.generated.resources.pdf_archivo
import virtualpetkmp.shared.generated.resources.pdf_descripcion
import virtualpetkmp.shared.generated.resources.pdf_diagnostico
import virtualpetkmp.shared.generated.resources.pdf_dosis
import virtualpetkmp.shared.generated.resources.pdf_evolucion_peso
import virtualpetkmp.shared.generated.resources.pdf_fecha
import virtualpetkmp.shared.generated.resources.pdf_ficha_de
import virtualpetkmp.shared.generated.resources.pdf_ficha_veterinaria
import virtualpetkmp.shared.generated.resources.pdf_fin
import virtualpetkmp.shared.generated.resources.pdf_frecuencia
import virtualpetkmp.shared.generated.resources.pdf_generado_el
import virtualpetkmp.shared.generated.resources.pdf_historial_tratamientos
import virtualpetkmp.shared.generated.resources.pdf_informes_notas
import virtualpetkmp.shared.generated.resources.pdf_inicio
import virtualpetkmp.shared.generated.resources.pdf_lote
import virtualpetkmp.shared.generated.resources.pdf_medicamento
import virtualpetkmp.shared.generated.resources.pdf_motivo
import virtualpetkmp.shared.generated.resources.pdf_ninguna_proxima
import virtualpetkmp.shared.generated.resources.pdf_no_tratamientos_activos
import virtualpetkmp.shared.generated.resources.pdf_nombre
import virtualpetkmp.shared.generated.resources.pdf_nota
import virtualpetkmp.shared.generated.resources.pdf_peso
import virtualpetkmp.shared.generated.resources.pdf_preventivos
import virtualpetkmp.shared.generated.resources.pdf_proxima_dosis
import virtualpetkmp.shared.generated.resources.pdf_proximas_dosis
import virtualpetkmp.shared.generated.resources.pdf_que_darle
import virtualpetkmp.shared.generated.resources.pdf_resumen_aviso
import virtualpetkmp.shared.generated.resources.pdf_resumen_cuidador
import virtualpetkmp.shared.generated.resources.pdf_revisiones
import virtualpetkmp.shared.generated.resources.pdf_sin_tratamientos
import virtualpetkmp.shared.generated.resources.pdf_tipo
import virtualpetkmp.shared.generated.resources.pdf_tratamientos_activos
import virtualpetkmp.shared.generated.resources.pdf_vacuna
import virtualpetkmp.shared.generated.resources.pdf_vacunas
import virtualpetkmp.shared.generated.resources.pdf_veterinario

/**
 * Genera el PDF de la ficha de una mascota.
 *
 * Son **dos documentos distintos**, no el mismo con menos secciones:
 *
 * - **Completa** (para el veterinario): identificación, tratamientos en curso con su dosis, el
 *   historial de tratamientos, vacunas y preventivos con lote y veterinario, visitas con
 *   diagnóstico y notas, evolución del peso, informes adjuntos y notas.
 * - **Resumida** (para quien se queda cuidando a la mascota): lo que hay que hacer. Qué darle
 *   (medicamento, **dosis**, cada cuánto y hasta cuándo), qué dosis vienen y qué tener en cuenta.
 *
 * Los rótulos salen de `composeResources` y **lo que se escribe en cada tabla lo decide
 * [ContenidoPdf]**, que tiene sus propios tests: aquí solo se pinta. Es lo que permite
 * comprobar, por ejemplo, que la dosis de un tratamiento llega al documento.
 */
class ExportadorPdf {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun exportarFichaMascota(
        mascota: Mascota,
        vacunas: List<Vacuna>,
        preventivos: List<Preventivo>,
        revisiones: List<Revision>,
        tratamientos: List<Tratamiento>,
        pesos: List<Peso>,
        informes: List<Informe>,
        notas: List<Nota>,
        version: VersionPdf = VersionPdf.COMPLETA,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES"),
        // "Hoy" se recibe en lugar de leer el reloj aquí dentro para que la ficha se pueda
        // generar con una fecha concreta (tests) y para que todos los cálculos del documento
        // —edad, tratamientos en curso, próximas dosis— usen exactamente el mismo día.
        hoy: LocalDate = hoyDelSistema()
    ): ByteArray {
        val documento = documentoFichaMascota(
            mascota = mascota,
            vacunas = vacunas,
            preventivos = preventivos,
            revisiones = revisiones,
            tratamientos = tratamientos,
            pesos = pesos,
            informes = informes,
            notas = notas,
            version = version,
            localizacion = localizacion,
            hoy = hoy
        )

        return try {
            val fuente = cargarFuente()
            documento.render(fuente, fuente)
        } catch (e: Exception) {
            println("[ExportadorPdf] no se pudo renderizar la ficha: ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * Arma el documento **sin renderizarlo**.
     *
     * Está separado de [exportarFichaMascota] para poder comprobar en un test **qué textos
     * acaban dentro**: la librería sabe recorrer el documento con `previewPages`, así que se
     * puede afirmar que la dosis de un tratamiento llega a la ficha sin depender de abrir el PDF.
     */
    @OptIn(ExperimentalResourceApi::class)
    internal suspend fun documentoFichaMascota(
        mascota: Mascota,
        vacunas: List<Vacuna>,
        preventivos: List<Preventivo>,
        revisiones: List<Revision>,
        tratamientos: List<Tratamiento>,
        pesos: List<Peso>,
        informes: List<Informe>,
        notas: List<Nota>,
        version: VersionPdf = VersionPdf.COMPLETA,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES"),
        hoy: LocalDate = hoyDelSistema()
    ): PdfDocumentSpec {
        val edadMeses = calcularEdadMeses(mascota.fechaNacimiento, hoy)
        val edad = localizacion.formatearEdad(edadMeses.anios, edadMeses.meses)
        val ultimoPeso = pesos.maxByOrNull { it.fecha }?.peso

        // --- Contenido, ya decidido y en texto (ver ContenidoPdf) --------------------------
        val identificacion = datosDeLaMascotaPdf(mascota, edad, ultimoPeso, localizacion)
            .map { getString(it.etiqueta) to it.valor }

        val activos = tratamientosActivosPdf(tratamientos, hoy).map { filaTratamientoPdf(it, localizacion) }
        val terminados = tratamientosTerminadosPdf(tratamientos, hoy).map { filaTratamientoPdf(it, localizacion) }
        val filasVacunas = vacunas.sortedBy { it.fechaAplicacion }.map { filaVacunaPdf(it, localizacion) }
        val filasPreventivos = preventivos.sortedBy { it.fechaAplicacion }
            .map { filaPreventivoPdf(it, localizacion) }
        val filasRevisiones = revisiones.sortedByDescending { it.fecha }.map { filaRevisionPdf(it, localizacion) }
        val filasInformes = informes.sortedByDescending { it.fecha }.map { filaInformePdf(it, localizacion) }
        val filasPeso = pesos.sortedByDescending { it.fecha }.map { filaPesoPdf(it, localizacion) }
        val resumenPeso = resumenDePesoPdf(pesos, mascota.pesoIdeal, localizacion)
            .map { getString(it.etiqueta) to it.valor }
        val proximas = proximasDosisPdf(vacunas, preventivos, hoy)
            .map { localizacion.formatearFecha(it.fecha) to it.descripcion }
        val textosNotas = notas.map { it.texto }

        val t = leerTextos(mascota.nombre, localizacion, version, hoy)
        val logo = cargarLogo()

        val documento = pdfDocument(
            PageConfig(
                margin = 36.dp,
                // La ficha completa ocupa varias hojas, así que la numeración ayuda; la cabecera
                // se repite para que cada página diga de qué mascota es.
                pageNumbers = true,
                repeatHeader = true
            )
        ) {
            header {
                row(gap = 8.dp, verticalAlignment = VerticalAlignment.Center) {
                    if (logo != null) {
                        // Los tres huecos llevan peso positivo a propósito: con peso 0 no está
                        // garantizado que la librería reserve sitio para el logo.
                        cell(0.5f) {
                            image(
                                imageBytes = logo,
                                width = 46.dp,
                                height = 42.dp,
                                fit = PhotoFit.Contain,
                                align = HorizontalAlignment.Start
                            )
                        }
                    }
                    cell(3f) {
                        text(t.titulo, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        text(t.subtitulo, TextStyle(fontSize = 8.sp, color = GRIS))
                    }
                    cell(1.5f) {
                        text(t.generadoEl, TextStyle(fontSize = 8.sp, color = GRIS, align = TextAlign.End))
                    }
                }
                divider(color = GRIS_LINEA)
            }

            cajaIdentificacion(identificacion)

            when (version) {
                VersionPdf.COMPLETA -> fichaCompleta(
                    t = t,
                    activos = activos,
                    terminados = terminados,
                    filasVacunas = filasVacunas,
                    filasPreventivos = filasPreventivos,
                    filasRevisiones = filasRevisiones,
                    filasInformes = filasInformes,
                    filasPeso = filasPeso,
                    resumenPeso = resumenPeso,
                    notas = textosNotas
                )

                VersionPdf.RESUMIDA -> resumenCuidador(
                    t = t,
                    activos = activos,
                    proximas = proximas,
                    notas = textosNotas
                )
            }

            if (t.pie.isNotBlank()) {
                spacer(10.dp)
                text(t.pie, TextStyle(fontSize = 7.sp, color = GRIS))
            }
        }

        return documento
    }

    // --- Composición de cada documento ----------------------------------------------------

    /** Datos de la mascota en un recuadro, a dos columnas. */
    private fun ContainerScope.cajaIdentificacion(datos: List<Pair<String, String>>) {
        box(
            padding = 10.dp,
            border = 1.dp,
            borderColor = GRIS_LINEA,
            background = FONDO_SUAVE,
            cornerRadius = 4.dp
        ) {
            datos.chunked(2).forEach { pareja ->
                row(gap = 12.dp) {
                    pareja.forEach { (etiqueta, valor) ->
                        cell(1f) { keyValue(etiqueta, valor) }
                    }
                    // Con un número impar de datos, la última fila deja el hueco a la derecha.
                    if (pareja.size == 1) cell(1f) {}
                }
            }
        }
    }

    /** Ficha para el veterinario: lo clínico, en el orden en que se consulta. */
    private fun ContainerScope.fichaCompleta(
        t: TextosPdf,
        activos: List<List<String>>,
        terminados: List<List<String>>,
        filasVacunas: List<List<String>>,
        filasPreventivos: List<List<String>>,
        filasRevisiones: List<List<String>>,
        filasInformes: List<List<String>>,
        filasPeso: List<List<String>>,
        resumenPeso: List<Pair<String, String>>,
        notas: List<String>
    ) {
        // Primero lo que el veterinario necesita saber ya: qué está tomando ahora mismo. Esta
        // sección sale siempre, aunque esté vacía: "no toma nada" también es información.
        seccion(t.tratamientosActivos)
        if (activos.isEmpty()) {
            text(t.noTratamientosActivos, TextStyle(fontSize = 9.sp, color = GRIS))
        } else {
            tablaTratamientos(t, activos)
        }

        if (terminados.isNotEmpty()) {
            seccion(t.historialTratamientos)
            tablaTratamientos(t, terminados)
        }

        if (filasVacunas.isNotEmpty()) {
            seccion(t.vacunas)
            tabla(
                columns = listOf(
                    PdfColumn(2.4f, t.vacuna),
                    PdfColumn(1.1f, t.aplicada),
                    PdfColumn(1.1f, t.proximaDosis),
                    PdfColumn(1.4f, t.veterinario),
                    PdfColumn(1f, t.lote)
                )
            ) {
                filasVacunas.forEach { fila -> row(fila) }
            }
        }

        if (filasPreventivos.isNotEmpty()) {
            seccion(t.preventivos)
            tabla(
                columns = listOf(
                    PdfColumn(2.4f, t.nombre),
                    PdfColumn(1.1f, t.aplicada),
                    PdfColumn(1.1f, t.proximaDosis),
                    PdfColumn(1.4f, t.veterinario),
                    PdfColumn(1f, t.lote)
                )
            ) {
                filasPreventivos.forEach { fila -> row(fila) }
            }
        }

        if (filasRevisiones.isNotEmpty()) {
            seccion(t.revisiones)
            tabla(
                columns = listOf(
                    PdfColumn(1f, t.fecha),
                    PdfColumn(1.8f, t.motivo),
                    PdfColumn(2f, t.diagnostico),
                    PdfColumn(2f, t.nota),
                    PdfColumn(1.4f, t.veterinario)
                )
            ) {
                filasRevisiones.forEach { fila -> row(fila) }
            }
        }

        if (filasPeso.isNotEmpty()) {
            seccion(t.evolucionPeso)
            // Resumen arriba (lo que se mira de un vistazo) y el detalle debajo.
            resumenPeso.forEach { (etiqueta, valor) -> keyValue(etiqueta, valor) }
            spacer(6.dp)
            tabla(
                columns = listOf(
                    PdfColumn(1f, t.fecha),
                    PdfColumn(1f, t.peso),
                    PdfColumn(3f, t.nota)
                )
            ) {
                filasPeso.forEach { fila -> row(fila) }
            }
        }

        if (filasInformes.isNotEmpty() || notas.isNotEmpty()) {
            seccion(t.informesNotas)
            if (filasInformes.isNotEmpty()) {
                tabla(
                    columns = listOf(
                        PdfColumn(1f, t.fecha),
                        PdfColumn(1.6f, t.tipo),
                        PdfColumn(2.4f, t.descripcion),
                        PdfColumn(1.6f, t.archivo)
                    )
                ) {
                    filasInformes.forEach { fila -> row(fila) }
                }
            }
            if (notas.isNotEmpty()) {
                tabla(
                    columns = listOf(PdfColumn(3f, t.nota))
                ) {
                    notas.forEach { nota -> row(nota) }
                }
            }
        }
    }

    /**
     * Resumen para quien cuida a la mascota: **qué darle y cuándo**, que es a lo que viene a
     * mirar. Sin historial, sin diagnósticos y sin gráficos: eso es cosa del veterinario.
     */
    private fun ContainerScope.resumenCuidador(
        t: TextosPdf,
        activos: List<List<String>>,
        proximas: List<Pair<String, String>>,
        notas: List<String>
    ) {
        seccion(t.queDarle)
        if (activos.isEmpty()) {
            text(t.sinTratamientos, TextStyle(fontSize = 9.sp, color = GRIS))
        } else {
            // La dosis va justo al lado del medicamento y antes de la pauta: es la instrucción.
            tabla(
                columns = listOf(
                    PdfColumn(2.2f, t.medicamento),
                    PdfColumn(1.4f, t.dosis),
                    PdfColumn(2f, t.frecuencia),
                    PdfColumn(1.2f, t.fin)
                )
            ) {
                // De la fila completa del tratamiento solo interesan estas cuatro columnas.
                activos.forEach { fila -> row(fila[0], fila[1], fila[2], fila[4]) }
            }
        }

        seccion(t.proximasDosis)
        if (proximas.isEmpty()) {
            text(t.ningunaProxima, TextStyle(fontSize = 9.sp, color = GRIS))
        } else {
            // Primero **qué** toca y a la derecha **cuándo**: la lista se lee como una tarea
            // ("Pipeta ...... 25/05/2026"), que es lo que viene a buscar quien cuida.
            proximas.forEach { (fecha, descripcion) -> keyValue(descripcion, fecha) }
        }

        if (notas.isNotEmpty()) {
            seccion(t.aTenerEnCuenta)
            // Las notas son texto libre y pueden ser largas: van como lista, no como dato con
            // su valor a la derecha, que dejaría media línea en blanco.
            notas.forEach { nota -> text("• $nota", TextStyle(fontSize = 9.sp)) }
        }
    }

    private fun ContainerScope.seccion(titulo: String) {
        spacer(12.dp)
        text(titulo, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        spacer(4.dp)
    }

    /**
     * Tabla con el estilo común del documento: cabecera gris y filas alternas.
     *
     * Existe para que todas las tablas de la ficha se vean igual: si el estilo se repitiera en
     * cada una, acabarían discrepando.
     */
    private fun ContainerScope.tabla(columns: List<PdfColumn>, build: TableScope.() -> Unit) {
        table(columns = columns, headerBackground = FONDO_CABECERA, zebra = FONDO_ZEBRA, build = build)
    }

    /** Las dos tablas de tratamientos comparten columnas: medicamento, dosis, pauta y fechas. */
    private fun ContainerScope.tablaTratamientos(t: TextosPdf, filas: List<List<String>>) {
        tabla(
            columns = listOf(
                PdfColumn(2.4f, t.medicamento),
                PdfColumn(1.4f, t.dosis),
                PdfColumn(2f, t.frecuencia),
                PdfColumn(1.1f, t.inicio),
                PdfColumn(1.1f, t.fin)
            )
        ) {
            filas.forEach { fila -> row(fila) }
        }
    }

    // --- Recursos ------------------------------------------------------------------------

    /**
     * Logo con el nombre de la app, para la cabecera.
     *
     * Es el que lleva el texto debajo a propósito: en el PDF no hay una etiqueta del sistema
     * poniendo el nombre al lado, así que la marca va completa. El icono de la app usa el otro
     * (solo el corazón), porque ahí el nombre ya lo pone el lanzador.
     */
    private suspend fun cargarLogo(): ByteArray? = try {
        Res.readBytes("drawable/logo_virtualpet.png")
    } catch (_: Exception) {
        null
    }

    /**
     * Fuente del documento. Se busca primero entre los recursos de Compose y, si no está
     * accesible (en Android, desde una corrutina de fondo), por el classpath.
     */
    private suspend fun cargarFuente(): ByteArray {
        val deRecursos = try {
            Res.readBytes("files/fonts/noto_sans_variable.ttf")
        } catch (_: Exception) {
            ByteArray(0)
        }
        if (deRecursos.isNotEmpty()) return deRecursos

        return try {
            val stream = Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("files/fonts/noto_sans_variable.ttf")
            stream?.use { it.readBytes() } ?: ByteArray(0)
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    private companion object {
        val GRIS = Color(0xFF6C757D)
        val GRIS_LINEA = Color(0xFFDDDDDD)
        val FONDO_SUAVE = Color(0xFFF5F7FA)
        val FONDO_CABECERA = Color(0xFFEDF1F5)
        val FONDO_ZEBRA = Color(0xFFFAFBFC)
    }
}

/** Todos los rótulos fijos del documento, leídos una vez y en el idioma activo. */
private data class TextosPdf(
    val titulo: String,
    val subtitulo: String,
    val generadoEl: String,
    val pie: String,
    val tratamientosActivos: String,
    val noTratamientosActivos: String,
    val historialTratamientos: String,
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
    val inicio: String,
    val fin: String,
    val vacunas: String,
    val vacuna: String,
    val aplicada: String,
    val proximaDosis: String,
    val veterinario: String,
    val lote: String,
    val preventivos: String,
    val nombre: String,
    val revisiones: String,
    val fecha: String,
    val motivo: String,
    val diagnostico: String,
    val nota: String,
    val evolucionPeso: String,
    val peso: String,
    val informesNotas: String,
    val tipo: String,
    val descripcion: String,
    val archivo: String,
    val queDarle: String,
    val sinTratamientos: String,
    val proximasDosis: String,
    val ningunaProxima: String,
    val aTenerEnCuenta: String
)

private suspend fun leerTextos(
    nombreMascota: String,
    localizacion: Localizacion,
    version: VersionPdf,
    hoy: LocalDate
): TextosPdf {
    return TextosPdf(
        titulo = getString(Res.string.pdf_ficha_de, nombreMascota),
        subtitulo = when (version) {
            VersionPdf.COMPLETA -> getString(Res.string.pdf_ficha_veterinaria)
            VersionPdf.RESUMIDA -> getString(Res.string.pdf_resumen_cuidador)
        },
        generadoEl = getString(Res.string.pdf_generado_el, localizacion.formatearFecha(hoy)),
        pie = when (version) {
            VersionPdf.COMPLETA -> ""
            VersionPdf.RESUMIDA -> getString(Res.string.pdf_resumen_aviso)
        },
        tratamientosActivos = getString(Res.string.pdf_tratamientos_activos),
        noTratamientosActivos = getString(Res.string.pdf_no_tratamientos_activos),
        historialTratamientos = getString(Res.string.pdf_historial_tratamientos),
        medicamento = getString(Res.string.pdf_medicamento),
        dosis = getString(Res.string.pdf_dosis),
        frecuencia = getString(Res.string.pdf_frecuencia),
        inicio = getString(Res.string.pdf_inicio),
        fin = getString(Res.string.pdf_fin),
        vacunas = getString(Res.string.pdf_vacunas),
        vacuna = getString(Res.string.pdf_vacuna),
        aplicada = getString(Res.string.pdf_aplicada),
        proximaDosis = getString(Res.string.pdf_proxima_dosis),
        veterinario = getString(Res.string.pdf_veterinario),
        lote = getString(Res.string.pdf_lote),
        preventivos = getString(Res.string.pdf_preventivos),
        nombre = getString(Res.string.pdf_nombre),
        revisiones = getString(Res.string.pdf_revisiones),
        fecha = getString(Res.string.pdf_fecha),
        motivo = getString(Res.string.pdf_motivo),
        diagnostico = getString(Res.string.pdf_diagnostico),
        nota = getString(Res.string.pdf_nota),
        evolucionPeso = getString(Res.string.pdf_evolucion_peso),
        peso = getString(Res.string.pdf_peso),
        informesNotas = getString(Res.string.pdf_informes_notas),
        tipo = getString(Res.string.pdf_tipo),
        descripcion = getString(Res.string.pdf_descripcion),
        archivo = getString(Res.string.pdf_archivo),
        queDarle = getString(Res.string.pdf_que_darle),
        sinTratamientos = getString(Res.string.pdf_sin_tratamientos),
        proximasDosis = getString(Res.string.pdf_proximas_dosis),
        ningunaProxima = getString(Res.string.pdf_ninguna_proxima),
        aTenerEnCuenta = getString(Res.string.pdf_a_tener_en_cuenta)
    )
}

enum class VersionPdf {
    RESUMIDA,
    COMPLETA
}

/** El día de hoy según el reloj del dispositivo, con la zona horaria del usuario. */
private fun hoyDelSistema(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
