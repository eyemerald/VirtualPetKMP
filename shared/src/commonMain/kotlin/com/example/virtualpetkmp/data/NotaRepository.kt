package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Nota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotaRepository(private val database: VirtualPetDatabase) {

    suspend fun getNotasByMascotaId(mascotaId: Long): List<Nota> = withContext(Dispatchers.IO) {
        database.notasQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila -> Nota(id = fila.id, mascotaId = fila.mascotaId, texto = fila.texto) }
    }

    suspend fun insertNota(mascotaId: Long, texto: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.notasQueries.insert(mascotaId = mascotaId, texto = texto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNota(id: Long, texto: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.notasQueries.update(texto = texto, id = id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNota(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.notasQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
