package com.example.virtualpetkmp.util

/**
 * Margen de seguridad del eje vertical: un **10 % del rango de los datos**, por encima del valor
 * más alto y por debajo del más bajo.
 *
 * Es relativo y no una cantidad fija a propósito. Un margen fijo de medio kilo arreglaba el caso
 * de un perro (26 kg) pero destrozaba el de un conejo: con pesajes de 1,60 a 1,70 el eje se iba a
 * 1,10–2,20 y la línea quedaba aplastada en el centro, "plana". Con el 10 %, 1,60–1,70 da
 * 1,59–1,71, y la línea ocupa el 83 % del alto esté la mascota en kilos o en libras.
 */
const val MARGEN_RELATIVO_ESCALA = 0.10

/**
 * Margen que se usa cuando **todos los pesajes valen lo mismo**, es decir cuando el rango de los
 * datos es cero y el 10 % de cero no daría eje ninguno. En ese caso la línea es una recta
 * horizontal centrada, que es lo que de verdad dicen los datos.
 */
const val MARGEN_ESCALA_PLANO_KILOS = 0.5

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
     * Con el margen del 10 %, el pesaje más bajo cae en 0,083 y el más alto en 0,917: nunca en
     * los extremos.
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
 * El rango **incluye el peso ideal además de los datos**: si el ideal es 1,60 y los pesajes van
 * de 1,50 a 1,80, la referencia se dibuja exactamente a la altura de 1,60 y el eje sigue
 * cubriendo los 1,80. Si el ideal queda fuera del rango de los datos, el eje se estira para
 * meterlo, porque una referencia que no se ve no sirve de nada.
 *
 * Los dos extremos llevan un margen de seguridad del [margenRelativo] (10 % por defecto) para
 * que ningún trazo toque el borde de arriba ni el de abajo.
 *
 * @param pesosKilos pesajes **en kilos**, como están guardados.
 * @param pesoIdealKilos ideal **en kilos**, o `null` si el usuario no lo ha configurado.
 * @param unidad unidad en la que se va a dibujar.
 */
fun escalaDePeso(
    pesosKilos: List<Double>,
    pesoIdealKilos: Double?,
    unidad: UnidadPeso,
    margenRelativo: Double = MARGEN_RELATIVO_ESCALA
): EscalaPeso {
    val valores = pesosKilos.map { UnidadPeso.desdeKilos(it, unidad) } +
        listOfNotNull(pesoIdealKilos?.let { UnidadPeso.desdeKilos(it, unidad) })

    if (valores.isEmpty()) return EscalaPeso(0.0, 0.0, unidad)

    val minimo = valores.min()
    val maximo = valores.max()
    val rangoDatos = maximo - minimo

    // Con todos los pesos iguales (o con un solo pesaje) el 10 % de cero no da margen, así que
    // se usa una cantidad fija: la línea sale centrada en lugar de pegada al borde.
    val margen = if (rangoDatos <= 0.0) {
        UnidadPeso.desdeKilos(MARGEN_ESCALA_PLANO_KILOS, unidad)
    } else {
        rangoDatos * margenRelativo
    }

    return EscalaPeso(
        min = minimo - margen,
        max = maximo + margen,
        unidad = unidad
    )
}
