package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.Informe
import com.example.virtualpetkmp.db.VirtualPetDatabase
import com.example.virtualpetkmp.util.ahoraMillis
import com.example.virtualpetkmp.util.nuevoIdentificadorUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Acceso a los informes veterinarios adjuntos.
 *
 * Guarda y devuelve **rutas** de archivo, no los archivos: el documento sigue viviendo en el
 * almacenamiento del dispositivo. Consecuencia práctica: al borrar un informe se elimina la
 * fila, pero el archivo se deja donde está (no es responsabilidad de la app borrar documentos
 * del usuario).
 */
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
        rutaArchivo: String,
        uuid: String = nuevoIdentificadorUniversal(),
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.informesQueries.insert(
                mascotaId = mascotaId,
                tipo = tipo,
                descripcion = descripcion,
                fecha = fecha.toString(),
                nombreArchivo = nombreArchivo,
                rutaArchivo = rutaArchivo,
                uuid = uuid,
                actualizadoEn = actualizadoEn
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
        fecha: LocalDate,
        actualizadoEn: Long = ahoraMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.informesQueries.update(
                tipo = tipo,
                descripcion = descripcion,
                fecha = fecha.toString(),
                actualizadoEn = actualizadoEn,
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
