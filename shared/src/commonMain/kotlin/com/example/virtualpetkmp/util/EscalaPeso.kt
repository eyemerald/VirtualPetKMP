package com.example.virtualpetkmp.util

/**
 * Margen que se deja por encima del peso más alto y por debajo del más bajo del eje vertical.
 *
 * Va **en kilos** y se convierte a la unidad que ve el usuario antes de aplicarlo, porque el
 * kilo es la unidad en la que se guarda todo: así el aire del gráfico es el mismo para un
 * usuario que ve kilos y para uno que ve libras.
 *
 * Sirve para dos cosas: que la línea no toque nunca los bordes del gráfico, y que la referencia
 * del peso ideal se lea aunque coincida con el máximo o el mínimo de los datos.
 */
const val MARGEN_ESCALA_KILOS = 0.5

/**
 * Rango vertical de un gráfico de peso.
 *
 * [min] y [max] están **en la unidad que ve el usuario** (kg o lb), porque son los extremos del
 * eje y se usan para rotular. En cambio [normalizarKilos] recibe el peso **en kilos**, que es
 * como se guarda siempre: la conversión la hace la propia escala.
 *
 * Ese reparto es a propósito. Si `normalizar` aceptara valores en la unidad mostrada, sería
 * facilísimo pasarle un peso en kilos y colocar el punto en cualquier sitio sin que nada
 * avisara. Con la conversión dentro, fuera de aquí todo son kilos.
 */
data class EscalaPeso(
    val min: Double,
    val max: Double,
    val unidad: UnidadPeso
) {
    /** Longitud del rango. Puede ser 0 si no hay ni datos ni ideal. */
    val rango: Double get() = max - min

    /**
     * Posición de un peso **en kilos** dentro del rango, de 0 (abajo) a 1 (arriba).
     *
     * Si no hay rango (no hay nada que dibujar), se devuelve 0.5 para que un punto suelto quede
     * centrado en lugar de pegado a un borde.
     */
    fun normalizarKilos(kilos: Double): Float {
        if (rango <= 0.0) return 0.5f
        val valor = UnidadPeso.desdeKilos(kilos, unidad)
        return ((valor - min) / rango).toFloat()
    }
}

/**
 * Calcula el eje vertical de un gráfico de peso.
 *
 * El rango **incluye el peso ideal además de los datos**, y es la corrección del fallo que hacía
 * que la línea del ideal apareciera pegada al borde (o directamente fuera del gráfico) cuando el
 * ideal quedaba por encima del pesaje más alto o por debajo del más bajo: si el ideal es 1,60 y
 * los pesajes van de 1,50 a 1,80, la referencia se dibuja exactamente a la altura de 1,60 y el
 * eje sigue cubriendo los 1,80.
 *
 * Los dos extremos llevan [margenKilos] de aire, así que ningún trazo toca el borde.
 *
 * @param pesosKilos pesajes **en kilos**, como están guardados.
 * @param pesoIdealKilos ideal **en kilos**, o `null` si el usuario no lo ha configurado.
 * @param unidad unidad en la que se va a dibujar.
 */
fun escalaDePeso(
    pesosKilos: List<Double>,
    pesoIdealKilos: Double?,
    unidad: UnidadPeso,
    margenKilos: Double = MARGEN_ESCALA_KILOS
): EscalaPeso {
    val valores = pesosKilos.map { UnidadPeso.desdeKilos(it, unidad) } +
        listOfNotNull(pesoIdealKilos?.let { UnidadPeso.desdeKilos(it, unidad) })

    if (valores.isEmpty()) return EscalaPeso(0.0, 0.0, unidad)

    val margen = UnidadPeso.desdeKilos(margenKilos, unidad)
    return EscalaPeso(
        min = valores.min() - margen,
        max = valores.max() + margen,
        unidad = unidad
    )
}
