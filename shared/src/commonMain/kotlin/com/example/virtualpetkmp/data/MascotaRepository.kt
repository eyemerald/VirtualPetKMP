package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class MascotaRepository(private val database: VirtualPetDatabase) {

    suspend fun getAllMascotas(): List<Mascota> = withContext(Dispatchers.IO) {
        database.mascotasQueries.selectAll()
            .executeAsList()
            .map { it.toMascota() }
    }

    suspend fun getMascotaById(id: Long): Mascota? = withContext(Dispatchers.IO) {
        database.mascotasQueries.selectById(id)
            .executeAsOneOrNull()
            ?.toMascota()
    }

    suspend fun insertMascota(mascota: Mascota): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Comprobar si el nombre ya existe (sin distinguir mayúsculas/minúsculas)
            val existente = database.mascotasQueries.checkNombreUnico(mascota.nombre)
                .executeAsList()
                .firstOrNull()

            if (existente != null) {
                return@withContext Result.failure(Exception("Ya existe una mascota con ese nombre"))
            }

            database.mascotasQueries.insert(
                nombre = mascota.nombre,
                especie = mascota.especie,
                raza = mascota.raza,
                fechaNacimiento = mascota.fechaNacimiento.toString(),
                sexo = mascota.sexo,
                color = mascota.color,
                microchip = mascota.microchip
            )

            // Obtener el ID insertado usando last_insert_rowid()
            val idInsertado = database.mascotasQueries.getLastInsertedId()
                .executeAsOne()

            Result.success(idInsertado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMascota(mascota: Mascota): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (mascota.id == null) {
                return@withContext Result.failure(Exception("ID es requerido para actualizar"))
            }

            // Comprobar si el nombre ya existe (sin distinguir mayúsculas/minúsculas), excluyendo el id actual
            val existente = database.mascotasQueries.checkNombreUnico(mascota.nombre)
                .executeAsList()
                .firstOrNull()

            if (existente != null && existente != mascota.id) {
                return@withContext Result.failure(Exception("Ya existe una mascota con ese nombre"))
            }

            database.mascotasQueries.update(
                nombre = mascota.nombre,
                especie = mascota.especie,
                raza = mascota.raza,
                fechaNacimiento = mascota.fechaNacimiento.toString(),
                sexo = mascota.sexo,
                color = mascota.color,
                microchip = mascota.microchip,
                id = mascota.id
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMascota(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.mascotasQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun com.example.virtualpetkmp.db.Mascotas.toMascota(): Mascota {
    return Mascota(
        id = id,
        nombre = nombre,
        especie = especie,
        raza = raza,
        fechaNacimiento = LocalDate.parse(fechaNacimiento),
        sexo = sexo,
        color = color,
        microchip = microchip
    )
}