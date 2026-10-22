package com.example.virtualpetkmp.util

/**
 * Operaciones de disco que la copia de seguridad necesita, sin depender de ninguna plataforma.
 *
 * Todas trabajan con **rutas absolutas** y devuelven `false`/`null` en vez de lanzar: un archivo
 * que no se puede leer no debe tumbar la exportación entera, y un paquete que no se puede escribir
 * tiene que acabar en un mensaje, no en una excepción que se escape.
 *
 * En Android y en escritorio se implementan con `java.*`, que es lo mismo en las dos; se declaran
 * como `expect` porque `commonMain` no puede ver `java.*`.
 */
expect fun leerBytes(ruta: String): ByteArray?

expect fun escribirBytes(ruta: String, contenido: ByteArray): Boolean

expect fun existeArchivo(ruta: String): Boolean

expect fun borrarArchivo(ruta: String): Boolean

expect fun borrarCarpeta(ruta: String): Boolean

/** Rutas absolutas de los ficheros que hay directamente en [carpeta] (sin recorrer subcarpetas). */
expect fun listarArchivos(carpeta: String): List<String>

/** Crea la carpeta (y las intermedias) si no existe. */
expect fun crearCarpeta(ruta: String): Boolean

/** Tamaño en bytes de un fichero, o `0` si no existe. */
expect fun tamanoArchivo(ruta: String): Long

/**
 * Nombre del fichero sin la carpeta.
 *
 * Existe porque **la barra no es igual en todas partes**: en Android y en Linux la ruta usa `/`,
 * pero en el escritorio de Windows usa `\`. Un `substringAfterLast('/')` en Windows devuelve la
 * ruta entera, y con eso un filtro por nombre deja de encontrar nada (pasó con la limpieza de
 * copias antiguas).
 */
expect fun nombreDeArchivo(ruta: String): String

/** SHA-256 del contenido, en hexadecimal minúscula. Es la huella que detecta documentos repetidos. */
expect fun sha256(contenido: ByteArray): String

/**
 * Copia un fichero a otro sitio **sin cargarlo entero en memoria**.
 *
 * Hace falta porque en Android el selector del sistema devuelve una ruta `content://`, que no es un
 * fichero: hay que escribirla con el `ContentResolver`, y una copia de seguridad con PDFs puede
 * pesar decenas de megas. Cargarla en un `ByteArray` para escribirla sería la forma más fácil de
 * quedarse sin memoria justo al guardar los datos de alguien.
 */
expect fun copiarArchivoA(destino: String, origen: String): Boolean

// --- ZIP ---------------------------------------------------------------------------------------

/**
 * Escribe un ZIP con dos tipos de contenido:
 *
 * - [textos]: entradas cuyo contenido ya está en memoria (`manifiesto.json`, `datos.json`).
 * - [archivos]: nombre dentro del ZIP → ruta local. Se copian **en streaming**, sin cargar el
 *   fichero entero en memoria: una carpeta con PDFs de varios megas no debe reventar la app.
 *
 * Devuelve `false` si no se pudo escribir.
 */
expect fun escribirZip(destino: String, textos: Map<String, String>, archivos: Map<String, String>): Boolean

/** Nombres de todas las entradas del ZIP, o lista vacía si el fichero no es un ZIP legible. */
expect fun entradasDeZip(paquete: String): List<String>

/** Lee una entrada de texto del ZIP, o `null` si no está. */
expect fun leerTextoDeZip(paquete: String, entrada: String): String?

/** Extrae una entrada del ZIP a [destino] (en streaming). Devuelve si se pudo. */
expect fun extraerDeZip(paquete: String, entrada: String, destino: String): Boolean

/** Tamaño sin comprimir de una entrada del ZIP, o `-1` si no está. */
expect fun tamanoDeEntradaZip(paquete: String, entrada: String): Long

/** SHA-256 del contenido de una entrada del ZIP, sin extraerla a disco. `null` si no está. */
expect fun sha256DeEntradaZip(paquete: String, entrada: String): String?
