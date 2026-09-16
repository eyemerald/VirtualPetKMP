package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

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
                    fechaFin = fila.fechaFin?.let { LocalDate.parse(it) }
                )
            }
    }

    suspend fun insertTratamiento(
        mascotaId: Long,
        nombreMedicamento: String,
        dosis: String?,
        frecuencia: String?,
        fechaInicio: LocalDate,
        fechaFin: LocalDate?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.tratamientosQueries.insert(
                mascotaId = mascotaId,
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio.toString(),
                fechaFin = fechaFin?.toString()
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
        fechaFin: LocalDate?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.tratamientosQueries.update(
                nombreMedicamento = nombreMedicamento,
                dosis = dosis,
                frecuencia = frecuencia,
                fechaInicio = fechaInicio.toString(),
                fechaFin = fechaFin?.toString(),
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
