package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Revision
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a las visitas al veterinario.
 *
 * CRUD sencillo en [Dispatchers.IO]: las revisiones son un registro histórico y no participan
 * en ningún cálculo de estado ni de avisos.
 */
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
        veterinario: String?,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.revisionesQueries.insert(
                mascotaId = mascotaId,
                fecha = fecha.toString(),
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario,
                uuid = uuid,
                actualizadoEn = actualizadoEn
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
        veterinario: String?,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.revisionesQueries.update(
                fecha = fecha.toString(),
                motivo = motivo,
                diagnostico = diagnostico,
                notas = notas,
                veterinario = veterinario,
                actualizadoEn = actualizadoEn,
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
