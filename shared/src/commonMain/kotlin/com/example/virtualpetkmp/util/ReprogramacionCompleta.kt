package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase

/**
 * Vuelve a programar **todos** los avisos de la base de datos: tratamientos, vacunas y
 * preventivos.
 *
 * Estaba escrito dos veces (al arrancar la app y en el receptor de reinicio) y las dos copias se
 * habían separado ya una vez: la del reinicio se olvidó de las dosis y los avisos de vacunas se
 * perdían en silencio. Con una sola función no puede volver a pasar, y ahora también la usa la
 * restauración de una copia, que deja la base de datos entera nueva y necesita reprogramarlo todo.
 *
 * Cancelar antes de programar es lo que la hace idempotente: si se llama dos veces no se duplican
 * los avisos.
 */
suspend fun reprogramarAvisosDeTodo(
    programador: ProgramadorNotificaciones,
    db: VirtualPetDatabase,
    localizacion: Localizacion
) {
    TratamientoRepository(db).getTratamientosConRecordatorio().forEach { (tratamiento, nombreMascota) ->
        tratamiento.id?.let { id ->
            ReprogramadorNotificaciones.cancelarTratamiento(programador, id)
            ReprogramadorNotificaciones.programarTratamiento(programador, tratamiento, nombreMascota)
        }
    }

    VacunaRepository(db).getAllVacunas().forEach { vacuna ->
        vacuna.id?.let { id ->
            ReprogramadorNotificaciones.cancelar(programador, id)
            ReprogramadorNotificaciones.programarVacuna(programador, vacuna, localizacion)
        }
    }

    PreventivoRepository(db).getAllPreventivos().forEach { preventivo ->
        preventivo.id?.let { id ->
            ReprogramadorNotificaciones.cancelarPreventivo(programador, id)
            ReprogramadorNotificaciones.programarPreventivo(programador, preventivo, localizacion)
        }
    }
}
