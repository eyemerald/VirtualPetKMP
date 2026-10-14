package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.StringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.peso_filtro_anio
import virtualpetkmp.shared.generated.resources.peso_filtro_fechas
import virtualpetkmp.shared.generated.resources.peso_filtro_mes
import virtualpetkmp.shared.generated.resources.peso_filtro_reciente

/**
 * Cuántos meses hacia atrás alcanza el filtro "6 meses".
 *
 * Es el filtro que viene seleccionado al abrir la hoja: con él no hay que cargar el historial
 * entero de golpe y se ve lo que de verdad importa, que es la evolución reciente.
 */
const val MESES_FILTRO_RECIENTE = 6

/**
 * Qué pesajes se están mostrando en el gráfico de la hoja de peso.
 *
 * Guarda el recurso de su etiqueta y no el texto: así el chip se traduce solo al pintarlo.
 * **El orden de las entradas es el orden de los chips** en pantalla.
 *
 * Vive en `util` y no en la pantalla porque lo que decide —qué pesajes entran en cada rango—
 * es lógica, no pintura, y así se puede probar sin montar la interfaz.
 */
enum class FiltroPeso(val etiqueta: StringResource) {
    /** Los últimos [MESES_FILTRO_RECIENTE] meses. Es el que viene puesto por defecto. */
    RECIENTE(Res.string.peso_filtro_reciente),

    /** El año elegido con los chips de año. */
    ANIO(Res.string.peso_filtro_anio),

    /** El mes natural en curso. */
    MES(Res.string.peso_filtro_mes),

    /** Un rango de fechas escrito a mano. */
    FECHAS(Res.string.peso_filtro_fechas)
}

/**
 * Aplica el filtro elegido al historial de pesajes.
 *
 * Devuelve siempre la lista **ordenada de más antiguo a más reciente**, que es como la espera el
 * gráfico para dibujar la línea de izquierda a derecha.
 *
 * Detalles que no son evidentes y por eso están aquí y con test:
 * - **"6 meses" cuenta desde hoy**, no desde el último pesaje: si la mascota lleva tres meses sin
 *   pesarse, el rango sigue siendo el mismo y el gráfico sale vacío, que es la verdad.
 * - **"Mes actual" compara año y mes**, no solo el mes: si no, octubre de este año se mezclaría
 *   con octubre del anterior.
 * - **Un rango de fechas abierto por un lado** (solo desde, o solo hasta) se respeta: el extremo
 *   que no se ha rellenado simplemente no recorta.
 * - **Un año sin datos** devuelve la lista vacía, no el historial entero: el gráfico enseña su
 *   mensaje de "no hay pesajes en este rango", que es lo que el usuario espera ver.
 *
 * @param hoy fecha de hoy, para "6 meses" y "mes actual". Se recibe en vez de leer el reloj para
 *   que la función sea pura y el test pueda fijar el día.
 * @param anio año seleccionado, solo para [FiltroPeso.ANIO].
 * @param desde límite inferior del rango de fechas, o `null` si no se ha rellenado.
 * @param hasta límite superior del rango de fechas, o `null` si no se ha rellenado.
 */
fun filtrarPesos(
    pesos: List<Peso>,
    filtro: FiltroPeso,
    hoy: LocalDate,
    anio: Int? = null,
    desde: LocalDate? = null,
    hasta: LocalDate? = null
): List<Peso> {
    val ordenados = pesos.sortedBy { it.fecha }
    return when (filtro) {
        FiltroPeso.RECIENTE -> {
            val limite = hoy.minus(DatePeriod(months = MESES_FILTRO_RECIENTE))
            ordenados.filter { it.fecha >= limite }
        }

        FiltroPeso.ANIO -> ordenados.filter { it.fecha.year == anio }
        FiltroPeso.MES -> ordenados.filter {
            it.fecha.year == hoy.year && it.fecha.monthNumber == hoy.monthNumber
        }

        FiltroPeso.FECHAS -> ordenados.filter { registro ->
            (desde == null || registro.fecha >= desde) &&
                (hasta == null || registro.fecha <= hasta)
        }
    }
}

/** Qué se escribe bajo el eje horizontal del gráfico. */
enum class RotuloEjePeso {
    /** Abreviatura del mes, con el año cuando cambia. */
    MES,

    /** Número del día. */
    DIA
}

/** Cómo se dibuja el gráfico con el filtro activo: cada cuánto se pone un punto y qué se rotula. */
data class PresentacionPeso(val granularidad: GranularidadPeso, val rotulo: RotuloEjePeso)

/**
 * Decide cómo se dibuja el gráfico según el filtro, en vez de deducirlo de los datos.
 *
 * Antes se miraba si todos los pesajes caían en el mismo mes, y eso hacía que "6 meses" agrupara
 * por meses y perdiera el detalle; el usuario quiere ver **cada pesaje** en la evolución reciente
 * y las medias solo cuando está mirando un año entero, que es cuando el detalle sería un amasijo.
 *
 * | Filtro | Puntos | Eje |
 * | --- | --- | --- |
 * | 6 meses | uno por pesaje | meses |
 * | Año | uno por mes (media) | meses |
 * | Mes actual | uno por pesaje | días |
 * | Elegir fechas | uno por pesaje | días si cabe en un mes, meses si no |
 */
fun presentacionDe(filtro: FiltroPeso, pesos: List<Peso>): PresentacionPeso = when (filtro) {
    FiltroPeso.RECIENTE -> PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.MES)
    FiltroPeso.ANIO -> PresentacionPeso(GranularidadPeso.MENSUAL, RotuloEjePeso.MES)
    FiltroPeso.MES -> PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.DIA)
    FiltroPeso.FECHAS -> PresentacionPeso(
        granularidad = GranularidadPeso.POR_PESAJES,
        rotulo = if (abarcaMasDeUnMes(pesos)) RotuloEjePeso.MES else RotuloEjePeso.DIA
    )
}

/** Si el rango que se está mirando cruza de un mes a otro. */
private fun abarcaMasDeUnMes(pesos: List<Peso>): Boolean {
    val primera = pesos.minByOrNull { it.fecha }?.fecha ?: return false
    val ultima = pesos.maxByOrNull { it.fecha }?.fecha ?: return false
    return primera.year != ultima.year || primera.monthNumber != ultima.monthNumber
}
