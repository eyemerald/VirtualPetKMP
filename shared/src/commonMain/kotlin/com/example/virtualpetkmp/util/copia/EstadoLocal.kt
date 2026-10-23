package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.db.VirtualPetDatabase

/**
 * Foto del estado local que necesita la fusión para decidir fila a fila.
 *
 * Se lee **una sola vez**, antes de tocar nada: con estas listas se decide qué se añade, qué se
 * actualiza y qué se queda igual. Buscar en la base de datos dentro del bucle costaría un recorrido
 * completo de tabla por fila del paquete.
 */
internal class EstadoLocal(db: VirtualPetDatabase) {

    data class MascotaLocal(
        val id: Long,
        val uuid: String,
        val nombre: String,
        val actualizadoEn: Long
    )

    val mascotasPorUuid: Map<String, MascotaLocal>
    val mascotasPorNombre: Map<String, MascotaLocal>

    /** Ruta de la foto de cada mascota, para no volver a extraerla si ya está en disco. */
    val fotoDeMascota: Map<Long, String?>

    /** `uuid` → `id` local, por tabla. */
    val vacunas: Map<String, Long>
    val preventivos: Map<String, Long>
    val revisiones: Map<String, Long>
    val tratamientos: Map<String, Long>
    val pesos: Map<String, Long>
    val informes: Map<String, Long>
    val notas: Map<String, Long>
    val veterinarios: Map<String, Long>

    /** `uuid` del informe → ruta local de su archivo, para saber si hay que volver a extraerlo. */
    val rutaDeInforme: Map<String, String>

    /** Momentos de modificación locales, por table y uuid (para decidir quién gana). */
    val vacunasEn: Map<String, Long>
    val preventivosEn: Map<String, Long>
    val revisionesEn: Map<String, Long>
    val tratamientosEn: Map<String, Long>
    val pesosEn: Map<String, Long>
    val informesEn: Map<String, Long>
    val notasEn: Map<String, Long>
    val veterinariosEn: Map<String, Long>

    init {
        val mascotas = db.mascotasQueries.selectAll().executeAsList()
        mascotasPorUuid = mascotas.associate { fila ->
            fila.uuid to MascotaLocal(fila.id, fila.uuid, fila.nombre, fila.actualizadoEn)
        }
        mascotasPorNombre = mascotas.associateBy { it.nombre.lowercase() }
            .mapValues { (_, fila) -> MascotaLocal(fila.id, fila.uuid, fila.nombre, fila.actualizadoEn) }
        fotoDeMascota = mascotas.associate { it.id to it.foto }

        val vacunasFilas = db.vacunasQueries.selectAll().executeAsList()
        vacunas = vacunasFilas.associate { it.uuid to it.id }
        vacunasEn = vacunasFilas.associate { it.uuid to it.actualizadoEn }

        val preventivosFilas = db.preventivosQueries.selectAll().executeAsList()
        preventivos = preventivosFilas.associate { it.uuid to it.id }
        preventivosEn = preventivosFilas.associate { it.uuid to it.actualizadoEn }

        val revisionesFilas = db.revisionesQueries.selectAll().executeAsList()
        revisiones = revisionesFilas.associate { it.uuid to it.id }
        revisionesEn = revisionesFilas.associate { it.uuid to it.actualizadoEn }

        val tratamientosFilas = db.tratamientosQueries.selectAll().executeAsList()
        tratamientos = tratamientosFilas.associate { it.uuid to it.id }
        tratamientosEn = tratamientosFilas.associate { it.uuid to it.actualizadoEn }

        val pesosFilas = db.pesosQueries.selectAll().executeAsList()
        pesos = pesosFilas.associate { it.uuid to it.id }
        pesosEn = pesosFilas.associate { it.uuid to it.actualizadoEn }

        val informesFilas = db.informesQueries.selectAll().executeAsList()
        informes = informesFilas.associate { it.uuid to it.id }
        informesEn = informesFilas.associate { it.uuid to it.actualizadoEn }
        rutaDeInforme = informesFilas.associate { it.uuid to it.rutaArchivo }

        val notasFilas = db.notasQueries.selectAll().executeAsList()
        notas = notasFilas.associate { it.uuid to it.id }
        notasEn = notasFilas.associate { it.uuid to it.actualizadoEn }

        val veterinariosFilas = db.veterinariosQueries.selectAll().executeAsList()
        veterinarios = veterinariosFilas.associate { it.uuid to it.id }
        veterinariosEn = veterinariosFilas.associate { it.uuid to it.actualizadoEn }
    }
}

/**
 * Nombre libre a partir de uno que ya existe: "Luna" → "Luna (2)", "Luna (3)"…
 *
 * [usados] se va actualizando para que dos mascotas del mismo paquete que se llamen igual no acaben
 * las dos con el mismo nombre nuevo.
 */
internal fun nombreLibre(nombre: String, usados: MutableSet<String>): String {
    var intento = 2
    while (intento < 1000) {
        val candidato = "$nombre ($intento)"
        if (usados.add(candidato.lowercase())) return candidato
        intento++
    }
    val alternativo = "$nombre (${usados.size + 1})"
    usados.add(alternativo.lowercase())
    return alternativo
}
