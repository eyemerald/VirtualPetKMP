package com.example.virtualpetkmp.util.copia

import com.example.virtualpetkmp.Tratamiento
import com.example.virtualpetkmp.util.HorasDeToma
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los tratamientos dentro de un `.vpet`: que un paquete de la 1.13 se pueda abrir con la 1.14 y
 * que uno escrito por una versión futura no rompa nada.
 *
 * Es la prueba de compatibilidad del formato: el paquete lo mueve el usuario a mano entre el móvil
 * y el ordenador, así que la versión del que escribe y la del que lee **no tienen por qué
 * coincidir**.
 */
class PaqueteTratamientosTest {

    private val json = PaqueteDatos.json

    @Test
    fun `un paquete de antes de la 1_14 se lee sin las columnas nuevas`() {
        // Exactamente lo que escribe la 1.13: sin vecesAlDia ni horas.
        val viejo = """
            {
              "formato": "virtualpet-datos",
              "versionFormato": 1,
              "tratamientos": [
                {
                  "uuid": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
                  "actualizadoEn": 1,
                  "mascotaUuid": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
                  "nombreMedicamento": "Oticoear",
                  "dosis": "4 gotas",
                  "frecuencia": "Cada 12 horas",
                  "fechaInicio": "2026-01-01",
                  "fechaFin": "2026-02-01",
                  "hora": "08:30",
                  "notificar": 1
                }
              ]
            }
        """.trimIndent()

        val datos = json.decodeFromString<PaqueteDatos>(viejo)

        val fila = datos.tratamientos.single()
        assertEquals("Oticoear", fila.nombreMedicamento)
        assertEquals("08:30", fila.hora)
        assertNull(fila.vecesAlDia, "un paquete antiguo no puede inventarse las tomas")
        assertNull(fila.horas)
    }

    @Test
    fun `un paquete con las columnas nuevas las conserva`() {
        val actual = """
            {
              "formato": "virtualpet-datos",
              "versionFormato": 1,
              "tratamientos": [
                {
                  "uuid": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
                  "mascotaUuid": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
                  "nombreMedicamento": "Amoxicilina",
                  "fechaInicio": "2026-01-01",
                  "hora": "08:00",
                  "notificar": 1,
                  "vecesAlDia": 3,
                  "horas": "08:00,14:00,22:00"
                }
              ]
            }
        """.trimIndent()

        val datos = json.decodeFromString<PaqueteDatos>(actual)

        val fila = datos.tratamientos.single()
        assertEquals(3, fila.vecesAlDia)
        assertEquals("08:00,14:00,22:00", fila.horas)
    }

    @Test
    fun `un campo que no conocemos no rompe la lectura del paquete`() {
        // Lo escribira una version futura: hoy no hay que entenderlo, solo no reventar.
        val futuro = """
            {
              "formato": "virtualpet-datos",
              "versionFormato": 1,
              "campoQueNoExisteTodavia": { "a": 1 },
              "tratamientos": [
                {
                  "uuid": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
                  "mascotaUuid": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
                  "nombreMedicamento": "Metacam",
                  "fechaInicio": "2026-01-01",
                  "notificar": 0,
                  "campoFuturoDelTratamiento": "lo que sea"
                }
              ]
            }
        """.trimIndent()

        val datos = json.decodeFromString<PaqueteDatos>(futuro)

        assertEquals("Metacam", datos.tratamientos.single().nombreMedicamento)
    }

    @Test
    fun `lo que se exporta se puede volver a leer igual`() {
        val original = PaqueteDatos(
            tratamientos = listOf(
                FilaTratamiento(
                    uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
                    mascotaUuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
                    nombreMedicamento = "Amoxicilina",
                    dosis = "1 comprimido",
                    fechaInicio = "2026-01-01",
                    hora = "08:00",
                    notificar = 1,
                    vecesAlDia = 2,
                    horas = "08:00,20:00"
                )
            )
        )

        val texto = json.encodeToString(PaqueteDatos.serializer(), original)
        val leido = json.decodeFromString<PaqueteDatos>(texto)

        assertEquals(original.tratamientos, leido.tratamientos)
        assertTrue(texto.contains("\"horas\""), "las horas tienen que quedar escritas en el paquete")
    }

    @Test
    fun `el modelo de la app y la fila del paquete llevan las mismas horas`() {
        // El puente entre los dos lo hace el exportador; aqui se comprueba que el dato no se
        // pierde al pasar de "3 tomas" a texto, que es donde se puede colar un despiste.
        val tratamiento = Tratamiento(
            id = 1L,
            mascotaId = 1L,
            nombreMedicamento = "Amoxicilina",
            fechaInicio = LocalDate(2026, 1, 1),
            hora = "08:00",
            notificar = true,
            vecesAlDia = 3,
            horas = "08:00,14:00,22:00"
        )

        val fila = FilaTratamiento(
            uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1",
            mascotaUuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1",
            nombreMedicamento = tratamiento.nombreMedicamento,
            dosis = tratamiento.dosis,
            frecuencia = tratamiento.frecuencia,
            fechaInicio = tratamiento.fechaInicio.toString(),
            fechaFin = tratamiento.fechaFin?.toString(),
            hora = tratamiento.hora,
            notificar = if (tratamiento.notificar) 1 else 0,
            vecesAlDia = tratamiento.vecesAlDia,
            horas = tratamiento.horas
        )

        assertEquals(
            listOf("08:00", "14:00", "22:00"),
            HorasDeToma.leerHoras(fila.horas)
        )
    }
}
