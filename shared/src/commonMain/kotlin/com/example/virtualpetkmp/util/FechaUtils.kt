package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate

/**
 * Convierte una fecha a formato europeo (DD/MM/AAAA) para mostrarla al usuario.
 * Internamente la fecha se sigue guardando en formato ISO (AAAA-MM-DD).
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

/**
 * Calcula la edad en años y meses a partir de la fecha de nacimiento.
 */
fun calcularEdad(fechaNacimiento: LocalDate, hoy: LocalDate): String {
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

    return "$anios años y $meses meses"
}
