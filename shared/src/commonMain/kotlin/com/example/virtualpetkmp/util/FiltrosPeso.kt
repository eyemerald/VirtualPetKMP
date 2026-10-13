package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.peso_filtro_anio
import virtualpetkmp.shared.generated.resources.peso_filtro_fechas
import virtualpetkmp.shared.generated.resources.peso_filtro_mes
import virtualpetkmp.shared.generated.resources.peso_filtro_todo

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
    /** Todo el historial. */
    TODO(Res.string.peso_filtro_todo),

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
 * - **"Mes actual" compara año y mes**, no solo el mes: si no, octubre de este año se mezclaría
 *   con octubre del anterior.
 * - **Un rango de fechas abierto por un lado** (solo desde, o solo hasta) se respeta: el extremo
 *   que no se ha rellenado simplemente no recorta.
 * - **Un año sin datos** devuelve la lista vacía, no el historial entero: el gráfico enseña su
 *   mensaje de "no hay pesajes en este rango", que es lo que el usuario espera ver.
 *
 * @param hoy fecha de hoy, para el filtro de mes actual. Se recibe en vez de leer el reloj para
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
        FiltroPeso.TODO -> ordenados
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
