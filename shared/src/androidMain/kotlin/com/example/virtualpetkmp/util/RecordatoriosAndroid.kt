package com.example.virtualpetkmp.util

import android.content.Context
import com.example.virtualpetkmp.data.DatabaseFactory

/**
 * Vuelve a programar **todos** los recordatorios (dosis y tratamientos) de todas las mascotas.
 *
 * Android borra las alarmas al apagar o reiniciar el móvil, así que el receptor de
 * `BOOT_COMPLETED` llama aquí: lee de la base de datos qué hay que avisar y lo programa otra vez,
 * sin que el usuario tenga que abrir la app.
 *
 * **Las dosis se olvidaban.** Hasta la 1.3 esto solo reprogramaba tratamientos, así que al
 * reiniciar el móvil los avisos de vacunas y preventivos se perdían en silencio hasta que el
 * usuario abriera la app o la ficha de la mascota. Como un aviso de vacuna se programa con quince
 * días de antelación, el usuario podía pasarse semanas sin abrirla y no recibir nada. Lo encontró
 * una prueba real: cinco alarmas antes de reiniciar, una sola después.
 *
 * La lógica está en `reprogramarAvisosDeTodo` (commonMain), compartida con el arranque de la app y
 * con la restauración de una copia: así no puede volver a haber dos versiones que se separen.
 */
suspend fun reprogramarRecordatorios(context: Context) {
    reprogramarAvisosDeTodo(
        programador = crearProgramadorNotificaciones(context),
        db = DatabaseFactory(context).createDatabase(),
        // Sin composición: este código corre dentro de un receptor y no hay interfaz.
        localizacion = localizacionFueraDeComposicion()
    )
}
