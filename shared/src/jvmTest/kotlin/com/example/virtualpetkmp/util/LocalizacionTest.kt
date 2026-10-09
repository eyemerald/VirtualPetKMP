package com.example.virtualpetkmp.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Reglas de configuración regional: qué unidad, qué orden de fecha y qué separador decimal
 * toca en cada dispositivo, y cómo se convierte el peso de kilos a la unidad mostrada.
 *
 * Merece la pena tenerlas cubiertas porque son decisiones que afectan a **datos que se
 * guardan**: si el factor de conversión estuviera mal, un usuario estadounidense guardaría
 * pesos equivocados sin que nada avisara.
 */
class LocalizacionTest {

    @Test
    fun `solo Estados Unidos usa libras`() {
        // Decisión tomada: en Reino Unido se usan kilogramos para el peso de un animal.
        assertEquals(UnidadPeso.LIBRAS, Localizacion.desdeRegion("en-US").unidadPeso)
        assertEquals(UnidadPeso.KILOS, Localizacion.desdeRegion("en_GB").unidadPeso)
    }

    @Test
    fun `los demas idiomas usan kilos`() {
        listOf("es-ES", "es-MX", "pt-BR", "pt-PT", "fr-FR", "de-DE", "it-IT").forEach { region ->
            assertEquals(UnidadPeso.KILOS, Localizacion.desdeRegion(region).unidadPeso, "región $region")
        }
    }

    @Test
    fun `el orden de fecha es mes-dia solo en ingles`() {
        assertEquals(OrdenFecha.MES_DIA_ANIO, Localizacion.desdeRegion("en-US").ordenFechaMostrada)
        assertEquals(OrdenFecha.MES_DIA_ANIO, Localizacion.desdeRegion("en-GB").ordenFechaMostrada)
        listOf("es-ES", "pt-BR", "fr-FR", "de-DE", "it-IT").forEach { region ->
            assertEquals(
                OrdenFecha.DIA_MES_ANIO,
                Localizacion.desdeRegion(region).ordenFechaMostrada,
                "región $region"
            )
        }
    }

    @Test
    fun `un idioma desconocido cae en espanol y kilos`() {
        val configuracion = Localizacion.desdeRegion("ja-JP")
        assertEquals(Idioma.ESPANOL, configuracion.idioma)
        assertEquals(UnidadPeso.KILOS, configuracion.unidadPeso)

        // Y una etiqueta ausente tampoco debe romper nada.
        assertEquals(Idioma.ESPANOL, Localizacion.desdeRegion(null).idioma)
        assertEquals(UnidadPeso.KILOS, Localizacion.desdeRegion(null).unidadPeso)
    }

    @Test
    fun `la etiqueta regional se interpreta con guion o con guion bajo`() {
        assertEquals(Idioma.ALEMAN, Idioma.desdeEtiqueta("de_DE"))
        assertEquals(Idioma.ALEMAN, Idioma.desdeEtiqueta("de-DE"))
        assertEquals(Idioma.ALEMAN, Idioma.desdeEtiqueta("de"))
        assertEquals(Idioma.FRANCES, Idioma.desdeEtiqueta("fr-CA"))
    }

    @Test
    fun `la conversion a libras usa el factor correcto`() {
        val configuracion = Localizacion.desdeRegion("en-US")

        // 26,40 kg son 58,20 lb. El valor exacto se comprueba aparte para no depender del
        // redondeo al mostrarlo.
        val libras = UnidadPeso.desdeKilos(26.40, UnidadPeso.LIBRAS)
        assertEquals(58.20, libras, 0.01)

        // Y volver atrás tiene que devolver el valor original: es lo que se guarda.
        assertEquals(26.40, UnidadPeso.aKilos(libras, UnidadPeso.LIBRAS), 0.000001)

        // El texto mostrado lleva dos decimales y punto, porque es inglés.
        assertEquals("58.20", configuracion.formatearPeso(26.40))
        assertEquals("58.20 lb", configuracion.formatearPeso(26.40, conUnidad = true))
    }

    @Test
    fun `en espanol el peso se muestra en kilos y con coma`() {
        val configuracion = Localizacion.desdeRegion("es-ES")

        assertEquals("26,40", configuracion.formatearPeso(26.40))
        assertEquals("26,40 kg", configuracion.formatearPeso(26.40, conUnidad = true))
        assertEquals("kg", configuracion.unidadPesoSufijo)
    }

    @Test
    fun `cambiar de unidad no cambia el numero de kilos guardado`() {
        val enEspanol = Localizacion.desdeRegion("es-ES")
        val enIngles = Localizacion.desdeRegion("en-US")

        // El mismo dato guardado se ve distinto, pero es el mismo dato.
        assertNotEquals(enEspanol.formatearPeso(12.5), enIngles.formatearPeso(12.5))
        assertEquals(12.5, UnidadPeso.aKilos(12.5, UnidadPeso.KILOS), 0.000001)
    }

    @Test
    fun `la fecha se formatea en el orden de la region`() {
        val fecha = LocalDate.parse("2026-03-04")

        assertEquals("04/03/2026", Localizacion.desdeRegion("es-ES").formatearFecha(fecha))
        assertEquals("03/04/2026", Localizacion.desdeRegion("en-US").formatearFecha(fecha))
        assertEquals("03/04/2026", Localizacion.desdeRegion("en-GB").formatearFecha(fecha))
    }

    @Test
    fun `la pista de formato de fecha acompana al orden`() {
        assertEquals("DD/MM/AAAA", Localizacion.desdeRegion("es-ES").pistaFormatoFecha)
        assertEquals("MM/DD/AAAA", Localizacion.desdeRegion("en-US").pistaFormatoFecha)
    }
}
