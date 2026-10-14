package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.Vacuna
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.pdf_color
import virtualpetkmp.shared.generated.resources.pdf_edad
import virtualpetkmp.shared.generated.resources.pdf_especie
import virtualpetkmp.shared.generated.resources.pdf_fecha_nacimiento
import virtualpetkmp.shared.generated.resources.pdf_maximo
import virtualpetkmp.shared.generated.resources.pdf_microchip
import virtualpetkmp.shared.generated.resources.pdf_minimo
import virtualpetkmp.shared.generated.resources.pdf_nombre
import virtualpetkmp.shared.generated.resources.pdf_peso_actual
import virtualpetkmp.shared.generated.resources.pdf_peso_ideal
import virtualpetkmp.shared.generated.resources.pdf_primero
import virtualpetkmp.shared.generated.resources.pdf_raza
import virtualpetkmp.shared.generated.resources.pdf_sexo
import virtualpetkmp.shared.generated.resources.pdf_ultimo

/**
 * Lo que se escribe en el PDF, ya decidido y en texto: qué datos van, en qué orden y con qué
 * formato. **No hay nada de dibujo aquí** a propósito.
 *
 * El motor del PDF (`ExportadorPdf`) se limita a pintar estas listas. Así lo que puede estar mal
 * de verdad —que falte la dosis de un tratamiento, que se mezclen los activos con el historial,
 * que las próximas dosis salgan desordenadas— se comprueba con tests, sin abrir un PDF.
 */

/** Texto que se escribe cuando un dato opcional no está relleno. */
const val SIN_DATO_PDF = "—"

/** Un dato de la ficha: su etiqueta (traducible) y su valor ya formateado. */
data class DatoPdf(val etiqueta: StringResource, val valor: String)

/** Una dosis por venir: la fecha y qué toca. */
data class DosisProxima(val fecha: LocalDate, val descripcion: String)

/**
 * Un tratamiento **está activo** si hoy cae dentro de su rango de fechas, o si no tiene fecha de
 * fin (crónico).
 *
 * Vive aquí y no en cada sitio que lo necesita (la tarjeta de la ficha, el resumen del PDF)
 * porque si una regla se duplica acaban discrepando: la pantalla diría "1 activo" y el PDF
 * listaría otro.
 */
fun Tratamiento.estaActivo(hoy: LocalDate): Boolean =
    fechaInicio <= hoy && (fechaFin == null || fechaFin >= hoy)

/** Los tratamientos en curso, del más reciente al más antiguo. */
fun tratamientosActivosPdf(tratamientos: List<Tratamiento>, hoy: LocalDate): List<Tratamiento> =
    tratamientos.filter { it.estaActivo(hoy) }.sortedByDescending { it.fechaInicio }

/** Los ya terminados, del más reciente al más antiguo. */
fun tratamientosTerminadosPdf(tratamientos: List<Tratamiento>, hoy: LocalDate): List<Tratamiento> =
    tratamientos.filterNot { it.estaActivo(hoy) }.sortedByDescending { it.fechaInicio }

/**
 * Datos de identificación de la mascota.
 *
 * @param edad ya calculada y en texto ("2 años y 6 meses"): el motor del PDF corre en una
 *   corrutina y no puede leer recursos.
 * @param ultimoPeso el pesaje más reciente, o `null` si no hay ninguno.
 */
fun datosDeLaMascotaPdf(
    mascota: Mascota,
    edad: String,
    ultimoPeso: Double?,
    formato: Localizacion
): List<DatoPdf> = buildList {
    add(DatoPdf(Res.string.pdf_nombre, mascota.nombre))
    add(DatoPdf(Res.string.pdf_especie, TraduccionesDatos.etiqueta(mascota.especie, formato.idioma)))
    add(DatoPdf(Res.string.pdf_raza, TraduccionesDatos.etiqueta(mascota.raza, formato.idioma)))
    add(DatoPdf(Res.string.pdf_sexo, TraduccionesDatos.etiqueta(mascota.sexo, formato.idioma)))
    add(DatoPdf(Res.string.pdf_edad, edad))
    add(DatoPdf(Res.string.pdf_fecha_nacimiento, formato.formatearFecha(mascota.fechaNacimiento)))
    if (mascota.color.isNotBlank()) {
        add(DatoPdf(Res.string.pdf_color, mascota.color))
    }
    if (!mascota.microchip.isNullOrBlank()) {
        add(DatoPdf(Res.string.pdf_microchip, mascota.microchip!!))
    }
    if (ultimoPeso != null) {
        add(DatoPdf(Res.string.pdf_peso_actual, formato.formatearPeso(ultimoPeso, conUnidad = true)))
    }
    if (mascota.pesoIdeal != null) {
        add(DatoPdf(Res.string.pdf_peso_ideal, formato.formatearPeso(mascota.pesoIdeal, conUnidad = true)))
    }
}

