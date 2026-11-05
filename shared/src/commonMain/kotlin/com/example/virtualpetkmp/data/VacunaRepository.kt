package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Vacuna
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a las vacunas.
 *
 * Devuelve modelos de dominio ([Vacuna]) ya convertidos, no filas de SQLDelight, y hace todas
 * las consultas en [Dispatchers.IO] para no bloquear la interfaz. Las escrituras devuelven
 * `Result` en lugar de lanzar: la app es offline y un fallo de escritura se comunica al
 * usuario, no se propaga como excepción.
 *
 * Tras cualquier alta, edición o borrado, quien llama es responsable de volver a leer la
 * lista (y de reprogramar los avisos, cosa que hace el `FichaMascotaViewModel`).
 */
class VacunaRepository(private val database: VirtualPetDatabase) {

    suspend fun getVacunasByMascotaId(mascotaId: Long): List<Vacuna> = withContext(Dispatchers.IO) {
        database.vacunasQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Vacuna(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    nombre = fila.nombre,
                    fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                    fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                    veterinario = fila.veterinario,
                    lote = fila.lote,
                    uuid = fila.uuid
                )
            }
    }

    suspend fun getAllVacunas(): List<Vacuna> = withContext(Dispatchers.IO) {
        database.vacunasQueries.selectAll()
            .executeAsList()
            .map { fila ->
                Vacuna(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    nombre = fila.nombre,
                    fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                    fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                    veterinario = fila.veterinario,
                    lote = fila.lote,
                    uuid = fila.uuid
                )
            }
    }

    /**
     * Una vacuna por su identificador universal.
     *
     * La consulta el aviso **al sonar**: el id local del intent ya no identifica la fila (al
     * restaurar una copia se reinserta con otro id), el `uuid` sí.
     */
    suspend fun getVacunaPorUuid(uuid: String): Vacuna? = withContext(Dispatchers.IO) {
        database.vacunasQueries.selectByUuid(uuid).executeAsList().firstOrNull()?.let { fila ->
            Vacuna(
                id = fila.id,
                mascotaId = fila.mascotaId,
                nombre = fila.nombre,
                fechaAplicacion = LocalDate.parse(fila.fechaAplicacion),
                fechaProximaDosis = LocalDate.parse(fila.fechaProximaDosis),
                veterinario = fila.veterinario,
                lote = fila.lote,
                uuid = fila.uuid
            )
        }
    }

    /**
     * Alta de una vacuna.
     *
     * [uuid] y [actualizadoEn] se pueden pasar a mano porque la importación de un paquete tiene
     * que conservar los del archivo original; en el uso normal se generan aquí.
     */
    suspend fun insertVacuna(
        mascotaId: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Alta + lectura del id en una sola transacción: así la conexión del hilo queda fijada
            // y `last_insert_rowid()` es el de la fila recién creada (fuera de transacción, sobre
            // fichero, cada sentencia va por una conexión distinta y devolvía 0).
            val idInsertado = database.transactionWithResult {
                database.vacunasQueries.insert(
                    mascotaId = mascotaId,
                    nombre = nombre,
                    fechaAplicacion = fechaAplicacion.toString(),
                    fechaProximaDosis = fechaProximaDosis.toString(),
                    veterinario = veterinario,
                    lote = lote,
                    uuid = uuid,
                    actualizadoEn = actualizadoEn
                )

                database.vacunasQueries.getLastInsertedId().executeAsOne()
            }

            Result.success(idInsertado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVacuna(
        id: Long,
        nombre: String,
        fechaAplicacion: LocalDate,
        fechaProximaDosis: LocalDate,
        veterinario: String?,
        lote: String?,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.vacunasQueries.update(
                nombre = nombre,
                fechaAplicacion = fechaAplicacion.toString(),
                fechaProximaDosis = fechaProximaDosis.toString(),
                veterinario = veterinario,
                lote = lote,
                actualizadoEn = actualizadoEn,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVacuna(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.vacunasQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
