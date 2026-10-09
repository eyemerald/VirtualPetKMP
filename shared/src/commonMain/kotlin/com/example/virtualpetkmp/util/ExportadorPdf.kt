package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import io.github.rikoappdev.composepdf.Color
import io.github.rikoappdev.composepdf.FontWeight
import io.github.rikoappdev.composepdf.PageConfig
import io.github.rikoappdev.composepdf.PdfColumn
import io.github.rikoappdev.composepdf.TextAlign
import io.github.rikoappdev.composepdf.TextStyle
import io.github.rikoappdev.composepdf.dp
import io.github.rikoappdev.composepdf.pdfDocument
import io.github.rikoappdev.composepdf.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.pdf_a_tener_en_cuenta
import virtualpetkmp.shared.generated.resources.pdf_aplicada
import virtualpetkmp.shared.generated.resources.pdf_descripcion
import virtualpetkmp.shared.generated.resources.pdf_diagnostico
import virtualpetkmp.shared.generated.resources.pdf_edad
import virtualpetkmp.shared.generated.resources.pdf_especie
import virtualpetkmp.shared.generated.resources.pdf_fecha
import virtualpetkmp.shared.generated.resources.pdf_fecha_nacimiento
import virtualpetkmp.shared.generated.resources.pdf_fecha_reporte
import virtualpetkmp.shared.generated.resources.pdf_ficha_de
import virtualpetkmp.shared.generated.resources.pdf_ficha_veterinaria
import virtualpetkmp.shared.generated.resources.pdf_fin
import virtualpetkmp.shared.generated.resources.pdf_frecuencia
import virtualpetkmp.shared.generated.resources.pdf_generado_el
import virtualpetkmp.shared.generated.resources.pdf_informes_notas
import virtualpetkmp.shared.generated.resources.pdf_inicio
import virtualpetkmp.shared.generated.resources.pdf_medicamento
import virtualpetkmp.shared.generated.resources.pdf_microchip
import virtualpetkmp.shared.generated.resources.pdf_motivo
import virtualpetkmp.shared.generated.resources.pdf_no_tratamientos_activos
import virtualpetkmp.shared.generated.resources.pdf_no_vacunas_proximas
import virtualpetkmp.shared.generated.resources.pdf_nombre
import virtualpetkmp.shared.generated.resources.pdf_nota
import virtualpetkmp.shared.generated.resources.pdf_peso_actual
import virtualpetkmp.shared.generated.resources.pdf_proxima
import virtualpetkmp.shared.generated.resources.pdf_proxima_dosis
import virtualpetkmp.shared.generated.resources.pdf_raza
import virtualpetkmp.shared.generated.resources.pdf_resumen_cuidador
import virtualpetkmp.shared.generated.resources.pdf_resumen_general
import virtualpetkmp.shared.generated.resources.pdf_revisiones
import virtualpetkmp.shared.generated.resources.pdf_sexo
import virtualpetkmp.shared.generated.resources.pdf_tipo
import virtualpetkmp.shared.generated.resources.pdf_tratamientos_activos
import virtualpetkmp.shared.generated.resources.pdf_ultimo_peso
import virtualpetkmp.shared.generated.resources.pdf_vacuna
import virtualpetkmp.shared.generated.resources.pdf_vacunas
import virtualpetkmp.shared.generated.resources.pdf_vacunas_proximas

/**
 * Genera el PDF de la ficha de una mascota.
 *
 * **Todos los rótulos salen de `composeResources`**, no escritos a mano: el PDF se abre en
 * otro dispositivo y tiene que estar en el mismo idioma que la pantalla desde la que se
 * exportó. Como esto corre en una corrutina (no hay contexto composable), los textos se leen
 * con `getString`, que es la variante `suspend` de `stringResource`.
 *
 * @param localizacion configuración regional activa. Decide el formato de fecha y la unidad
 *   de peso (kg o lb). Se pasa desde la interfaz porque aquí no se puede consultar el sistema
 *   de forma fiable en todas las plataformas.
 */
