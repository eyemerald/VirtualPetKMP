package com.example.virtualpetkmp.util

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La vista de la lista (tarjetas o listado) tiene que **seguir puesta al cerrar y abrir la app**.
 *
 * `rememberSaveable` no llega: sobrevive a girar el móvil, pero no a cerrar la app. Quien tiene
 * muchos animales pone el listado compacto para no desplazarse y no quiere volver a ponerlo cada vez.
 */
class PreferenciaVistaTest {

    private lateinit var carpeta: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-vista").toFile()
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    @Test
    fun `sin fichero se empieza en tarjetas`() {
        assertFalse(
            PreferenciaVista.leerListado(carpeta.absolutePath),
            "si nunca se ha elegido nada, la vista de siempre son las tarjetas"
        )
    }

    @Test
    fun `lo guardado como listado se lee como listado`() {
        PreferenciaVista.guardarListado(true, carpeta.absolutePath)

        assertTrue(PreferenciaVista.leerListado(carpeta.absolutePath))
    }

    @Test
    fun `volver a tarjetas se guarda tambien`() {
        PreferenciaVista.guardarListado(true, carpeta.absolutePath)
        PreferenciaVista.guardarListado(false, carpeta.absolutePath)

        assertFalse(
            PreferenciaVista.leerListado(carpeta.absolutePath),
            "volver a tarjetas tiene que quedar guardado, no solo el cambio a listado"
        )
    }

    @Test
    fun `un fichero ilegible no rompe la pantalla`() {
        File(carpeta, "vista.txt").writeText("cualquier cosa")

        assertFalse(
            PreferenciaVista.leerListado(carpeta.absolutePath),
            "una preferencia corrupta cae en tarjetas en vez de fallar"
        )
    }

    @Test
    fun `se crea la carpeta si no existe`() {
        val inexistente = File(carpeta, "datos").absolutePath
        PreferenciaVista.guardarListado(true, inexistente)

        assertTrue(PreferenciaVista.leerListado(inexistente))
    }
}
