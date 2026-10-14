package com.example.virtualpetkmp.util

import com.example.virtualpetkmp.Peso
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Filtros del gráfico de peso: **6 meses**, año, mes actual y rango de fechas.
 *
 * El de "mes actual" es el que más fácil se rompe sin que nadie se entere: si se comparara solo
 * el mes sin el año, octubre de este año arrastraría también el octubre del anterior, y el
 * gráfico enseñaría pesajes de hace un año como si fueran de este mes.
 *
 * El de "6 meses" tiene su propia trampa: la ventana se cuenta **desde hoy**, no desde el último
 * pesaje. Si la mascota lleva meses sin pesarse, el rango sigue siendo el mismo y el gráfico sale
 * vacío, que es la verdad.
 */
class FiltrosPesoTest {

    /** Hoy, fijo: así los filtros que dependen de la fecha no cambian según cuándo se ejecute. */
    private val hoy = LocalDate.parse("2026-10-13")

    private fun peso(fecha: String, kilos: Double) = Peso(
        id = null,
        mascotaId = 1L,
        fecha = LocalDate.parse(fecha),
        peso = kilos,
        notas = null
    )

    private val historial = listOf(
        peso("2025-10-05", 20.0),
        peso("2026-01-20", 21.5),
        peso("2026-09-30", 24.0),
        peso("2026-10-02", 24.8),
        peso("2026-10-13", 25.1),
        peso("2026-11-01", 25.4)
    )

    private fun fechas(resultado: List<Peso>) = resultado.map { it.fecha.toString() }

    @Test
    fun `seis meses deja fuera lo anterior a la ventana`() {
        // Con hoy = 13/10/2026, la ventana empieza el 13/04/2026: los dos primeros pesajes
        // (octubre de 2025 y enero de 2026) se quedan fuera.
        val resultado = filtrarPesos(historial, FiltroPeso.RECIENTE, hoy)

        assertEquals(
            listOf("2026-09-30", "2026-10-02", "2026-10-13", "2026-11-01"),
            fechas(resultado)
        )
    }

    @Test
    fun `la ventana de seis meses incluye el dia exacto del limite`() {
        // El límite entra: si no, un pesaje justo en el borde desaparecería sin explicación.
        val limite = listOf(
            peso("2026-04-12", 22.0),  // un día antes: fuera
            peso("2026-04-13", 22.1)   // justo el límite: dentro
        )

        val resultado = filtrarPesos(limite, FiltroPeso.RECIENTE, hoy)

        assertEquals(listOf("2026-04-13"), fechas(resultado))
    }

    @Test
    fun `la ventana se cuenta desde hoy aunque la mascota lleve meses sin pesarse`() {
        // Todos los pesajes son de hace más de un año: el filtro devuelve la lista vacía y la
        // pantalla enseña su mensaje. Es lo correcto: estirar la ventana hasta el último pesaje
        // escondería que hace un año que no se pesa.
        val antiguos = listOf(peso("2024-01-10", 20.0), peso("2024-06-10", 21.0))

        assertTrue(filtrarPesos(antiguos, FiltroPeso.RECIENTE, hoy).isEmpty())
    }

    @Test
    fun `cambiar de filtro no reordena los pesajes`() {
        val resultado = filtrarPesos(historial, FiltroPeso.RECIENTE, hoy)

        assertEquals(fechas(resultado).sorted(), fechas(resultado))
    }

    @Test
    fun `el mes actual trae solo los pesajes de este mes`() {
        val resultado = filtrarPesos(historial, FiltroPeso.MES, hoy)

        assertEquals(listOf("2026-10-02", "2026-10-13"), fechas(resultado))
    }

    @Test
    fun `el mes actual no mezcla el mismo mes de otro ano`() {
        // El historial tiene un 5 de octubre de 2025: no puede colarse.
        val resultado = filtrarPesos(historial, FiltroPeso.MES, hoy)

        assertTrue(
            resultado.none { it.fecha.year == 2025 },
            "el filtro de mes actual se trajo un pesaje de otro año: ${fechas(resultado)}"
        )
    }

    @Test
    fun `el mes actual respeta el cambio de mes`() {
        // El mismo historial, mirado desde noviembre: el mes en curso es otro.
        val desdeNoviembre = filtrarPesos(historial, FiltroPeso.MES, LocalDate.parse("2026-11-01"))

        assertEquals(listOf("2026-11-01"), fechas(desdeNoviembre))
    }

    @Test
    fun `un mes sin pesajes deja la lista vacia, no el historial entero`() {
        val enDiciembre = filtrarPesos(historial, FiltroPeso.MES, LocalDate.parse("2026-12-24"))

        assertTrue(enDiciembre.isEmpty(), "esperaba lista vacía y salió ${fechas(enDiciembre)}")
    }

    @Test
    fun `el ano elegido trae solo ese ano`() {
        val resultado = filtrarPesos(historial, FiltroPeso.ANIO, hoy, anio = 2026)

        assertEquals(
            listOf("2026-01-20", "2026-09-30", "2026-10-02", "2026-10-13", "2026-11-01"),
            fechas(resultado)
        )
    }

