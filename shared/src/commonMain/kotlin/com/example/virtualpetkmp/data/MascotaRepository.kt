package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ErrorApp
import com.example.virtualpetkmp.util.ErrorAppException
import com.example.virtualpetkmp.util.Pendientes
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a las mascotas, que son la entidad raíz de la app.
 *
 * Además del CRUD, aquí vive la única regla de negocio de este nivel: **el nombre no se puede
 * repetir** (comparando sin distinguir mayúsculas). La comprobación se hace antes de escribir
 * y excluyendo el propio id al editar, y devuelve el error como `Result.failure` con un
 * **motivo traducible** ([ErrorApp]): el texto final lo pone la interfaz en el idioma activo.
 *
 * Todas las consultas van a [Dispatchers.IO].
 */
class MascotaRepository(private val database: VirtualPetDatabase) {

    suspend fun getAllMascotas(): List<Mascota> = withContext(Dispatchers.IO) {
        database.mascotasQueries.selectAll()
            .executeAsList()
            .map { it.toMascota() }
    }

    /**
     * Ids de las mascotas con alguna **vacuna pendiente** (vencida o con la próxima dosis en los
     * próximos [Pendientes.VENTANA_DIAS] días).
     *
     * Se resuelve en SQL y no recorriendo mascotas en Kotlin: con 40 animales daría igual, pero la
     * lista tiene que seguir abriendo igual de rápida con 500, y esto se llama cada vez que se
     * recarga. La cuenta la hace la base de datos con un `EXISTS` e índice por `(mascotaId, nombre)`.
     *
     * Solo se miran los **últimos** registros de cada vacuna: la misma vacuna puesta cinco veces es
     * una vacuna, no cinco pendientes.
     */
    suspend fun getIdsConVacunaPendiente(hoy: LocalDate): Set<Long> = withContext(Dispatchers.IO) {
        database.mascotasQueries
            .selectIdsConVacunaPendiente(Pendientes.limite(hoy).toString())
            .executeAsList()
            .toSet()
    }

    /** Lo mismo que [getIdsConVacunaPendiente], para preventivos. Es independiente: un animal
     *  puede tener la vacuna al día y la pipeta vencida. */
    suspend fun getIdsConPreventivoPendiente(hoy: LocalDate): Set<Long> = withContext(Dispatchers.IO) {
        database.mascotasQueries
            .selectIdsConPreventivoPendiente(Pendientes.limite(hoy).toString())
            .executeAsList()
            .toSet()
    }

    suspend fun getMascotaById(id: Long): Mascota? = withContext(Dispatchers.IO) {
        database.mascotasQueries.selectById(id)
            .executeAsOneOrNull()
            ?.toMascota()
    }

    /**
     * Alta de una mascota, con su identificador universal.
     *
     * [uuid] y [actualizadoEn] se pueden pasar a mano porque la importación de un paquete tiene que
     * conservar los del archivo original; en el uso normal se generan aquí.
     */
    suspend fun insertMascota(
        mascota: Mascota,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Comprobar si el nombre ya existe (sin distinguir mayúsculas/minúsculas)
            val existente = database.mascotasQueries.checkNombreUnico(mascota.nombre)
                .executeAsList()
                .firstOrNull()

            if (existente != null) {
                return@withContext Result.failure(ErrorAppException(ErrorApp.NOMBRE_DUPLICADO))
            }

            // El alta y la lectura del id van en la MISMA transacción. Fuera de una transacción,
            // sqldelight sobre fichero abre una conexión por sentencia, así que
            // `last_insert_rowid()` se ejecutaba en otra conexión y devolvía 0. Dentro de una
            // transacción la conexión del hilo queda fijada y el id es el de la fila creada.
            val idInsertado = database.transactionWithResult {
                database.mascotasQueries.insert(
                    nombre = mascota.nombre,
                    especie = mascota.especie,
                    raza = mascota.raza,
                    fechaNacimiento = mascota.fechaNacimiento.toString(),
                    sexo = mascota.sexo,
                    color = mascota.color,
                    microchip = mascota.microchip,
                    foto = mascota.foto,
                    peso_ideal = mascota.pesoIdeal,
                    uuid = uuid,
                    actualizadoEn = actualizadoEn
                )

                database.mascotasQueries.getLastInsertedId().executeAsOne()
            }

            Result.success(idInsertado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMascota(
        mascota: Mascota,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (mascota.id == null) {
                return@withContext Result.failure(ErrorAppException(ErrorApp.ID_REQUERIDO))
            }

            // Comprobar si el nombre ya existe (sin distinguir mayúsculas/minúsculas), excluyendo el id actual
            val existente = database.mascotasQueries.checkNombreUnico(mascota.nombre)
                .executeAsList()
                .firstOrNull()

            if (existente != null && existente != mascota.id) {
                return@withContext Result.failure(ErrorAppException(ErrorApp.NOMBRE_DUPLICADO))
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
                actualizadoEn = actualizadoEn,
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
    suspend fun updateFoto(
        id: Long,
        foto: String?,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.mascotasQueries.updateFoto(foto = foto, actualizadoEn = actualizadoEn, id = id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza solo el peso ideal. Se llama desde la hoja de Peso, para poder fijarlo o
     * quitarlo sin abrir el formulario completo de la mascota. `null` lo deja sin configurar.
     */
    suspend fun updatePesoIdeal(
        id: Long,
        pesoIdeal: Double?,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.mascotasQueries.updatePesoIdeal(
                peso_ideal = pesoIdeal,
                actualizadoEn = actualizadoEn,
                id = id
            )
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
        pesoIdeal = peso_ideal,
        uuid = uuid
    )
}