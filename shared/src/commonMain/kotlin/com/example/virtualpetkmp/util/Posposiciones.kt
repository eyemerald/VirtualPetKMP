package com.example.virtualpetkmp.util

/**
 * Los avisos de tratamiento que el usuario ha pospuesto y todavía no han vuelto a salir.
 *
 * **Por qué existe este fichero.** "Posponer" reprogramaba la alarma y ya está: el momento nuevo
 * solo vivía dentro de `AlarmManager`. Android **borra las alarmas al reiniciar**, así que el aviso
 * pospuesto se perdía y el receptor de arranque reprogramaba el siguiente aviso *diario* —o sea,
 * mañana—. Eso es exactamente lo que pasó en una prueba real: aviso de las 13:34 pospuesto,
 * reinicio, y nada hasta el día siguiente. Además, abrir la ficha de la mascota también reprograma
 * los avisos, así que la posposición se perdía igual sin reiniciar nada.
 *
 * Guardándolo aquí, el momento pospuesto **sobrevive** al reinicio y a que la app reprograme: quien
 * programa un tratamiento mira primero si hay una posposición viva y, si la hay, usa esa hora.
 *
 * **Formato:** un fichero de texto con una línea `idDelTratamiento=milisegundos`. Se eligió texto
 * plano y no la base de datos porque esto es estado efímero de una alarma (no un dato del usuario):
 * así no hace falta una migración del esquema para arreglar un aviso.
 */
object Posposiciones {

    private const val FICHERO = "posposiciones.txt"

    /** Lee la posposición viva de un tratamiento, o `null` si no hay o ya pasó. */
    fun leer(
        tratamientoId: Long,
        ahora: Long = ahoraMillis(),
        carpeta: String = carpetaDatosApp()
    ): Long? {
        val entradas = leerTodas(carpeta)
        val momento = entradas[tratamientoId] ?: return null

        // Una posposición que ya pasó no vale: se limpia y se sigue con la hora normal. Si no se
        // borrara, un tratamiento volvería a avisar a una hora vieja en el siguiente arranque.
        if (momento <= ahora) {
            guardarTodas(entradas - tratamientoId, carpeta)
            return null
        }
        return momento
    }

    fun guardar(
        tratamientoId: Long,
        avisoMillis: Long,
        carpeta: String = carpetaDatosApp()
    ) {
        guardarTodas(leerTodas(carpeta) + (tratamientoId to avisoMillis), carpeta)
    }

    fun borrar(
        tratamientoId: Long,
        carpeta: String = carpetaDatosApp()
    ) {
        val entradas = leerTodas(carpeta)
        if (!entradas.containsKey(tratamientoId)) return
        guardarTodas(entradas - tratamientoId, carpeta)
    }

    private fun leerTodas(carpeta: String): Map<Long, Long> {
        val ruta = "$carpeta/$FICHERO"
        val texto = leerBytes(ruta)?.toString(Charsets.UTF_8) ?: return emptyMap()

        return texto.lineSequence()
            .mapNotNull { linea ->
                val partes = linea.trim().split('=')
                if (partes.size != 2) {
                    null
                } else {
                    val id = partes[0].toLongOrNull()
                    val momento = partes[1].toLongOrNull()
                    if (id == null || momento == null) null else id to momento
                }
            }
            .toMap()
    }

    private fun guardarTodas(entradas: Map<Long, Long>, carpeta: String) {
        crearCarpeta(carpeta)
        val texto = entradas.entries.joinToString("\n") { (id, momento) -> "$id=$momento" }
        // Se escribe siempre, aunque se quede vacío: así "borrar" deja el fichero limpio y no hay
        // que distinguir entre "no existe" y "no hay ninguna".
        val escrito = escribirBytes("$carpeta/$FICHERO", texto.toByteArray(Charsets.UTF_8))
        if (!escrito) println("[Posposiciones] no se pudo guardar en $carpeta")
    }
}