    @Test
    fun `un ano sin datos deja la lista vacia`() {
        assertTrue(filtrarPesos(historial, FiltroPeso.ANIO, hoy, anio = 2020).isEmpty())
    }

    @Test
    fun `el rango de fechas recorta por los dos lados`() {
        val resultado = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-09-01"),
            hasta = LocalDate.parse("2026-10-05")
        )

        assertEquals(listOf("2026-09-30", "2026-10-02"), fechas(resultado))
    }

    @Test
    fun `un rango abierto por un lado solo recorta por el otro`() {
        val soloDesde = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-10-01")
        )
        val soloHasta = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            hasta = LocalDate.parse("2026-01-31")
        )

        assertEquals(listOf("2026-10-02", "2026-10-13", "2026-11-01"), fechas(soloDesde))
        assertEquals(listOf("2025-10-05", "2026-01-20"), fechas(soloHasta))
    }

    @Test
    fun `sin fechas el rango no recorta nada`() {
        assertEquals(historial.size, filtrarPesos(historial, FiltroPeso.FECHAS, hoy).size)
    }

    @Test
    fun `los limites del rango entran en el resultado`() {
        // El día exacto que se escribe como "desde" o "hasta" cuenta: si no, el usuario
        // escribiría la fecha de un pesaje y no lo vería.
        val resultado = filtrarPesos(
            historial,
            FiltroPeso.FECHAS,
            hoy,
            desde = LocalDate.parse("2026-10-02"),
            hasta = LocalDate.parse("2026-10-13")
        )

        assertEquals(listOf("2026-10-02", "2026-10-13"), fechas(resultado))
    }

    @Test
    fun `todos los filtros devuelven la lista ordenada`() {
        val desordenado = historial.reversed()

        FiltroPeso.entries.forEach { filtro ->
            val resultado = filtrarPesos(desordenado, filtro, hoy, anio = 2026)

            assertEquals(
                fechas(resultado).sorted(),
                fechas(resultado),
                "el gráfico espera orden cronológico con el filtro $filtro"
            )
        }
    }

    // --- Cómo se dibuja cada filtro -------------------------------------------------------

    @Test
    fun `el filtro decide la granularidad y el rotulo del eje`() {
        // "6 meses" enseña cada pesaje (el detalle de la evolución reciente) con los meses
        // debajo; "Año" agrupa por meses porque un año entero con punto por pesaje sería un
        // amasijo; "Mes actual" son todo días del mismo mes.
        assertEquals(
            PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.MES),
            presentacionDe(FiltroPeso.RECIENTE, historial)
        )
        assertEquals(
            PresentacionPeso(GranularidadPeso.MENSUAL, RotuloEjePeso.MES),
            presentacionDe(FiltroPeso.ANIO, historial)
        )
        assertEquals(
            PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.DIA),
            presentacionDe(FiltroPeso.MES, historial)
        )
    }

    @Test
    fun `elegir fechas rotula dias si el rango cabe en un mes y meses si no`() {
        val deUnMes = listOf(peso("2026-10-02", 24.8), peso("2026-10-13", 25.1))
        val deDosMeses = listOf(peso("2026-09-30", 24.0), peso("2026-10-13", 25.1))

        assertEquals(RotuloEjePeso.DIA, presentacionDe(FiltroPeso.FECHAS, deUnMes).rotulo)
        assertEquals(RotuloEjePeso.MES, presentacionDe(FiltroPeso.FECHAS, deDosMeses).rotulo)
    }

    @Test
    fun `elegir fechas siempre enseña cada pesaje, no medias`() {
        // El usuario pidió agrupación por días en este filtro: nunca se promedian los pesajes.
        assertEquals(
            GranularidadPeso.POR_PESAJES,
            presentacionDe(FiltroPeso.FECHAS, historial).granularidad
        )
    }

    @Test
    fun `sin pesajes la decision de como dibujar sigue estando definida`() {
        // El gráfico ni siquiera se dibuja cuando no hay datos (enseña su mensaje), pero la
        // decisión no puede depender de que la lista tenga algo: se pide antes.
        assertEquals(
            PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.MES),
            presentacionDe(FiltroPeso.RECIENTE, emptyList())
        )
        assertEquals(
            PresentacionPeso(GranularidadPeso.MENSUAL, RotuloEjePeso.MES),
            presentacionDe(FiltroPeso.ANIO, emptyList())
        )
        assertEquals(
            PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.DIA),
            presentacionDe(FiltroPeso.MES, emptyList())
        )
        // Sin pesajes no se sabe si el rango cruza de mes, así que se opta por días.
        assertEquals(
            PresentacionPeso(GranularidadPeso.POR_PESAJES, RotuloEjePeso.DIA),
            presentacionDe(FiltroPeso.FECHAS, emptyList())
        )
    }
}
