package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.db.VirtualPetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class InformeRepository(private val database: VirtualPetDatabase) {

    suspend fun getInformesByMascotaId(mascotaId: Long): List<Informe> = withContext(Dispatchers.IO) {
        database.informesQueries.selectByMascotaId(mascotaId)
            .executeAsList()
            .map { fila ->
                Informe(
                    id = fila.id,
                    mascotaId = fila.mascotaId,
                    tipo = fila.tipo,
                    descripcion = fila.descripcion,
                    fecha = LocalDate.parse(fila.fecha),
                    nombreArchivo = fila.nombreArchivo,
                    rutaArchivo = fila.rutaArchivo
                )
            }
    }

    suspend fun insertInforme(
        mascotaId: Long,
        tipo: String,
        descripcion: String?,
        fecha: LocalDate,
        nombreArchivo: String,
        rutaArchivo: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.informesQueries.insert(
                mascotaId = mascotaId,
                tipo = tipo,
                descripcion = descripcion,
                fecha = fecha.toString(),
                nombreArchivo = nombreArchivo,
                rutaArchivo = rutaArchivo
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInforme(
        id: Long,
        tipo: String,
        descripcion: String?,
        fecha: LocalDate
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.informesQueries.update(
                tipo = tipo,
                descripcion = descripcion,
                fecha = fecha.toString(),
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInforme(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.informesQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
