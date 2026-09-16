package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Peso
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class PesoRepository(private val database: VirtualPetDatabase) {

    suspend fun getPesosByMascotaId(mascotaId: Long): List<Peso> = withContext(Dispatchers.IO) {
        database.pesosQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Peso(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    fecha = LocalDate.parse(fila.fecha),
                    peso = fila.peso,
                    notas = fila.notas
                )
            }
    }

    suspend fun insertPeso(mascotaId: Long, fecha: LocalDate, peso: Double, notas: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                database.pesosQueries.insert(
                    mascotaId = mascotaId,
                    fecha = fecha.toString(),
                    peso = peso,
                    notas = notas
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updatePeso(id: Long, fecha: LocalDate, peso: Double, notas: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                database.pesosQueries.update(
                    fecha = fecha.toString(),
                    peso = peso,
                    notas = notas,
                    id = id
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deletePeso(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.pesosQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
