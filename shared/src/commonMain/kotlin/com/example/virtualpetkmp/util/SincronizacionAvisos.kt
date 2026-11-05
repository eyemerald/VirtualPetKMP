package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Deja las alarmas de la app **exactamente** como dicen los datos: lo que sobra se cancela y solo
 * se programa lo que hay que avisar.
 *
 * **El fallo que arregla.** Hasta la 1.14 cada pantalla cancelaba y programaba "su" aviso
 * recalculando el id desde la base de datos (`cancelar(idNotificacion(tratamiento.id, toma))`). El
 * problema es que una alarma solo se puede cancelar si alguna fila de la base de datos dice cuál es
 * su id: en cuanto la fila desaparecía —al restaurar un `.vpet` que sustituye los datos, al borrar
 * una mascota con su tratamiento o al "Deshacer" una importación— esa alarma se quedaba viva para
 * siempre, porque además el receptor la reprograma para el día siguiente cada vez que suena. Se
 * reprodujo en el emulador: seis alarmas programadas, de las que cinco eran de mascotas o
 * tratamientos que ya no existían.
 *
 * **La solución tiene tres piezas y esta es la segunda.** El [RegistroAvisos] apunta qué hay
 * programado de verdad; esta función lo cancela **todo** y vuelve a programar el conjunto esperado
 * calculado desde la base de datos; y el receptor de la alarma verifica, cuando salta, que su
 * entidad siga existiendo (ver [decidirAviso]).
 *
 * Es idempotente: llamarla dos veces seguidas deja lo mismo. Eso es lo que permite llamarla sin
 * miedo al arrancar, al reiniciar el móvil, al actualizar la app, después de restaurar, de fusionar,
 * de deshacer, al borrar una mascota o cualquiera de sus registros y al editar.
 *
 * @param registro dónde se apunta lo programado. Es una interfaz para que las pruebas usen una
 *   implementación en memoria y no el fichero real de la app.
 * @param ahora momento desde el que se calcula el próximo aviso de cada toma.
 * @return los avisos que han quedado programados, que es también lo que se ha escrito en el
 *   registro.
 */
suspend fun sincronizarAvisos(
    programador: ProgramadorNotificaciones,
    db: VirtualPetDatabase,
    localizacion: Localizacion,
    registro: RegistroAvisos = RegistroAvisosEnFichero,
    ahora: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
): List<AvisoProgramado> {
    // 1. Cancelar TODO lo que la app cree que tiene programado, aunque su fila ya no exista. Es el
    //    único paso que puede limpiar lo que restaurar, borrar o deshacer dejaron atrás.
    registro.leer().forEach { aviso ->
        programador.cancelar(aviso.notificacionId)
    }
    registro.escribir(emptyList())

    // 2. Programar el conjunto esperado, calculado entero desde la base de datos.
    val esperados = mutableListOf<AvisoProgramado>()

    TratamientoRepository(db).getTratamientosConRecordatorio().forEach { (tratamiento, nombreMascota) ->
        tratamiento.id?.let { id ->
            // Cancelar los seis huecos de cada tratamiento sigue siendo necesario: un tratamiento
            // que pasa de tres tomas a dos pudo dejar viva la tercera, y si esa alarma no estaba en
            // el registro (porque la escribió una versión anterior) no la cancela el paso 1.
            ReprogramadorNotificaciones.cancelarTratamiento(programador, id)
            esperados += ReprogramadorNotificaciones.programarTratamiento(
                programador = programador,
                tratamiento = tratamiento,
                nombreMascota = nombreMascota,
                ahora = ahora
            )
        }
    }

    VacunaRepository(db).getAllVacunas().forEach { vacuna ->
        ReprogramadorNotificaciones.programarVacuna(programador, vacuna, localizacion)?.let {
            esperados += it
        }
    }

    PreventivoRepository(db).getAllPreventivos().forEach { preventivo ->
        ReprogramadorNotificaciones.programarPreventivo(programador, preventivo, localizacion)?.let {
            esperados += it
        }
    }

    // 3. Un solo registro, sin ids repetidos: dos filas distintas no pueden compartir una alarma.
    val unicos = esperados.distinctBy { it.notificacionId }
    registro.escribir(unicos)
    return unicos
}
