fun LocalDate.toFormatoEuropeo(): String {
    val dia = dayOfMonth.toString().padStart(2, '0')
    val mes = monthNumber.toString().padStart(2, '0')
    return "$dia/$mes/$year"
}
