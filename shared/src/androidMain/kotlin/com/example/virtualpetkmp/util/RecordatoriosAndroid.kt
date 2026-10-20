package com.example.virtualpetkmp.util

import android.content.Context
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.TratamientoRepository

/**
 * Vuelve a programar los recordatorios de tratamiento de **todas** las mascotas.
 *
 * Android borra las alarmas al apagar o reiniciar el móvil, así que el receptor de
 * `BOOT_COMPLETED` llama aquí: lee de la base de datos qué tratamientos piden aviso (con el nombre
 * de su mascota) y los programa otra vez, sin que el usuario tenga que abrir la app.
 *
 * Cancelar antes de programar es lo que hace que sea idempotente: si el arranque se repite o
 * quedara alguna alarma viva, no se duplican los avisos.
 */
suspend fun reprogramarRecordatoriosDeTratamientos(context: Context) {
    val baseDatos = DatabaseFactory(context).createDatabase()
    val programador = crearProgramadorNotificaciones(context)

    TratamientoRepository(baseDatos).getTratamientosConRecordatorio().forEach { (tratamiento, nombreMascota) ->
        tratamiento.id?.let { id ->
            ReprogramadorNotificaciones.cancelarTratamiento(programador, id)
            ReprogramadorNotificaciones.programarTratamiento(programador, tratamiento, nombreMascota)
        }
    }
}
