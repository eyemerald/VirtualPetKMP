package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class VacunaRepository(private val database: VirtualPetDatabase) {

    suspend fun getVacunasByMascotaId(mascotaId: Long): List<Vacuna> = withContext(Dispatchers.IO) {
        database.vacunasQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Vacuna(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    nombre = fila.nombre,
                    fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                    fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                    veterinario = fila.veterinario,
                    lote = fila.lote
                )
            }
    }

    suspend fun insertVacuna(
        mascotaId: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.vacunasQueries.insert(
                mascotaId = mascotaId,
                nombre = nombre,
                fechaAplicacion = fechaAplicacion.toString(),
                fechaProximaDosis = fechaProximaDosis.toString(),
                veterinario = veterinario,
                lote = lote
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVacuna(
        id: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.vacunasQueries.update(
                nombre = nombre,
                fechaAplicacion = fechaAplicacion.toString(),
                fechaProximaDosis = fechaProximaDosis.toString(),
                veterinario = veterinario,
                lote = lote,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVacuna(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.vacunasQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
