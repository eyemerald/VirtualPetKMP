package com.example.virtualpetkmp.util

import kotlin.math.roundToInt

/**
 * Cuánto se reduce la foto de una mascota al guardarla.
 *
 * **De dónde sale [LADO_MAXIMO_FOTO_MASCOTA].** La foto de la mascota se pinta como mucho a
 * **84 dp** de ancho (la cabecera de la ficha, `tamanoAvatar = 84.dp`). Para verse nítida en una
 * pantalla de alta densidad hay que guardar al menos ese tamaño en píxeles: 84 dp × 4,0 (xxxhdpi)
 * = 336 px. Con **640 px** (1,9 veces eso) se gana margen para tres cosas concretas:
 * 1. pantallas de densidad aún mayor y el reescalado del sistema;
 * 2. la foto del **PDF** de la ficha, que se imprime a 46 dp: 640 px dan ~350 ppp en vez de ~180;
 * 3. futuros tamaños de foto más grandes sin volver a tocar los ficheros guardados.
 *
 * El coste está medido con fotos reales de 3072×4096: a 640 px cada foto pesa **~60-70 KB**
 * (frente a ~20 KB a 336 px y 3,5 MB en origen), así que 40 mascotas con dos fotos son ~5 MB.
 *
 * Si algún día la foto se enseña mucho más grande, hay que **volver a calcular** este número
 * (lado en dp × densidad) y actualizar el test.
 */
const val LADO_MAXIMO_FOTO_MASCOTA = 640

/** Ancho y alto en píxeles. `ancho <= 0` o `alto <= 0` significa "no hay imagen válida". */
data class DimensionesFoto(val ancho: Int, val alto: Int)

/**
 * Calcula el tamaño al que hay que guardar una foto de mascota.
 *
 * Reglas, todas con su test:
 * - **Mantiene la proporción** (se escala el lado mayor y el otro sale de la relación original).
 * - **Nunca amplía**: si la foto ya cabe, se deja tal cual.
 * - **Cuadrada** también se reduce por el lado mayor.
 * - **Tamaño inválido** (0 o negativo, p. ej. un fichero que no se pudo decodificar) devuelve
 *   `0 x 0` para que quien llame sepa que no hay nada que guardar.
 */
fun calcularDimensionesFoto(
    anchoOriginal: Int,
    altoOriginal: Int,
    ladoMaximo: Int = LADO_MAXIMO_FOTO_MASCOTA
): DimensionesFoto {
    if (anchoOriginal <= 0 || altoOriginal <= 0) return DimensionesFoto(0, 0)
    if (ladoMaximo <= 0) return DimensionesFoto(0, 0)

    val ladoMayor = maxOf(anchoOriginal, altoOriginal)
    if (ladoMayor <= ladoMaximo) return DimensionesFoto(anchoOriginal, altoOriginal)

    val factor = ladoMaximo.toDouble() / ladoMayor
    val ancho = (anchoOriginal * factor).roundToInt().coerceAtLeast(1)
    val alto = (altoOriginal * factor).roundToInt().coerceAtLeast(1)
    return DimensionesFoto(ancho, alto)
}

/**
 * Reduce la foto que ya está en [ruta] y la vuelve a dejar en su sitio.
 *
 * Se llama justo después de copiar la foto elegida a la carpeta privada, así que el fichero que se
 * modifica es **la copia**, nunca el original del usuario. Si algo falla, la foto se queda como
 * estaba (mejor una foto grande que ninguna) y se devuelve `false`.
 *
 * Formato de salida: **JPEG con calidad 82** si la imagen no tiene transparencia; si la tiene (un
 * PNG con zonas transparentes), se mantiene **PNG** para no rellenar de negro ni de blanco lo que
 * el usuario quería transparente.
 */
expect fun reducirFotoDeMascota(ruta: String): Boolean
