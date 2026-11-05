package com.example.virtualpetkmp.util

/**
 * Por qué un aviso que ha saltado **no debería sonar**.
 *
 * Se enumera en vez de devolver un texto para que la decisión se pueda probar sin móvil y para que
 * el registro de la app diga algo concreto cuando limpia una alarma zombi.
 */
enum class MotivoZombi {
    /** La alarma es de una versión anterior: no lleva `uuid` ni tipo, así que no se puede verificar. */
    SIN_IDENTIDAD,

    /** La fila (tratamiento, vacuna o preventivo) ya no está en la base de datos. */
    ENTIDAD_BORRADA,

    /** El tratamiento sigue existiendo, pero esa toma ya no está entre sus horas de aviso. */
    HORA_CAMBIADA,

    /** El tratamiento ya no avisa (se le quitó el recordatorio o se acabó) o la dosis ya no toca. */
    YA_NO_TOCA
}

/** Qué hacer con un aviso que acaba de saltar. */
sealed interface DecisionAviso {

    /** Se notifica: la entidad existe, la toma sigue siendo suya y el aviso sigue vigente. */
    data object Sonar : DecisionAviso

    /**
     * No se notifica y **no se reprograma**: el aviso se cancela a sí mismo y se quita del registro.
     * Es lo que limpia las alarmas que quedaron vivas en versiones anteriores.
     */
    data class Zombi(val motivo: MotivoZombi) : DecisionAviso
}

/**
 * Lo que la base de datos dice **ahora mismo** de la entidad de un aviso.
 *
 * No es la entidad entera a propósito: la decisión solo necesita saber si sigue existiendo, qué
 * horas avisa hoy y si el aviso procede. Así se puede construir en una prueba con tres líneas.
 *
 * @param horasDeAviso las horas actuales del tratamiento, ya normalizadas. Vacío en vacunas y
 *   preventivos, que avisan una sola vez.
 * @param vigente `false` cuando el tratamiento ya no pide aviso o está fuera de sus fechas, o
 *   cuando la dosis de una vacuna o un preventivo ya no cae en el día que se programó.
 */
data class EstadoEntidadAviso(
    val uuid: String,
    val horasDeAviso: List<String> = emptyList(),
    val vigente: Boolean = true
)

/**
 * ¿Debe sonar este aviso?
 *
 * Es **la** regla que decide si un aviso programado sigue teniendo sentido, y es una función pura
 * para poder probarla con los cuatro casos que importan: la entidad se borró, le cambiaron la hora
 * a esa toma, la entidad existe con otro id pero otra hora, y el aviso sigue vigente.
 *
 * Los tres motivos de "no" son los mismos que dejaban alarmas sonando para siempre:
 *
 * 1. Se restauró una copia (o se borró la mascota) y la fila desapareció: antes nadie cancelaba esa
 *    alarma, porque para cancelarla se recalculaba su id desde la base de datos y ya no había fila.
 * 2. Al tratamiento le cambiaron las horas: la alarma de la toma que sobraba seguía viva.
 * 3. El tratamiento se terminó o se le quitó el recordatorio.
 *
 * @param tipo tipo guardado en la alarma, o `null` si la escribió una versión anterior.
 * @param uuidDeLaAlarma identificador universal guardado en la alarma.
 * @param horaDeLaAlarma "HH:mm" de la toma que avisa, en los tratamientos.
 * @param estado lo que hay hoy en la base de datos, o `null` si la entidad ya no existe.
 */
fun decidirAviso(
    tipo: TipoAviso?,
    uuidDeLaAlarma: String?,
    horaDeLaAlarma: String?,
    estado: EstadoEntidadAviso?
): DecisionAviso {
    // Una alarma de antes de este cambio no lleva ni tipo ni uuid: no se puede verificar, así que se
    // trata como zombi. Al arrancar, la sincronización vuelve a programar las vigentes con el
    // formato nuevo.
    if (tipo == null || uuidDeLaAlarma.isNullOrBlank()) {
        return DecisionAviso.Zombi(MotivoZombi.SIN_IDENTIDAD)
    }

    if (estado == null || estado.uuid != uuidDeLaAlarma) {
        return DecisionAviso.Zombi(MotivoZombi.ENTIDAD_BORRADA)
    }

    if (!estado.vigente) {
        return DecisionAviso.Zombi(MotivoZombi.YA_NO_TOCA)
    }

    if (tipo == TipoAviso.TRATAMIENTO) {
        val hora = horaDeLaAlarma?.trim()
        if (hora.isNullOrEmpty() || estado.horasDeAviso.none { it == hora }) {
            return DecisionAviso.Zombi(MotivoZombi.HORA_CAMBIADA)
        }
    }

    return DecisionAviso.Sonar
}