class ExportadorPdf {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun exportarFichaMascota(
        mascota: Mascota,
        vacunas: List<Vacuna>,
        revisiones: List<Revision>,
        tratamientos: List<Tratamiento>,
        pesos: List<Peso>,
        informes: List<Informe>,
        notas: List<Nota>,
        version: VersionPdf = VersionPdf.COMPLETA,
        localizacion: Localizacion = Localizacion.desdeRegion("es-ES")
    ): ByteArray {
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val edadMeses = calcularEdadMeses(mascota.fechaNacimiento, hoy)
        val edad = localizacion.formatearEdad(edadMeses.anios, edadMeses.meses)
        val ultimoPeso = pesos.maxByOrNull { it.fecha }?.peso
        val tratamientosActivos = tratamientos.filter { it.fechaFin == null || it.fechaFin > hoy }

        // ---- Rótulos, resueltos una sola vez y en el idioma activo ----
        val tFichaDe = getString(Res.string.pdf_ficha_de, mascota.nombre)
        val tResumenCuidador = getString(Res.string.pdf_resumen_cuidador)
        val tFichaVeterinaria = getString(Res.string.pdf_ficha_veterinaria)
        val tNombre = getString(Res.string.pdf_nombre)
        val tEspecie = getString(Res.string.pdf_especie)
        val tRaza = getString(Res.string.pdf_raza)
        val tSexo = getString(Res.string.pdf_sexo)
        val tEdad = getString(Res.string.pdf_edad)
        val tFechaNacimiento = getString(Res.string.pdf_fecha_nacimiento)
        val tMicrochip = getString(Res.string.pdf_microchip)
        val tPesoActual = getString(Res.string.pdf_peso_actual)
        val tVacunasProximas = getString(Res.string.pdf_vacunas_proximas)
        val tNoVacunasProximas = getString(Res.string.pdf_no_vacunas_proximas)
        val tVacuna = getString(Res.string.pdf_vacuna)
        val tProximaDosis = getString(Res.string.pdf_proxima_dosis)
        val tTratamientosActivos = getString(Res.string.pdf_tratamientos_activos)
        val tNoTratamientosActivos = getString(Res.string.pdf_no_tratamientos_activos)
        val tMedicamento = getString(Res.string.pdf_medicamento)
        val tFrecuencia = getString(Res.string.pdf_frecuencia)
        val tResumenGeneral = getString(Res.string.pdf_resumen_general)
        val tUltimoPeso = getString(Res.string.pdf_ultimo_peso)
        val tFechaReporte = getString(Res.string.pdf_fecha_reporte)
        val tATenerEnCuenta = getString(Res.string.pdf_a_tener_en_cuenta)
        val tNota = getString(Res.string.pdf_nota)
        val tVacunas = getString(Res.string.pdf_vacunas)
        val tAplicada = getString(Res.string.pdf_aplicada)
        val tProxima = getString(Res.string.pdf_proxima)
        val tRevisiones = getString(Res.string.pdf_revisiones)
        val tFecha = getString(Res.string.pdf_fecha)
        val tMotivo = getString(Res.string.pdf_motivo)
        val tDiagnostico = getString(Res.string.pdf_diagnostico)
        val tInicio = getString(Res.string.pdf_inicio)
        val tFin = getString(Res.string.pdf_fin)
        val tInformesNotas = getString(Res.string.pdf_informes_notas)
        val tTipo = getString(Res.string.pdf_tipo)
        val tDescripcion = getString(Res.string.pdf_descripcion)
        val tGeneradoEl = getString(Res.string.pdf_generado_el, localizacion.formatearFecha(hoy))

        fun classpathRead(name: String): ByteArray {
            return try {
                val loader = Thread.currentThread().contextClassLoader
                val stream = loader?.getResourceAsStream(name)
                stream?.use { it.readBytes() } ?: ByteArray(0)
            } catch (_: Exception) {
                ByteArray(0)
            }
        }

        var regularFontBytes = try {
            Res.readBytes("files/fonts/noto_sans_variable.ttf")
        } catch (_: Exception) {
            ByteArray(0)
        }
        var boldFontBytes = try {
            Res.readBytes("files/fonts/noto_sans_variable.ttf")
        } catch (_: Exception) {
            ByteArray(0)
        }

        if (regularFontBytes.isEmpty()) {
            regularFontBytes = classpathRead("files/fonts/noto_sans_variable.ttf")
        }
        if (boldFontBytes.isEmpty()) {
            boldFontBytes = classpathRead("files/fonts/noto_sans_variable.ttf")
        }

        val documento = pdfDocument(PageConfig(margin = 36.dp, pageNumbers = false, repeatHeader = false)) {
            header {
                row {
                    cell(1f) {
                        text(
                            text = tFichaDe,
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                divider()
            }

            text(
                text = if (version == VersionPdf.RESUMIDA) tResumenCuidador else tFichaVeterinaria,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            spacer(8.dp)

            box(
                padding = 12.dp,
                border = 1.dp,
                borderColor = Color(0xFFDDDDDD),
                background = Color(0xFFF5F7FA)
            ) {
                // Especie, raza y sexo se guardan en español en la base de datos: se traducen
                // aquí igual que en la interfaz, para que el PDF coincida con la pantalla.
                keyValue(tNombre, mascota.nombre)
                keyValue(tEspecie, TraduccionesDatos.etiqueta(mascota.especie, localizacion.idioma))
                keyValue(tRaza, TraduccionesDatos.etiqueta(mascota.raza, localizacion.idioma))
                keyValue(tSexo, TraduccionesDatos.etiqueta(mascota.sexo, localizacion.idioma))
                keyValue(tEdad, edad)
                keyValue(tFechaNacimiento, localizacion.formatearFecha(mascota.fechaNacimiento))
                if (mascota.microchip != null) {
                    keyValue(tMicrochip, mascota.microchip)
                }
                if (ultimoPeso != null) {
                    keyValue(tPesoActual, localizacion.formatearPeso(ultimoPeso, conUnidad = true))
                }
            }

            spacer(12.dp)

            if (version == VersionPdf.RESUMIDA) {
                text(tVacunasProximas, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                val proximasVacunas = vacunas.filter { it.fechaProximaDosis > hoy }
                    .sortedBy { it.fechaProximaDosis }
                    .take(3)
                if (proximasVacunas.isEmpty()) {
                    text(tNoVacunasProximas)
                } else {
                    table(
                        columns = listOf(
                            PdfColumn(2f, tVacuna),
                            PdfColumn(1f, tProximaDosis, TextAlign.End)
                        )
                    ) {
                        proximasVacunas.forEach { vacuna ->
                            row(vacuna.nombre, localizacion.formatearFecha(vacuna.fechaProximaDosis))
                        }
                    }
                }
                spacer(12.dp)

                text(tTratamientosActivos, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (tratamientosActivos.isEmpty()) {
                    text(tNoTratamientosActivos)
                } else {
                    table(
                        columns = listOf(
                            PdfColumn(2f, tMedicamento),
                            PdfColumn(1f, tFrecuencia, TextAlign.End)
                        )
                    ) {
                        tratamientosActivos.sortedBy { it.fechaInicio }.forEach { tratamiento ->
                            row(
                                tratamiento.nombreMedicamento,
                                tratamiento.frecuencia ?: "-"
                            )
                        }
                    }
                }
                spacer(12.dp)

                text(tResumenGeneral, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (ultimoPeso != null) {
                    keyValue(tUltimoPeso, localizacion.formatearPeso(ultimoPeso, conUnidad = true))
                }
                keyValue(tFechaReporte, localizacion.formatearFecha(hoy))

                if (notas.isNotEmpty()) {
                    spacer(12.dp)
                    text(tATenerEnCuenta, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(PdfColumn(3f, tNota))
                    ) {
                        notas.forEach { nota ->
                            row(nota.texto)
                        }
                    }
                }
            } else {
                if (vacunas.isNotEmpty()) {
                    text(tVacunas, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(2f, tVacuna),
                            PdfColumn(1f, tAplicada, TextAlign.End),
                            PdfColumn(1f, tProxima, TextAlign.End)
                        )
                    ) {
                        vacunas.sortedBy { it.fechaAplicacion }.forEach { vacuna ->
                            row(
                                vacuna.nombre,
                                localizacion.formatearFecha(vacuna.fechaAplicacion),
                                localizacion.formatearFecha(vacuna.fechaProximaDosis)
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (revisiones.isNotEmpty()) {
                    text(tRevisiones, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(1f, tFecha),
                            PdfColumn(2f, tMotivo),
                            PdfColumn(2f, tDiagnostico)
                        )
                    ) {
                        revisiones.sortedByDescending { it.fecha }.forEach { revision ->
                            row(
                                localizacion.formatearFecha(revision.fecha),
                                revision.motivo,
                                revision.diagnostico ?: "-"
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (tratamientosActivos.isNotEmpty()) {
                    text(tTratamientosActivos, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(2f, tMedicamento),
                            PdfColumn(1f, tInicio, TextAlign.End),
                            PdfColumn(1f, tFin, TextAlign.End),
                            PdfColumn(1f, tFrecuencia, TextAlign.End)
                        )
                    ) {
                        tratamientosActivos.sortedBy { it.fechaInicio }.forEach { tratamiento ->
                            row(
                                tratamiento.nombreMedicamento,
                                localizacion.formatearFecha(tratamiento.fechaInicio),
                                tratamiento.fechaFin?.let { localizacion.formatearFecha(it) } ?: "-",
                                tratamiento.frecuencia ?: "-"
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (informes.isNotEmpty() || notas.isNotEmpty()) {
                    text(tInformesNotas, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    if (informes.isNotEmpty()) {
                        table(
                            columns = listOf(
                                PdfColumn(1f, tFecha),
                                PdfColumn(2f, tTipo),
                                PdfColumn(2f, tDescripcion)
                            )
                        ) {
                            informes.sortedByDescending { it.fecha }.forEach { informe ->
                                row(
                                    localizacion.formatearFecha(informe.fecha),
                                    informe.tipo,
                                    informe.descripcion ?: "-"
                                )
                            }
                        }
                    }
                    if (notas.isNotEmpty()) {
                        table(
                            columns = listOf(
                                PdfColumn(3f, tNota)
                            )
                        ) {
                            notas.forEach { nota ->
                                row(nota.texto)
                            }
                        }
                    }
                    spacer(12.dp)
                }
            }

            text(tGeneradoEl, TextStyle(fontSize = 8.sp, color = Color(0xFF6C757D)))
        }

        // El tamaño de las fuentes se registra a propósito: si algún día el PDF sale en blanco
        // en Android, este log dice si el problema es que no se encontró la fuente.
        val pdf = try {
            val out = documento.render(regularFontBytes, boldFontBytes)
            println("[ExportadorPdf] regularFontBytes=${regularFontBytes.size}, boldFontBytes=${boldFontBytes.size}, pdfSize=${out.size}")
            out
        } catch (e: Exception) {
            println("[ExportadorPdf] render failed: ${e.message}")
            ByteArray(0)
        }

        return pdf
    }
}

enum class VersionPdf {
    RESUMIDA,
    COMPLETA
}
