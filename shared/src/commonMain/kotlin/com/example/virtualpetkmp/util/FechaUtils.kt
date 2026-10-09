package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

/**
 * Convierte una fecha a formato europeo (DD/MM/AAAA) para mostrarla al usuario.
 * Internamente la fecha se sigue guardando en formato ISO (AAAA-MM-DD).
 *
 * Se mantiene por compatibilidad y para los sitios que aún no usan la capa de localización;
 * lo nuevo debería usar `Localizacion.formatearFecha`, que respeta la región.
 */
fun LocalDate.toFormatoEuropeo(): String {
    val dia = dayOfMonth.toString().padStart(2, '0')
    val mes = monthNumber.toString().padStart(2, '0')
    return "$dia/$mes/$year"
}

/**
 * Convierte un texto en formato europeo (DD/MM/AAAA) a LocalDate.
 * Lanza una excepción si el formato no es válido.
 */
fun parseFormatoEuropeo(texto: String): LocalDate {
    val partes = texto.trim().split("/")
    require(partes.size == 3) { "El formato debe ser DD/MM/AAAA" }

    val dia = partes[0].trim().toIntOrNull()
        ?: throw IllegalArgumentException("Día inválido")
    val mes = partes[1].trim().toIntOrNull()
        ?: throw IllegalArgumentException("Mes inválido")
    val anio = partes[2].trim().toIntOrNull()
        ?: throw IllegalArgumentException("Año inválido")

    return LocalDate(anio, mes, dia)
}

/**
 * Lee una fecha escrita por el usuario aceptando el orden de su región y, como respaldo, el
 * contrario.
 *
 * Por qué se acepta el otro orden: el usuario de habla inglesa escribe MM/DD/AAAA, pero las
 * fechas que ya tenía guardadas y las que le muestra la app pueden ser DD/MM. Rechazar el
 * formato "que no toca" llevaría a errores tontos al corregir un registro antiguo.
 *
 * Cuando el texto es ambiguo ("03/04/2026" vale como 3 de abril y como 4 de marzo) **gana el
 * orden preferido de la región**, que es lo que el usuario espera. Por eso el orden preferido
 * es un parámetro obligatorio: no hay una interpretación "correcta" sin saber de dónde es.
 *
 * Devuelve `null` si no encaja en ningún orden o si la fecha no existe (31/02).
 */
fun parseFechaLocalizada(texto: String, ordenPreferido: OrdenFecha): LocalDate? {
    val partes = texto.trim().split("/")
    if (partes.size != 3) return null

    val primero = partes[0].trim().toIntOrNull() ?: return null
    val segundo = partes[1].trim().toIntOrNull() ?: return null
    val anio = partes[2].trim().toIntOrNull() ?: return null
    // Los años de dos cifras no se admiten: ambigüedad innecesaria.
    if (anio < 1000 || anio > 9999) return null

    val intentos = when (ordenPreferido) {
        OrdenFecha.DIA_MES_ANIO -> listOf(primero to segundo, segundo to primero)
        OrdenFecha.MES_DIA_ANIO -> listOf(segundo to primero, primero to segundo)
    }

    return intentos.firstNotNullOfOrNull { (dia, mes) ->
        runCatching { LocalDate(anio, mes, dia) }.getOrNull()
    }
}

/**
 * Número de días desde el 1970-01-01 (día "epoch"), calculado de forma
 * autocontenida sin depender de funciones específicas de kotlinx-datetime
 * que puedan variar entre versiones de la librería.
 * Se usa para posicionar puntos proporcionalmente en el tiempo (ej: gráficos).
 */
fun LocalDate.aDiasEpoch(): Long {
    val y = if (monthNumber <= 2) year.toLong() - 1 else year.toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (monthNumber + 9) % 12
    val doy = (153 * mp + 2) / 5 + dayOfMonth - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/**
 * Inversa de aDiasEpoch: convierte un número de días epoch de vuelta a LocalDate.
 * Igualmente autocontenida, sin depender de la versión de kotlinx-datetime.
 */
fun localDateDeDiasEpoch(diasEpoch: Long): LocalDate {
    val z = diasEpoch + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val dia = doy - (153 * mp + 2) / 5 + 1
    val mes = if (mp < 10) mp + 3 else mp - 9
    val anio = if (mes <= 2) y + 1 else y
    return LocalDate(anio.toInt(), mes.toInt(), dia.toInt())
}

/** Años y meses cumplidos de una mascota. */
data class EdadMascota(val anios: Int, val meses: Int)

/**
 * Calcula la edad en años y meses a partir de la fecha de nacimiento.
 *
 * Devuelve **números, no texto**: el texto se compone con recursos y plurales en
 * `textoEdad()`, porque cada idioma tiene sus propias formas ("1 año" / "2 años") y no vale
 * concatenar a mano.
 */
fun calcularEdadMeses(fechaNacimiento: LocalDate, hoy: LocalDate): EdadMascota {
    var anios = hoy.year - fechaNacimiento.year
    var meses = hoy.monthNumber - fechaNacimiento.monthNumber
    val dias = hoy.dayOfMonth - fechaNacimiento.dayOfMonth

    if (dias < 0) {
        meses -= 1
    }
    if (meses < 0) {
        anios -= 1
        meses += 12
    }
    if (anios < 0) {
        anios = 0
        meses = 0
    }

    return EdadMascota(anios, meses)
}

/**
 * Edad en texto, en el idioma activo: "2 años y 6 meses", "2 years and 6 months".
 *
 * El texto lo compone [Localizacion.formatearEdad], que ya conoce los plurales y el conector
 * de cada idioma. Se hace así porque `pluralStringResource` elige la forma según la cantidad
 * pero **no sustituye argumentos**, así que no sirve para frases con el número dentro.
 */
@Composable
fun textoEdad(edad: EdadMascota): String =
    localizacion.formatearEdad(edad.anios, edad.meses)

/**
 * Atajo para los sitios que solo quieren el texto ya montado.
 *
 * Está marcado como `@Composable` a propósito: la edad se muestra, así que necesita el idioma
 * activo, y así no se puede llamar por error desde lógica de negocio.
 */
@Composable
fun calcularEdad(fechaNacimiento: LocalDate, hoy: LocalDate): String =
    textoEdad(calcularEdadMeses(fechaNacimiento, hoy))
