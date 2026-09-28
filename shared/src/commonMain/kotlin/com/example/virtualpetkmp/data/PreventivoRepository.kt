package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Preventivo
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class PreventivoRepository(private val database: VirtualPetDatabase) {

    suspend fun getPreventivosByMascotaId(mascotaId: Long): List<Preventivo> = withContext(Dispatchers.IO) {
        database.preventivosQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Preventivo(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    tipo = fila.tipo,
                    nombre = fila.nombre,
                    fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                    fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                    veterinario = fila.veterinario,
                    lote = fila.lote
                )
            }
    }

    suspend fun getPreventivosByMascotaIdAndTipo(mascotaId: Long, tipo: String): List<Preventivo> = withContext(Dispatchers.IO) {
        database.preventivosQueries.selectByMascotaIdAndTipo(mascotaId, tipo)
            .executeAsList()
            .map { fila ->
                Preventivo(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    tipo = fila.tipo,
                    nombre = fila.nombre,
                    fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                    fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                    veterinario = fila.veterinario,
                    lote = fila.lote
                )
            }
    }

    suspend fun insertPreventivo(
        mascotaId: Long,
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.preventivosQueries.insert(
                mascotaId = mascotaId,
                tipo = tipo,
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

    suspend fun updatePreventivo(
        id: Long,
        tipo: String,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.preventivosQueries.update(
                tipo = tipo,
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

    suspend fun deletePreventivo(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.preventivosQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
