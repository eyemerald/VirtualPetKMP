package com.example.virtualpetkmp.util

/**
 * Recuerda si la lista de mascotas se estaba viendo como tarjetas o como listado compacto.
 *
 * **Por qué no basta con `rememberSaveable`.** Ese estado sobrevive a un giro de pantalla y a que el
 * sistema recree la actividad, pero **no** a cerrar la app: al volver a abrirla, la lista aparecía
 * otra vez en tarjetas. Quien tiene cuarenta animales pone el listado justo para no tener que
 * desplazarse, así que se guarda en disco y se lee al arrancar.
 *
 * **Formato:** un fichero de texto de una línea, `tarjetas` o `listado`, al lado de los datos. Se
 * hace con texto plano por lo mismo que las posposiciones: es una preferencia de la interfaz, no un
 * dato del usuario, y así no hace falta tocar el esquema.
 *
 * Si el fichero no existe o no se entiende, la vista por defecto son las tarjetas (lo de siempre):
 * nunca se rompe la pantalla por una preferencia ilegible.
 */
object PreferenciaVista {

    private const val FICHERO = "vista.txt"
    private const val LISTADO = "listado"
    private const val TARJETAS = "tarjetas"

    /** `true` si la última vez se dejó en listado compacto. */
    fun leerListado(carpeta: String = carpetaDatosApp()): Boolean =
        leerBytes("$carpeta/$FICHERO")?.toString(Charsets.UTF_8)?.trim() == LISTADO

    fun guardarListado(listado: Boolean, carpeta: String = carpetaDatosApp()) {
        crearCarpeta(carpeta)
        val texto = if (listado) LISTADO else TARJETAS
        if (!escribirBytes("$carpeta/$FICHERO", texto.toByteArray(Charsets.UTF_8))) {
            println("[PreferenciaVista] no se pudo guardar en $carpeta")
        }
    }
}