/** Resumen del peso: el primero, el último, el mínimo, el máximo y el ideal si está fijado. */
fun resumenDePesoPdf(
    pesos: List<Peso>,
    pesoIdeal: Double?,
    formato: Localizacion
): List<DatoPdf> {
    if (pesos.isEmpty()) return emptyList()

    val ordenados = pesos.sortedBy { it.fecha }
    val primero = ordenados.first()
    val ultimo = ordenados.last()
    val minimo = ordenados.minBy { it.peso }
    val maximo = ordenados.maxBy { it.peso }

    fun valor(peso: Peso) =
        "${formato.formatearPeso(peso.peso, conUnidad = true)} · ${formato.formatearFecha(peso.fecha)}"

    return buildList {
        add(DatoPdf(Res.string.pdf_primero, valor(primero)))
        add(DatoPdf(Res.string.pdf_ultimo, valor(ultimo)))
        // El mínimo y el máximo solo aportan algo si no coinciden con los extremos del rango.
        if (minimo.fecha != primero.fecha && minimo.fecha != ultimo.fecha) {
            add(DatoPdf(Res.string.pdf_minimo, valor(minimo)))
        }
        if (maximo.fecha != primero.fecha && maximo.fecha != ultimo.fecha) {
            add(DatoPdf(Res.string.pdf_maximo, valor(maximo)))
        }
        if (pesoIdeal != null) {
            add(DatoPdf(Res.string.pdf_peso_ideal, formato.formatearPeso(pesoIdeal, conUnidad = true)))
        }
    }
}

/**
 * Fila de un tratamiento: **medicamento, dosis, frecuencia, inicio y fin**.
 *
 * La dosis va la segunda a propósito y no es opcional en el diseño: sin ella el veterinario (o
 * quien cuide a la mascota) sabe qué medicamento y cada cuánto, pero no **cuánto** dar, que es
 * justo el dato que se consulta.
 */
fun filaTratamientoPdf(tratamiento: Tratamiento, formato: Localizacion): List<String> = listOf(
    tratamiento.nombreMedicamento,
    tratamiento.dosis?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    tratamiento.frecuencia?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    formato.formatearFecha(tratamiento.fechaInicio),
    tratamiento.fechaFin?.let { formato.formatearFecha(it) } ?: SIN_DATO_PDF
)

/** Fila de una vacuna: nombre, aplicada, próxima, veterinario y lote. */
fun filaVacunaPdf(vacuna: Vacuna, formato: Localizacion): List<String> = listOf(
    vacuna.nombre,
    formato.formatearFecha(vacuna.fechaAplicacion),
    formato.formatearFecha(vacuna.fechaProximaDosis),
    vacuna.veterinario?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    vacuna.lote?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF
)

/** Fila de un preventivo: producto, aplicado, próxima, veterinario y lote. */
fun filaPreventivoPdf(preventivo: Preventivo, formato: Localizacion): List<String> = listOf(
    preventivo.nombre,
    formato.formatearFecha(preventivo.fechaAplicacion),
    formato.formatearFecha(preventivo.fechaProximaDosis),
    preventivo.veterinario?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    preventivo.lote?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF
)

/**
 * Fila de una revisión: fecha, motivo, diagnóstico, notas y veterinario.
 *
 * Las notas van incluidas porque hasta ahora se perdían en el PDF, y son justo lo que el
 * veterinario apunta para la próxima visita.
 */
fun filaRevisionPdf(revision: Revision, formato: Localizacion): List<String> = listOf(
    formato.formatearFecha(revision.fecha),
    revision.motivo,
    revision.diagnostico?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    revision.notas?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    revision.veterinario?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF
)

/** Fila de un informe: fecha, tipo, descripción y archivo adjunto. */
fun filaInformePdf(informe: Informe, formato: Localizacion): List<String> = listOf(
    formato.formatearFecha(informe.fecha),
    informe.tipo,
    informe.descripcion?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF,
    informe.nombreArchivo
)

/** Fila de un pesaje: fecha, peso y notas. */
fun filaPesoPdf(peso: Peso, formato: Localizacion): List<String> = listOf(
    formato.formatearFecha(peso.fecha),
    formato.formatearPeso(peso.peso, conUnidad = true),
    peso.notas?.takeIf { it.isNotBlank() } ?: SIN_DATO_PDF
)

/**
 * Las dosis que vienen: vacunas y preventivos cuya próxima dosis es hoy o más adelante, mezclados
 * y ordenados por fecha.
 *
 * Van juntos a propósito. Para quien se queda cuidando a la mascota da igual si lo que toca es
 * una vacuna o una pipeta: lo que necesita es una lista de "qué hay que hacer y cuándo", en orden.
 */
fun proximasDosisPdf(
    vacunas: List<Vacuna>,
    preventivos: List<Preventivo>,
    hoy: LocalDate
): List<DosisProxima> =
    (vacunas.map { DosisProxima(it.fechaProximaDosis, it.nombre) } +
        preventivos.map { DosisProxima(it.fechaProximaDosis, it.nombre) })
        .filter { it.fecha >= hoy }
        .sortedBy { it.fecha }
