package com.example.virtualpetkmp.util

import android.content.Context
import com.example.virtualpetkmp.data.DatabaseFactory

/**
 * Vuelve a dejar los recordatorios (dosis y tratamientos) como dicen los datos, sin interfaz.
 *
 * Android borra las alarmas al apagar o reiniciar el móvil y al actualizar la app, así que el
 * receptor de `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` llama aquí: lee de la base de datos qué hay
 * que avisar y lo programa otra vez, sin que el usuario tenga que abrir la app.
 *
 * La lógica está en `sincronizarAvisos()` (commonMain), compartida con el arranque de la app, la
 * restauración de una copia, la fusión y cada borrado o edición: así no puede volver a haber dos
 * versiones que se separen.
 *
 * Es también lo que, **después de actualizar**, deja las alarmas con el formato nuevo (con `uuid`):
 * las que escribió la versión anterior se cancelan al sonar (no se pueden verificar) y el arranque
 * ya las reprograma.
 */
suspend fun reprogramarRecordatorios(context: Context) {
    sincronizarAvisos(
        programador = crearProgramadorNotificaciones(context),
        db = DatabaseFactory(context).createDatabase(),
        // Sin composición: este código corre dentro de un receptor y no hay interfaz.
        localizacion = localizacionFueraDeComposicion()
    )
}
