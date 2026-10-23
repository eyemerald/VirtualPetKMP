package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.util.entradasDeZip
import com.example.virtualpetkmp.util.existeArchivo
import com.example.virtualpetkmp.util.leerTextoDeZip
import com.example.virtualpetkmp.util.sha256

/**
 * Abre un paquete `.vpet` y comprueba que es nuestro y que está entero.
 *
 * Está aparte de los importadores porque **lo usan los dos**: el que reemplaza todo (restaurar una
 * copia) y el que fusiona. Si cada uno validara a su manera, uno de los dos podría aceptar un
 * paquete que el otro rechaza, y ese es justo el tipo de diferencia que se descubre tarde.
 *
 * Los fallos se clasifican ([ErrorCopia]) en vez de devolver texto: el mensaje final lo pone la
 * interfaz, en el idioma del usuario.
 *
 * La huella SHA-256 del manifiesto es lo que detecta un paquete a medio copiar: un ZIP cortado se
 * puede abrir, pero el `datos.json` ya no cuadra con la huella.
 */
object LectorPaquete {

    fun leer(paquete: String): PaqueteDatos {
        if (!existeArchivo(paquete)) throw ErrorCopiaException(ErrorCopia.PAQUETE_ILEGIBLE)

        val entradas = entradasDeZip(paquete)
        val textoDatos = if (entradas.isEmpty()) {
            null
        } else {
            leerTextoDeZip(paquete, PaqueteDatos.ENTRADA_DATOS)
        }
        if (textoDatos == null) {
            throw ErrorCopiaException(
                if (entradas.isEmpty()) ErrorCopia.PAQUETE_ILEGIBLE else ErrorCopia.DATOS_INCOMPLETOS
            )
        }

        val textoManifiesto = leerTextoDeZip(paquete, PaqueteDatos.ENTRADA_MANIFIESTO)
        if (textoManifiesto != null) {
            val manifiesto = runCatching {
                PaqueteDatos.json.decodeFromString(ManifiestoPaquete.serializer(), textoManifiesto)
            }.getOrElse { throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS) }

            if (manifiesto.formato != PaqueteDatos.FORMATO) {
                throw ErrorCopiaException(ErrorCopia.NO_ES_DE_VIRTUALPET)
            }
            if (manifiesto.versionFormato > PaqueteDatos.VERSION_FORMATO) {
                throw ErrorCopiaException(ErrorCopia.FORMATO_DEMASIADO_NUEVO)
            }
            if (manifiesto.hashDatos.isNotBlank() &&
                sha256(textoDatos.toByteArray(Charsets.UTF_8)) != manifiesto.hashDatos
            ) {
                throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS)
            }
        }

        val datos = runCatching {
            PaqueteDatos.json.decodeFromString(PaqueteDatos.serializer(), textoDatos)
        }.getOrElse { throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS) }

        if (datos.formato != PaqueteDatos.FORMATO) throw ErrorCopiaException(ErrorCopia.NO_ES_DE_VIRTUALPET)
        if (datos.versionFormato > PaqueteDatos.VERSION_FORMATO) {
            throw ErrorCopiaException(ErrorCopia.FORMATO_DEMASIADO_NUEVO)
        }
        if (datos.mascotas.isEmpty() && datos.veterinarios.isEmpty()) {
            throw ErrorCopiaException(ErrorCopia.DATOS_INCOMPLETOS)
        }

        return datos
    }

    /** Cuenta lo que trae un paquete, sin escribir nada. Es lo que se enseña antes de importar. */
    fun vistaPrevia(paquete: String, datos: PaqueteDatos = leer(paquete)): VistaPreviaPaquete {
        val adjuntos = entradasDeZip(paquete).filter { it.startsWith(PaqueteDatos.CARPETA_ARCHIVOS) }
        return VistaPreviaPaquete(
            mascotas = datos.mascotas.size,
            vacunas = datos.vacunas.size,
            preventivos = datos.preventivos.size,
            revisiones = datos.revisiones.size,
            tratamientos = datos.tratamientos.size,
            pesos = datos.pesos.size,
            informes = datos.informes.size,
            notas = datos.notas.size,
            veterinarios = datos.veterinarios.size,
            archivos = adjuntos.size,
            bytesArchivos = adjuntos.sumOf { tamanoDeEntrada(paquete, it) },
            nombresMascotas = datos.mascotas.map { it.nombre },
            versionApp = datos.versionApp,
            creadoEn = datos.creadoEn
        )
    }

    private fun tamanoDeEntrada(paquete: String, entrada: String): Long =
        com.example.virtualpetkmp.util.tamanoDeEntradaZip(paquete, entrada).coerceAtLeast(0)
}
