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
import virtualpetkmp.shared.generated.resources.Res

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
        version: VersionPdf = VersionPdf.COMPLETA
    ): ByteArray {
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val edad = calcularEdad(mascota.fechaNacimiento, hoy)
        val ultimoPeso = pesos.maxByOrNull { it.fecha }?.peso
        val tratamientosActivos = tratamientos.filter { it.fechaFin == null || it.fechaFin > hoy }
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
                            text = "Ficha de ${mascota.nombre}",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                divider()
            }

            text(
                text = if (version == VersionPdf.RESUMIDA) "Resumen para cuidador" else "Ficha veterinaria",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            spacer(8.dp)

            box(
                padding = 12.dp,
                border = 1.dp,
                borderColor = Color(0xFFDDDDDD),
                background = Color(0xFFF5F7FA)
            ) {
                keyValue("Nombre", mascota.nombre)
                keyValue("Especie", mascota.especie)
                keyValue("Raza", mascota.raza)
                keyValue("Sexo", mascota.sexo)
                keyValue("Edad", edad)
                keyValue("Fecha nacimiento", mascota.fechaNacimiento.toFormatoEuropeo())
                if (mascota.microchip != null) {
                    keyValue("Microchip", mascota.microchip)
                }
                if (ultimoPeso != null) {
                    keyValue("Peso actual", "${ultimoPeso} kg")
                }
            }

            spacer(12.dp)

            if (version == VersionPdf.RESUMIDA) {
                text("Vacunas próximas", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                val proximasVacunas = vacunas.filter { it.fechaProximaDosis > hoy }
                    .sortedBy { it.fechaProximaDosis }
                    .take(3)
                if (proximasVacunas.isEmpty()) {
                    text("No hay vacunas próximas programadas")
                } else {
                    table(
                        columns = listOf(
                            PdfColumn(2f, "Vacuna"),
                            PdfColumn(1f, "Próxima dosis", TextAlign.End)
                        )
                    ) {
                        proximasVacunas.forEach { vacuna ->
                            row(vacuna.nombre, vacuna.fechaProximaDosis.toFormatoEuropeo())
                        }
                    }
                }
                spacer(12.dp)

                text("Tratamientos activos", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (tratamientosActivos.isEmpty()) {
                    text("No hay tratamientos activos")
                } else {
                    table(
                        columns = listOf(
                            PdfColumn(2f, "Medicamento"),
                            PdfColumn(1f, "Frecuencia", TextAlign.End)
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

                text("Resumen general", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (ultimoPeso != null) {
                    keyValue("Último peso", "${ultimoPeso} kg")
                }
                keyValue("Fecha del reporte", hoy.toFormatoEuropeo())

                if (notas.isNotEmpty()) {
                    spacer(12.dp)
                    text("A tener en cuenta", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(PdfColumn(3f, "Nota"))
                    ) {
                        notas.forEach { nota ->
                            row(nota.texto)
                        }
                    }
                }
            } else {
                if (vacunas.isNotEmpty()) {
                    text("Vacunas", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(2f, "Vacuna"),
                            PdfColumn(1f, "Aplicada", TextAlign.End),
                            PdfColumn(1f, "Próxima", TextAlign.End)
                        )
                    ) {
                        vacunas.sortedBy { it.fechaAplicacion }.forEach { vacuna ->
                            row(
                                vacuna.nombre,
                                vacuna.fechaAplicacion.toFormatoEuropeo(),
                                vacuna.fechaProximaDosis.toFormatoEuropeo()
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (revisiones.isNotEmpty()) {
                    text("Revisiones veterinarias", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(1f, "Fecha"),
                            PdfColumn(2f, "Motivo"),
                            PdfColumn(2f, "Diagnóstico")
                        )
                    ) {
                        revisiones.sortedByDescending { it.fecha }.forEach { revision ->
                            row(
                                revision.fecha.toFormatoEuropeo(),
                                revision.motivo,
                                revision.diagnostico ?: "-"
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (tratamientosActivos.isNotEmpty()) {
                    text("Tratamientos activos", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    table(
                        columns = listOf(
                            PdfColumn(2f, "Medicamento"),
                            PdfColumn(1f, "Inicio", TextAlign.End),
                            PdfColumn(1f, "Fin", TextAlign.End),
                            PdfColumn(1f, "Frecuencia", TextAlign.End)
                        )
                    ) {
                        tratamientosActivos.sortedBy { it.fechaInicio }.forEach { tratamiento ->
                            row(
                                tratamiento.nombreMedicamento,
                                tratamiento.fechaInicio.toFormatoEuropeo(),
                                tratamiento.fechaFin?.toFormatoEuropeo() ?: "-",
                                tratamiento.frecuencia ?: "-"
                            )
                        }
                    }
                    spacer(12.dp)
                }

                if (informes.isNotEmpty() || notas.isNotEmpty()) {
                    text("Informes y notas", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    if (informes.isNotEmpty()) {
                        table(
                            columns = listOf(
                                PdfColumn(1f, "Fecha"),
                                PdfColumn(2f, "Tipo"),
                                PdfColumn(2f, "Descripción")
                            )
                        ) {
                            informes.sortedByDescending { it.fecha }.forEach { informe ->
                                row(
                                    informe.fecha.toFormatoEuropeo(),
                                    informe.tipo,
                                    informe.descripcion ?: "-"
                                )
                            }
                        }
                    }
                    if (notas.isNotEmpty()) {
                        table(
                            columns = listOf(
                                PdfColumn(3f, "Nota")
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

            text("Generado el ${hoy.toFormatoEuropeo()}", TextStyle(fontSize = 8.sp, color = Color(0xFF6C757D)))
        }
        
        // Render and log sizes to help Android debugging
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
