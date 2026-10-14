package com.example.virtualpetkmp.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Márgenes internos del **área de dibujo** de un gráfico de peso.
 *
 * Son del lienzo, no del contenedor de la pantalla: la tarjeta o la hoja siguen teniendo su
 * propio `padding`, y esto se suma por dentro para que la línea "flote" en el recuadro sin
 * tocar ninguno de los cuatro bordes.
 *
 * Comparten sitio los dos gráficos de peso (el de la tarjeta de la ficha y el de la hoja) para
 * que se vean igual: si uno llevara otros márgenes, parecerían dos gráficos distintos.
 */
data class MargenesGrafico(
    val izquierda: Dp = 16.dp,
    val derecha: Dp = 16.dp,
    val arriba: Dp = 10.dp,
    val abajo: Dp = 10.dp
)

/** Márgenes por defecto de un gráfico de peso. */
val MARGENES_GRAFICO_PESO = MargenesGrafico()

/**
 * Separación mínima entre dos puntos del gráfico.
 *
 * Es lo que decide si hace falta arrastrar en horizontal: si la serie no cabe con este aire, el
 * lienzo se hace más ancho que la pantalla y se puede mover. Con 14 dp, una serie de seis meses
 * con un pesaje por semana entra sin desplazamiento; con un pesaje al día se arrastra, que es lo
 * razonable porque ahí sí hay detalle que ver.
 */
val SEPARACION_MINIMA_PUNTO = 14.dp

/**
 * Ancho del lienzo del gráfico: lo que necesita la serie para no apelotonarse, y **nunca menos
 * que la pantalla**.
 *
 * Que sea un `maxOf` es lo que hace que el comportamiento sea el de siempre cuando hay pocos
 * datos (el gráfico se ajusta al ancho y no se arrastra nada) y que el desplazamiento aparezca
 * solo cuando de verdad hace falta. Sin el `maxOf`, con dos pesajes el lienzo mediría 14 dp.
 */
fun anchoDelGrafico(
    anchoDisponible: Dp,
    puntos: Int,
    separacionMinima: Dp = SEPARACION_MINIMA_PUNTO,
    margenes: MargenesGrafico = MARGENES_GRAFICO_PESO
): Dp {
    val necesario = separacionMinima * (puntos - 1).coerceAtLeast(0) +
        margenes.izquierda + margenes.derecha
    return maxOf(anchoDisponible, necesario)
}

/**
 * Opacidad del degradado bajo la línea, en su punto más alto. Baja a propósito: da volumen al
 * gráfico sin que el relleno compita con la línea, que es el dato.
 */
private const val OPACIDAD_RELLENO = 0.22f

/**
 * Posición horizontal de cada punto, repartidos **por igual dentro del área de dibujo**.
 *
 * El primero cae exactamente en [inicio] (el margen izquierdo) y el último en `inicio + ancho`
 * (el borde derecho del área), así que ninguno se sale del lienzo, tenga la serie los puntos
 * que tenga.
 *
 * Esto sustituye a un reparto anterior que separaba los puntos a un sexto del ancho y luego
 * "recortaba" por la izquierda lo que sobraba: con ocho pesajes o más, el recorte se comía los
 * primeros puntos y la línea arrancaba fuera de la pantalla. Es una función pura y con test
 * justo porque el fallo no se veía con pocos datos.
 */
fun posicionesX(cantidad: Int, inicio: Float, ancho: Float): List<Float> {
    if (cantidad <= 0) return emptyList()
    if (cantidad == 1) return listOf(inicio + ancho / 2f)

    val paso = ancho / (cantidad - 1)
    return List(cantidad) { indice -> inicio + paso * indice }
}

/**
 * Curva suave que pasa por todos los puntos.
 *
 * Los puntos de control se colocan a media altura entre cada dos puntos y a mitad de camino en
 * horizontal (`cubicTo(medio, y1, medio, y2, ...)`). Esa elección no es solo estética: como los
 * controles comparten la `y` de los extremos, la curva **no puede pasarse** por arriba ni por
 * abajo del tramo, así que el suavizado nunca se sale del margen de seguridad del eje. Un
 * suavizado tipo Catmull-Rom sí puede sobrepasar el valor máximo, y la línea se colaría por el
 * borde que precisamente estamos evitando.
 */
fun pathCurvaSuave(puntos: List<Offset>): Path {
    val path = Path()
    if (puntos.isEmpty()) return path

    path.moveTo(puntos.first().x, puntos.first().y)
    for (i in 0 until puntos.size - 1) {
        val p1 = puntos[i]
        val p2 = puntos[i + 1]
        val medioX = (p1.x + p2.x) / 2f
        path.cubicTo(medioX, p1.y, medioX, p2.y, p2.x, p2.y)
    }
    return path
}

/**
 * Rellena con un degradado el área entre la curva y [baseY] (la base del área de dibujo).
 *
 * El color es el de la línea y se desvanece hacia abajo, de forma que el gráfico gana volumen
 * pero el borde inferior queda limpio.
 */
fun DrawScope.rellenarBajoLaCurva(puntos: List<Offset>, baseY: Float, color: Color) {
    if (puntos.size < 2) return

    val relleno = pathCurvaSuave(puntos).apply {
        lineTo(puntos.last().x, baseY)
        lineTo(puntos.first().x, baseY)
        close()
    }

    // El degradado arranca en el punto más alto de la curva y termina en la base. Se evita que
    // los dos extremos coincidan (línea totalmente plana pegada a la base): un degradado de
    // altura cero no se pinta y el relleno desaparecería.
    val arriba = puntos.minOf { it.y }
    val abajo = if (baseY > arriba) baseY else arriba + 1f

    drawPath(
        path = relleno,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = OPACIDAD_RELLENO),
                color.copy(alpha = 0f)
            ),
            startY = arriba,
            endY = abajo
        )
    )
}
