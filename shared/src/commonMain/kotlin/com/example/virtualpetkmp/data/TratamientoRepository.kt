package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a los tratamientos con medicamentos.
 *
 * Devuelve la lista tal cual está guardada: **la separación entre activos e historial no se
 * hace aquí**, sino en el `FichaMascotaViewModel`, porque depende de la fecha de hoy y eso es
 * lógica de presentación, no de persistencia.
 *
 * El recordatorio se guarda como dos columnas (`hora` en "HH:mm" y `notificar` en 0/1) y aquí se
 * convierte al modelo: `notificar` es un `Boolean` en Kotlin porque en el resto de la app no tiene
 * sentido ir arrastrando un entero.
 */
class TratamientoRepository(private val database: VirtualPetDatabase) {

    suspend fun getTratamientosByMascotaId(mascotaId: Long): List<Tratamiento> = withContext(Dispatchers.IO) {
        database.tratamientosQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Tratamiento(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    nombreMedicamento = fila.nombreMedicamento,
                    dosis = fila.dosis,
                    frecuencia = fila.frecuencia,
                    fechaInicio = LocalDate.parse(fila.fechaInicio),
                    fechaFin = fila.fechaFin?.let { LocalDate.parse(it) },
                    hora = fila.hora,
                    notificar = fila.notificar == 1L
                )
            }
    }

    /**
     * Los tratamientos de **todas** las mascotas que piden aviso, con el nombre de su mascota.
     *
     * Lo usa el receptor de arranque del móvil: al reiniciar, Android borra las alarmas y hay que
     * volver a programarlas sin que el usuario abra la app.
     */
    suspend fun getTratamientosConRecordatorio(): List<Pair<Tratamiento, String>> =
        withContext(Dispatchers.IO) {
            database.tratamientosQueries.selectConRecordatorio()
                .executeAsList()
                .map { fila ->
                    Tratamiento(
                        id = fila.id,
                        mascotaId = fila.mascotaId,
                        nombreMedicamento = fila.nombreMedicamento,
                        dosis = fila.dosis,
                        frecuencia = fila.frecuencia,
                        fechaInicio = LocalDate.parse(fila.fechaInicio),
                        fechaFin = fila.fechaFin?.let { LocalDate.parse(it) },
                        hora = fila.hora,
                        notificar = fila.notificar == 1L
                    ) to fila.nombreMascota
                }
        }

    suspend fun insertTratamiento(
        mascotaId: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?,
        hora: String? = null,
        notificar: Boolean = false,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.tratamientosQueries.insert(
                mascotaId = mascotaId,
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio.toString(),
                fechaFin = fechaFin?.toString(),
                hora = hora,
                notificar = if (notificar) 1L else 0L,
                uuid = uuid,
                actualizadoEn = actualizadoEn
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTratamiento(
        id: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?,
        hora: String? = null,
        notificar: Boolean = false,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.tratamientosQueries.update(
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio.toString(),
                fechaFin = fechaFin?.toString(),
                hora = hora,
                notificar = if (notificar) 1L else 0L,
                actualizadoEn = actualizadoEn,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTratamiento(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.tratamientosQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
