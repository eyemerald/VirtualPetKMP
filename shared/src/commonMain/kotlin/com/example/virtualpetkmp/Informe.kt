package com.example.virtualpetkmp

import kotlinx.datetime.LocalDate

/**
 * Un informe veterinario adjunto (analítica, radiografía, informe de cirugía...).
 *
 * Lo que se guarda en la base de datos es la **ruta** del archivo, no el archivo: el
 * documento vive en el almacenamiento del dispositivo y la app solo lo referencia y lo abre
 * con la aplicación que corresponda. Por eso el informe puede quedar "roto" si el usuario
 * mueve o borra el archivo por fuera de la app, y las pantallas contemplan ese caso
 * mostrando un error en vez de fallar.
 *
 * @param tipo categoría elegida por el usuario.
 * @param nombreArchivo nombre original, para mostrarlo sin depender de la ruta.
 * @param rutaArchivo ruta absoluta del archivo en el dispositivo.
 */
data class Informe(
    val id: Long? = null,
    val mascotaId: Long,
    val tipo: String,
    val descripcion: String? = null,
    val fecha: LocalDate,
    val nombreArchivo: String,
    val rutaArchivo: String
)
