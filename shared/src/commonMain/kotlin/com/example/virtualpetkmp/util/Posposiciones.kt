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
 * programa un tratamiento mira primero si hay una posposición viva y, si la hay, usa esa hora. Y
 * quien sincroniza los avisos ([sincronizarAvisos]) **no la borra**: la conserva mientras su
 * tratamiento y su toma sigan existiendo, y solo retira las que ya no pueden sonar nunca.
 *
 * **Formato:** un fichero de texto con una línea `idDeLaAlarma=milisegundos`. La clave es el **id
 * de la notificación** (que desde la 1.14 lleva dentro el tratamiento *y* la toma), no el id del
 * tratamiento: si fuera el del tratamiento, posponer la toma de la mañana pospondría también la de
 * la noche. Se eligió texto plano y no la base de datos porque esto es estado efímero de una alarma
 * (no un dato del usuario): así no hace falta una migración del esquema para arreglar un aviso.
 */
interface PosposicionesDeAvisos {

    /**
     * El momento al que se pospuso esa alarma, o `null` si no hay posposición o ya pasó.
     *
     * Una posposición caducada no se devuelve y se borra: si no, un tratamiento volvería a avisar a
     * una hora vieja en el siguiente arranque.
     */
    fun leer(notificacionId: Long): Long?

    /** Apunta el momento al que se ha pospuesto esa alarma. */
    fun guardar(notificacionId: Long, avisoMillis: Long)

    /** Olvida la posposición de esa alarma: se aceptó, ya sonó o su entidad ya no existe. */
    fun borrar(notificacionId: Long)

    /** Todas las posposiciones guardadas, estén vivas o caducadas. Es para depurarlas. */
    fun todas(): Map<Long, Long>

    /**
     * Borra las posposiciones cuya alarma ya no está entre [vigentes].
     *
     * Es lo que impide que queden para siempre las de tratamientos borrados o tomas que se
     * quitaron al editar: esas alarmas se cancelan en la sincronización, así que su posposición
     * tampoco puede volver a usarse.
     */
    fun depurar(vigentes: Set<Long>)
}

/**
 * Las posposiciones, en un fichero de texto de una carpeta concreta.
 *
 * La carpeta es un parámetro (y no [carpetaDatosApp] siempre) para que las pruebas usen un
 * directorio temporal y no el de la app de verdad.
 */
class PosposicionesEnFichero(
    private val carpeta: String = carpetaDatosApp()
) : PosposicionesDeAvisos {

    override fun leer(notificacionId: Long): Long? = leer(notificacionId, ahoraMillis(), carpeta)

    /**
     * Igual que [leer], con el "ahora" y la carpeta explícitos.
     *
     * @param ahora instante con el que se decide si la posposición sigue viva.
     */
    fun leer(notificacionId: Long, ahora: Long, carpeta: String = this.carpeta): Long? {
        val entradas = leerTodas(carpeta)
        val momento = entradas[notificacionId] ?: return null

        // Una posposición que ya pasó no vale: se limpia y se sigue con la hora normal.
        if (momento <= ahora) {
            guardarTodas(entradas - notificacionId, carpeta)
            return null
        }
        return momento
    }

    override fun guardar(notificacionId: Long, avisoMillis: Long) {
        guardarTodas(leerTodas(carpeta) + (notificacionId to avisoMillis), carpeta)
    }

    override fun borrar(notificacionId: Long) = borrar(notificacionId, carpeta)

    /** Igual que [borrar], sobre una carpeta concreta. */
    fun borrar(notificacionId: Long, carpeta: String) {
        val entradas = leerTodas(carpeta)
        if (!entradas.containsKey(notificacionId)) return
        guardarTodas(entradas - notificacionId, carpeta)
    }

    override fun todas(): Map<Long, Long> = leerTodas(carpeta)

    override fun depurar(vigentes: Set<Long>) {
        val actuales = leerTodas(carpeta)
        val quedan = actuales.filterKeys { it in vigentes }
        // Solo se escribe si hay algo que quitar: sincronizar no puede tocar el disco por gusto.
        if (quedan.size != actuales.size) guardarTodas(quedan, carpeta)
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

    private companion object {
        const val FICHERO = "posposiciones.txt"
    }
}

/**
 * El de verdad: el fichero de la carpeta de datos de la app.
 *
 * Se mantiene el nombre y la firma de antes (con `ahora` y `carpeta` opcionales) porque hay código y
 * pruebas que ya lo usan así; por dentro es el fichero de siempre.
 */
object Posposiciones {

    fun leer(
        notificacionId: Long,
        ahora: Long = ahoraMillis(),
        carpeta: String = carpetaDatosApp()
    ): Long? = PosposicionesEnFichero(carpeta).leer(notificacionId, ahora, carpeta)

    fun guardar(
        notificacionId: Long,
        avisoMillis: Long,
        carpeta: String = carpetaDatosApp()
    ) = PosposicionesEnFichero(carpeta).guardar(notificacionId, avisoMillis)

    fun borrar(
        notificacionId: Long,
        carpeta: String = carpetaDatosApp()
    ) = PosposicionesEnFichero(carpeta).borrar(notificacionId, carpeta)
}

/** Posposiciones en memoria: las de las pruebas, que no pueden tocar la carpeta de la app. */
class PosposicionesEnMemoria(inicial: Map<Long, Long> = emptyMap()) : PosposicionesDeAvisos {

    private val entradas = inicial.toMutableMap()

    /** Lo que hay guardado ahora mismo, para comprobarlo desde las pruebas. */
    val actuales: Map<Long, Long> get() = entradas.toMap()

    override fun leer(notificacionId: Long): Long? = entradas[notificacionId]

    override fun guardar(notificacionId: Long, avisoMillis: Long) {
        entradas[notificacionId] = avisoMillis
    }

    override fun borrar(notificacionId: Long) {
        entradas.remove(notificacionId)
    }

    override fun todas(): Map<Long, Long> = entradas.toMap()

    override fun depurar(vigentes: Set<Long>) {
        entradas.keys.retainAll(vigentes)
    }
}
