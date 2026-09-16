package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class RevisionRepository(private val database: VirtualPetDatabase) {

    suspend fun getRevisionesByMascotaId(mascotaId: Long): List<Revision> = withContext(Dispatchers.IO) {
        database.revisionesQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Revision(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    fecha = LocalDate.parse(fila.fecha),
                    motivo = fila.motivo,
                    diagnostico = fila.diagnostico,
                    notas = fila.notas,
                    veterinario = fila.veterinario
                )
            }
    }

    suspend fun insertRevision(
        mascotaId: Long,
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.revisionesQueries.insert(
                mascotaId = mascotaId,
                fecha = fecha.toString(),
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRevision(
        id: Long,
        fecha: LocalDate,
        motivo: String,
        diagnostico: String?,
        notas: String?,
        veterinario: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.revisionesQueries.update(
                fecha = fecha.toString(),
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRevision(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.revisionesQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
