package com.example.virtualpetkmp.util

import android.content.Context
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository

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
 * Cancelar antes de programar es lo que hace que sea idempotente: si el arranque se repite o
 * quedara alguna alarma viva, no se duplican los avisos.
 */
suspend fun reprogramarRecordatorios(context: Context) {
    val baseDatos = DatabaseFactory(context).createDatabase()
    val programador = crearProgramadorNotificaciones(context)
    // La fecha de la notificación se escribe en el idioma y el formato del móvil: se decide aquí
    // una vez y se usa en los tres tipos de aviso. Es la variante sin composición, porque esto
    // corre dentro de un receptor y no hay interfaz.
    val localizacion = localizacionFueraDeComposicion()

    TratamientoRepository(baseDatos).getTratamientosConRecordatorio().forEach { (tratamiento, nombreMascota) ->
        tratamiento.id?.let { id ->
            ReprogramadorNotificaciones.cancelarTratamiento(programador, id)
            ReprogramadorNotificaciones.programarTratamiento(programador, tratamiento, nombreMascota)
        }
    }

    VacunaRepository(baseDatos).getAllVacunas().forEach { vacuna ->
        vacuna.id?.let { id ->
            ReprogramadorNotificaciones.cancelar(programador, id)
            ReprogramadorNotificaciones.programarVacuna(programador, vacuna, localizacion)
        }
    }

    PreventivoRepository(baseDatos).getAllPreventivos().forEach { preventivo ->
        preventivo.id?.let { id ->
            ReprogramadorNotificaciones.cancelarPreventivo(programador, id)
            ReprogramadorNotificaciones.programarPreventivo(programador, preventivo, localizacion)
        }
    }
}
