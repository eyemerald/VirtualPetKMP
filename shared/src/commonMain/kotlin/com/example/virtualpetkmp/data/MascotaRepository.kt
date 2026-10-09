package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a las mascotas, que son la entidad raíz de la app.
 *
 * Además del CRUD, aquí vive la única regla de negocio de este nivel: **el nombre no se puede
 * repetir** (comparando sin distinguir mayúsculas). La comprobación se hace antes de escribir
 * y excluyendo el propio id al editar, y devuelve el error como `Result.failure` con un
 * mensaje ya listo para enseñar.
 *
 * Todas las consultas van a [Dispatchers.IO].
 */
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
                microchip = mascota.microchip,
                foto = mascota.foto,
                peso_ideal = mascota.pesoIdeal
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
                foto = mascota.foto,
                peso_ideal = mascota.pesoIdeal,
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

    /**
     * Actualiza solo la foto de una mascota. Se usa desde la ficha para no tener que
     * reenviar (ni volver a validar) el resto de sus datos.
     */
    suspend fun updateFoto(id: Long, foto: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.mascotasQueries.updateFoto(foto = foto, id = id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza solo el peso ideal. Se llama desde la hoja de Peso, para poder fijarlo o
     * quitarlo sin abrir el formulario completo de la mascota. `null` lo deja sin configurar.
     */
    suspend fun updatePesoIdeal(id: Long, pesoIdeal: Double?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.mascotasQueries.updatePesoIdeal(peso_ideal = pesoIdeal, id = id)
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
        microchip = microchip,
        foto = foto,
        pesoIdeal = peso_ideal
    )
}