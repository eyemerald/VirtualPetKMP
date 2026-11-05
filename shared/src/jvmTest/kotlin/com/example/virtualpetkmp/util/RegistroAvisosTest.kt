package com.example.virtualpetkmp.util

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El registro de lo que hay programado tiene que poder leerse tal cual se escribió.
 *
 * Se prueba sobre una carpeta temporal y no sobre la de la app: en jvmTest no existe
 * `carpetaDatosApp()`, y el formato (una línea por aviso) es justo lo que hay que fijar.
 */
class RegistroAvisosTest {

    private lateinit var carpeta: File

    @BeforeTest
    fun setup() {
        carpeta = Files.createTempDirectory("virtualpet-registro-avisos").toFile()
    }

    @AfterTest
    fun tearDown() {
        carpeta.deleteRecursively()
    }

    private fun avisoTratamiento(id: Long, uuid: String, hora: String) =
        AvisoProgramado(id, TipoAviso.TRATAMIENTO, uuid, hora)

    @Test
    fun `lo escrito se lee igual`() {
        val avisos = listOf(
            avisoTratamiento(2_000_000_007L, "u-tratamiento", "08:00"),
            AvisoProgramado(1_000_000_003L, TipoAviso.PREVENTIVO, "u-preventivo")
        )

        RegistroAvisosEnFichero.escribir(avisos, carpeta.absolutePath)

        assertEquals(avisos, RegistroAvisosEnFichero.leer(carpeta.absolutePath))
    }

    @Test
    fun `escribir una lista vacia deja el registro limpio`() {
        RegistroAvisosEnFichero.escribir(
            listOf(avisoTratamiento(1L, "u", "08:00")),
            carpeta.absolutePath
        )

        RegistroAvisosEnFichero.escribir(emptyList(), carpeta.absolutePath)

        assertTrue(RegistroAvisosEnFichero.leer(carpeta.absolutePath).isEmpty())
    }

    @Test
    fun `una linea rota no impide leer el resto`() {
        File(carpeta, "avisos_programados.txt").writeText(
            """
            esto no es una linea valida
            2|TRATAMIENTO|u-buena|08:00
            3|TIPO_QUE_NO_EXISTE|u-mala|09:00
            4|TRATAMIENTO||09:00
            """.trimIndent(),
            Charsets.UTF_8
        )

        val leidos = RegistroAvisosEnFichero.leer(carpeta.absolutePath)

        assertEquals(
            listOf(avisoTratamiento(2L, "u-buena", "08:00")),
            leidos,
            "solo la línea completa se puede usar: las demás se descartan"
        )
    }

    @Test
    fun `apuntar sustituye la entrada del mismo id y quitar la borra`() {
        val registro = RegistroAvisosEnMemoria(
            listOf(avisoTratamiento(10L, "u", "08:00"))
        )

        // El mismo aviso, reprogramado a otra hora: no puede quedar duplicado.
        registro.apuntar(avisoTratamiento(10L, "u", "09:00"))
        assertEquals(listOf(avisoTratamiento(10L, "u", "09:00")), registro.actuales)

        registro.quitar(10L)
        assertTrue(registro.actuales.isEmpty())
    }
}
