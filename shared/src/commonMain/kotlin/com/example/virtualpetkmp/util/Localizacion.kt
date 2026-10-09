package com.example.virtualpetkmp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate

/** Idioma de la interfaz. */
enum class Idioma(val codigo: String) {
    ESPANOL("es"),
    INGLES("en"),
    PORTUGUES("pt"),
    FRANCES("fr"),
    ALEMAN("de"),
    ITALIANO("it");

    companion object {
        /**
         * Idioma a partir de la etiqueta regional del sistema ("es-ES", "en_US", "de").
         *
         * Solo se mira la parte del idioma: las variantes regionales (es-ES, es-MX, en-GB)
         * comparten textos. Lo que sí cambia entre regiones son las unidades y el formato de
         * fecha, y eso se resuelve en [Localizacion], no aquí.
         */
        fun desdeEtiqueta(etiqueta: String?): Idioma {
            val idioma = etiqueta?.substringBefore('-')?.substringBefore('_')?.lowercase()
            return entries.firstOrNull { it.codigo == idioma } ?: ESPANOL
        }
    }
}

/** Unidad de peso que se muestra al usuario. Internamente **siempre** se guarda en kilos. */
enum class UnidadPeso(val sufijo: String) {
    KILOS("kg"),
    LIBRAS("lb");

    companion object {
        /** 1 kg = 2.20462262185 lb (definición exacta de la libra internacional). */
        const val LIBRAS_POR_KILO = 2.20462262185

        fun aKilos(valorMostrado: Double, unidad: UnidadPeso): Double = when (unidad) {
            KILOS -> valorMostrado
            LIBRAS -> valorMostrado / LIBRAS_POR_KILO
        }

        fun desdeKilos(kilos: Double, unidad: UnidadPeso): Double = when (unidad) {
            KILOS -> kilos
            LIBRAS -> kilos * LIBRAS_POR_KILO
        }
    }
}

/** Orden de los componentes de una fecha escrita con barras. */
enum class OrdenFecha { DIA_MES_ANIO, MES_DIA_ANIO }

/**
 * Todo lo que depende del idioma o de la región del dispositivo.
 *
 * Se pasa por el árbol de composición con [Localizacion] en lugar de consultar el sistema cada
 * vez, por dos motivos: los ficheros de recursos de Compose ya resuelven solos el idioma de los
 * textos, y así las decisiones de formato (unidades, orden de fecha, separador decimal) se
 * toman **en un solo sitio** y se pueden probar sin depender del dispositivo.
 */
