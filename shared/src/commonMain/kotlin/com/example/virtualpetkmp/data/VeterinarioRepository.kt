package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Veterinario
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ErrorApp
import com.example.virtualpetkmp.util.ErrorAppException
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Acceso a la agenda de clínicas veterinarias.
 *
 * Es independiente de las mascotas: no hay relación entre ambas tablas. La consulta principal
 * ya devuelve las clínicas ordenadas con las de urgencias primero.
 */
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

    suspend fun insertVeterinario(
        veterinario: Veterinario,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            database.veterinariosQueries.insert(
                nombreClinica = veterinario.nombreClinica,
                nombreVeterinario = veterinario.nombreVeterinario,
                telefono = veterinario.telefono,
                direccion = veterinario.direccion,
                esUrgencias = if (veterinario.esUrgencias) 1L else 0L,
                uuid = uuid,
                actualizadoEn = actualizadoEn
            )

            val idInsertado = database.veterinariosQueries.getLastInsertedId()
                .executeAsOne()

            Result.success(idInsertado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVeterinario(
        veterinario: Veterinario,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (veterinario.id == null) {
                return@withContext Result.failure(ErrorAppException(ErrorApp.ID_REQUERIDO))
            }

            database.veterinariosQueries.update(
                nombreClinica = veterinario.nombreClinica,
                nombreVeterinario = veterinario.nombreVeterinario,
                telefono = veterinario.telefono,
                direccion = veterinario.direccion,
                esUrgencias = if (veterinario.esUrgencias) 1L else 0L,
                actualizadoEn = actualizadoEn,
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
