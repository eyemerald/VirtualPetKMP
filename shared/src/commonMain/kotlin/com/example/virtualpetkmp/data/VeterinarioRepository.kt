package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VeterinarioRepository(private val database: VirtualPetDatabase) {

    suspend fun getAllVeterinarios(): List<Veterinario> = withContext(Dispatchers.IO) {
        database.veterinariosQueries.selectAll()
            .executeAsList()
            .map { it.toVeterinario() }
    }

    suspend fun getVeterinarioById(id: Long): Veterinario? = withContext(Dispatchers.IO) {
        database.veterinariosQueries.selectById(id)
            .executeAsOneOrNull()
            ?.toVeterinario()
    }

    suspend fun getVeterinariosUrgencias(): List<Veterinario> = withContext(Dispatchers.IO) {
        database.veterinariosQueries.selectUrgencias()
            .executeAsList()
            .map { it.toVeterinario() }
    }

    suspend fun insertVeterinario(veterinario: Veterinario): Result<Long> = withContext(Dispatchers.IO) {
        try {
            database.veterinariosQueries.insert(
                nombreClinica = veterinario.nombreClinica,
                nombreVeterinario = veterinario.nombreVeterinario,
                telefono = veterinario.telefono,
                direccion = veterinario.direccion,
                esUrgencias = if (veterinario.esUrgencias) 1L else 0L
            )

            val idInsertado = database.veterinariosQueries.getLastInsertedId()
                .executeAsOne()

            Result.success(idInsertado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVeterinario(veterinario: Veterinario): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (veterinario.id == null) {
                return@withContext Result.failure(Exception("ID es requerido para actualizar"))
            }

            database.veterinariosQueries.update(
                nombreClinica = veterinario.nombreClinica,
                nombreVeterinario = veterinario.nombreVeterinario,
                telefono = veterinario.telefono,
                direccion = veterinario.direccion,
                esUrgencias = if (veterinario.esUrgencias) 1L else 0L,
                id = veterinario.id
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVeterinario(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.veterinariosQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun com.example.virtualpetkmp.db.Veterinarios.toVeterinario(): Veterinario {
    return Veterinario(
        id = id,
        nombreClinica = nombreClinica,
        nombreVeterinario = nombreVeterinario,
        telefono = telefono,
        direccion = direccion,
        esUrgencias = esUrgencias == 1L
    )
}