data class Localizacion(
    val idioma: Idioma,
    /** Etiqueta regional completa, por ejemplo "en-US". Decide las unidades. */
    val region: String,
    val unidadPeso: UnidadPeso,
    /** Lo que se le pide al usuario que escriba en los campos de fecha de la interfaz. */
    val ordenFechaMostrada: OrdenFecha,
    /** Separador decimal al mostrar números: coma en español, punto en el resto. */
    val separadorDecimal: Char
) {
    /**
     * Pesos con dos decimales y su unidad, en la unidad del usuario.
     * Ejemplo: 1.75 kg → "1,75" en español, "3.86" en inglés (libras).
     */
    fun formatearPeso(kilos: Double, conUnidad: Boolean = false): String {
        val valor = UnidadPeso.desdeKilos(kilos, unidadPeso)
        val texto = formatearConDecimales(valor, 2)
        return if (conUnidad) "$texto $unidadPesoSufijo" else texto
    }

    /** "kg" o "lb", para ponerlo junto a un número ya formateado. */
    val unidadPesoSufijo: String get() = unidadPeso.sufijo

    /** Número con los decimales indicados usando el separador decimal de la región. */
    fun formatearConDecimales(valor: Double, decimales: Int): String {
        var factor = 1L
        repeat(decimales) { factor *= 10 }
        val escalado = kotlin.math.round(kotlin.math.abs(valor) * factor).toLong()
        val signo = if (valor < 0) "-" else ""
        val entero = escalado / factor
        if (decimales == 0) return "$signo$entero"
        val resto = (escalado % factor).toString().padStart(decimales, '0')
        return "$signo$entero$separadorDecimal$resto"
    }

    /** Fecha para mostrar en pantalla, en el orden de la región. */
    fun formatearFecha(fecha: LocalDate): String {
        val dia = fecha.dayOfMonth.toString().padStart(2, '0')
        val mes = fecha.monthNumber.toString().padStart(2, '0')
        return when (ordenFechaMostrada) {
            OrdenFecha.DIA_MES_ANIO -> "$dia/$mes/${fecha.year}"
            OrdenFecha.MES_DIA_ANIO -> "$mes/$dia/${fecha.year}"
        }
    }

    /**
     * Edad en texto sin recursos, para los sitios donde no hay contexto composable (por
     * ejemplo, generar el PDF desde una corrutina). Aquí el plural es simplemente "-s" cuando
     * hay más de uno, que cubre los seis idiomas soportados por igual: en los seis el plural
     * se forma añadiendo una ese o no cambia ("anni", "Jahre", "anos").
     *
     * Si algún día se añade un idioma con plurales complejos (ruso, polaco, árabe), habrá que
     * sustituir esto por los plurales de los recursos y pasar el texto ya montado.
     */
    fun formatearEdad(anios: Int, meses: Int): String {
        val etiquetaAnios = when (idioma) {
            Idioma.ESPANOL -> if (anios == 1) "1 año" else "$anios años"
            Idioma.INGLES -> if (anios == 1) "1 year" else "$anios years"
            Idioma.PORTUGUES -> if (anios == 1) "1 ano" else "$anios anos"
            Idioma.FRANCES -> if (anios == 1) "1 an" else "$anios ans"
            Idioma.ALEMAN -> if (anios == 1) "1 Jahr" else "$anios Jahre"
            Idioma.ITALIANO -> if (anios == 1) "1 anno" else "$anios anni"
        }
        if (meses == 0) return etiquetaAnios

        val etiquetaMeses = when (idioma) {
            Idioma.ESPANOL -> if (meses == 1) "1 mes" else "$meses meses"
            Idioma.INGLES -> if (meses == 1) "1 month" else "$meses months"
            Idioma.PORTUGUES -> if (meses == 1) "1 mês" else "$meses meses"
            Idioma.FRANCES -> if (meses == 1) "1 mois" else "$meses mois"
            Idioma.ALEMAN -> if (meses == 1) "1 Monat" else "$meses Monate"
            Idioma.ITALIANO -> if (meses == 1) "1 mese" else "$meses mesi"
        }
        val conector = when (idioma) {
            Idioma.ESPANOL -> " y "
            Idioma.INGLES -> " and "
            Idioma.PORTUGUES -> " e "
            Idioma.FRANCES -> " et "
            Idioma.ALEMAN -> " und "
            Idioma.ITALIANO -> " e "
        }
        return "$etiquetaAnios$conector$etiquetaMeses"
    }

    /**
     * Formato de fecha que se le pide al usuario: "DD/MM/AAAA" o "MM/DD/AAAA".
     * Se usa en las etiquetas de los campos para que sepa qué se espera.
     */
    val pistaFormatoFecha: String
        get() = when (ordenFechaMostrada) {
            OrdenFecha.DIA_MES_ANIO -> "DD/MM/AAAA"
            OrdenFecha.MES_DIA_ANIO -> "MM/DD/AAAA"
        }

    companion object {
        /**
         * Decide la configuración regional a partir de la etiqueta del sistema.
         *
         * Reglas acordadas:
         * - **Unidades**: libras solo en Estados Unidos. Reino Unido se queda en kilos, porque
         *   para el peso corporal de un animal allí se usan kilogramos.
         * - **Fecha**: orden mes/día solo en inglés; el resto, día/mes.
         * - **Decimal**: coma en español y portugués, punto en el resto.
         */
        fun desdeRegion(etiqueta: String?): Localizacion {
            val idioma = Idioma.desdeEtiqueta(etiqueta)
            val region = etiqueta?.replace('_', '-') ?: idioma.codigo
            val esEstadosUnidos = region.equals("en-US", ignoreCase = true) ||
                region.equals("en_US", ignoreCase = true)

            return Localizacion(
                idioma = idioma,
                region = region,
                unidadPeso = if (esEstadosUnidos) UnidadPeso.LIBRAS else UnidadPeso.KILOS,
                ordenFechaMostrada = if (idioma == Idioma.INGLES) {
                    OrdenFecha.MES_DIA_ANIO
                } else {
                    OrdenFecha.DIA_MES_ANIO
                },
                separadorDecimal = if (idioma == Idioma.ESPANOL || idioma == Idioma.PORTUGUES) ',' else '.'
            )
        }
    }
}

/**
 * Configuración regional activa. Por defecto, español con kilos y día/mes: es el idioma en el
 * que están escritos los recursos de `values/` mientras no se detecte otra cosa.
 */
val LocalLocalizacion: ProvidableCompositionLocal<Localizacion> = staticCompositionLocalOf {
    Localizacion.desdeRegion("es-ES")
}

/** Atajo para leer la configuración regional desde cualquier composable. */
val localizacion: Localizacion
    @Composable @ReadOnlyComposable
    get() = LocalLocalizacion.current

/**
 * Publica la configuración regional al resto de la interfaz.
 * Se llama una sola vez, en la raíz de la app.
 */
@Composable
fun ConLocalizacion(localizacion: Localizacion, contenido: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLocalizacion provides localizacion, content = contenido)
}
