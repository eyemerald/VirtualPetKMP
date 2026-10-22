package com.example.virtualpetkmp.tools

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Genera una base de datos de ejemplo para las capturas del README y la deja en la ruta que
 * se le pase como primer argumento.
 *
 * No forma parte de la aplicación: es una herramienta de desarrollo. Se ejecuta con
 * `./gradlew :shared:generarDatosEjemplo -PejemploSalida=<ruta>`.
 *
 * Todas las fechas se calculan a partir de HOY, para que el ejemplo muestre siempre el mismo
 * aspecto: una vacuna vencida, una próxima, un preventivo al día, un tratamiento activo y
 * pesajes repartidos por el año **y dentro del mes en curso** (si no, el filtro "mes actual"
 * del gráfico saldría vacío).
 */
fun main(args: Array<String>) {
    val salida = File(args.firstOrNull() ?: "datos-ejemplo.db")
    salida.delete()
    if (salida.parentFile?.exists() == false) salida.parentFile?.mkdirs()

    val driver = JdbcSqliteDriver("jdbc:sqlite:${salida.absolutePath}")
    VirtualPetDatabase.Schema.create(driver)
    // Hay que marcar la versión del esquema a mano: el driver de Android se fía de
    // `PRAGMA user_version` para decidir si la base es nueva (y crear las tablas) o si ya
    // existe (y solo migrar). Sin esto, la app encontraría las tablas ya creadas e intentaría
    // crearlas otra vez, y reventaría con "table ... already exists".
    driver.execute(null, "PRAGMA user_version = ${VirtualPetDatabase.Schema.version};", 0)
    val db = VirtualPetDatabase(driver)

    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    fun hoyMenos(dias: Int) = LocalDate.fromEpochDays(hoy.toEpochDays() - dias)
    fun hoyMas(dias: Int) = LocalDate.fromEpochDays(hoy.toEpochDays() + dias)

    fun ejecutar(sql: String) = driver.execute(null, sql.trimIndent(), 0)

    /**
     * `uuid` y `actualizadoEn` son obligatorios desde la versión 14 del esquema. Aquí se escriben
     * con funciones de SQLite en vez de generarlos en Kotlin porque este generador escribe con
     * SQL a pelo y no pasa por los repositorios.
     */
    val uuidNuevo = "lower(hex(randomblob(16)))"
    val ahoraMillis = "CAST(strftime('%s', 'now') AS INTEGER) * 1000"

    // --- Ayudas para escribir los datos de cada mascota ------------------------
    // La mascota se crea SIN foto a propósito: el ejemplo no trae ninguna imagen, y con una
    // ruta que no existe la app intenta cargarla y deja el hueco del avatar vacío. Sin foto
    // dibuja el avatar de la especie, que es lo que sale en las capturas del README.
    fun insertarMascota(
        nombre: String,
        especie: String,
        raza: String,
        nacimientoHace: Int,
        sexo: String,
        color: String,
        microchip: String?,
        pesoIdeal: Double
    ): Long {
        ejecutar(
            """
            INSERT INTO mascotas(nombre, especie, raza, fechaNacimiento, sexo, color, microchip, foto, peso_ideal, uuid, actualizadoEn)
            VALUES ('$nombre', '$especie', '$raza', '${hoyMenos(nacimientoHace)}', '$sexo', '$color',
                    ${microchip?.let { "'$it'" } ?: "NULL"}, NULL, $pesoIdeal, $uuidNuevo, $ahoraMillis)
            """
        )
        // El id se lee por nombre y no con last_insert_rowid(): el driver JDBC puede usar otra
        // conexión y esa función devolvería 0.
        return db.mascotasQueries.selectAll().executeAsList().first { it.nombre == nombre }.id
    }

    fun vacuna(mascota: Long, nombre: String, aplicadaHace: Int, venceEn: Int, vet: String, lote: String) =
        ejecutar(
            """
            INSERT INTO vacunas(mascotaId, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote, uuid, actualizadoEn)
            VALUES ($mascota, '$nombre', '${hoyMenos(aplicadaHace)}', '${hoyMas(venceEn)}', '$vet', '$lote',
                    $uuidNuevo, $ahoraMillis)
            """
        )

    fun preventivo(mascota: Long, tipo: String, nombre: String, aplicadoHace: Int, venceEn: Int, lote: String) =
        ejecutar(
            """
            INSERT INTO preventivos(mascotaId, tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote, uuid, actualizadoEn)
            VALUES ($mascota, '$tipo', '$nombre', '${hoyMenos(aplicadoHace)}', '${hoyMas(venceEn)}', 'Dra. Ruiz', '$lote',
                    $uuidNuevo, $ahoraMillis)
            """
        )

    fun peso(mascota: Long, fecha: LocalDate, kilos: Double, notas: String? = null) =
        ejecutar(
            """
            INSERT INTO pesos(mascotaId, fecha, peso, notas, uuid, actualizadoEn)
            VALUES ($mascota, '$fecha', $kilos, ${notas?.let { "'$it'" } ?: "NULL"}, $uuidNuevo, $ahoraMillis)
            """
        )

    fun tratamiento(
        mascota: Long,
        medicamento: String,
        dosis: String,
        frecuencia: String,
        desdeHace: Int,
        hastaEn: Int,
        hora: String? = null,
        notificar: Boolean = false
    ) =
        ejecutar(
            """
            INSERT INTO tratamientos(mascotaId, nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin, hora, notificar, uuid, actualizadoEn)
            VALUES ($mascota, '$medicamento', '$dosis', '$frecuencia', '${hoyMenos(desdeHace)}', '${hoyMas(hastaEn)}',
                    ${hora?.let { "'$it'" } ?: "NULL"}, ${if (notificar) 1 else 0}, $uuidNuevo, $ahoraMillis)
            """
        )

    fun tratamientoTerminado(mascota: Long, medicamento: String, dosis: String, frecuencia: String, desdeHace: Int, hastaHace: Int) =
        ejecutar(
            """
            INSERT INTO tratamientos(mascotaId, nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin, uuid, actualizadoEn)
            VALUES ($mascota, '$medicamento', '$dosis', '$frecuencia', '${hoyMenos(desdeHace)}', '${hoyMenos(hastaHace)}',
                    $uuidNuevo, $ahoraMillis)
            """
        )

    fun revision(mascota: Long, hace: Int, motivo: String, diagnostico: String, notas: String) =
        ejecutar(
            """
            INSERT INTO revisiones(mascotaId, fecha, motivo, diagnostico, notas, veterinario, uuid, actualizadoEn)
            VALUES ($mascota, '${hoyMenos(hace)}', '$motivo', '$diagnostico', '$notas', 'Dra. Ruiz', $uuidNuevo, $ahoraMillis)
            """
        )

    fun nota(mascota: Long, texto: String) =
        ejecutar("INSERT INTO notas(mascotaId, texto, uuid, actualizadoEn) VALUES ($mascota, '$texto', $uuidNuevo, $ahoraMillis)")

    fun informe(mascota: Long, hace: Int, tipo: String, descripcion: String, archivo: String) =
        ejecutar(
            """
            INSERT INTO informes(mascotaId, tipo, descripcion, fecha, nombreArchivo, rutaArchivo, uuid, actualizadoEn)
            VALUES ($mascota, '$tipo', '$descripcion', '${hoyMenos(hace)}', '$archivo',
                    '/data/data/com.example.virtualpetkmp/files/informes/$archivo', $uuidNuevo, $ahoraMillis)
            """
        )

    /**
     * Pesajes del mes en curso, repartidos por lo que llevamos de mes.
     *
     * Se calculan así y no con días fijos para que el filtro "Mes actual" del gráfico tenga
     * varios puntos se genere el ejemplo el día 3 o el 28.
     */
    fun diasDelMesEnCurso(cuantos: Int): List<Int> {
        val pasados = (1 until hoy.dayOfMonth).toList()
        if (pasados.size <= cuantos) return pasados
        return (0 until cuantos).map { i -> pasados[i * (pasados.size - 1) / (cuantos - 1)] }.distinct()
    }

    // =========================================================================
    // Luna: la mascota con la ficha completa (es la que sale en las capturas)
    // =========================================================================
    val luna = insertarMascota(
        nombre = "Luna",
        especie = "Perro",
        raza = "Labrador Retriever",
        nacimientoHace = 920,
        sexo = "Hembra",
        color = "Dorado",
        microchip = "981000123456789",
        pesoIdeal = 27.0
    )

    // Rabia: dosis anual pasada (vencida). Polivalente: vence dentro de 11 días (próxima).
    // Tos de las perreras: reciente y al día. Así la lista muestra las tres situaciones.
    vacuna(luna, "Rabia", aplicadaHace = 365, venceEn = -1, vet = "Dra. Ruiz", lote = "R-2291")
    vacuna(luna, "Polivalente (DHPPi + Lepto)", aplicadaHace = 354, venceEn = 11, vet = "Dra. Ruiz", lote = "P-8842")
    vacuna(luna, "Tos de las perreras", aplicadaHace = 40, venceEn = 320, vet = "Dr. López", lote = "T-1073")

    preventivo(luna, "Pipeta", "Frontline Tri-Act", aplicadoHace = 22, venceEn = 8, lote = "FT-5521")
    preventivo(luna, "Desparasitación interna", "Drontal Plus", aplicadoHace = 95, venceEn = 85, lote = "DP-3310")

    // Evolución del último año: subida realista desde 24,8 kg hasta los 26,4 kg de hoy, con el
    // último peso por debajo del ideal (27 kg) para que la lista enseñe el indicador ▼.
    listOf(
        -330 to 24.8,
        -270 to 25.1,
        -210 to 25.4,
        -150 to 25.9,
        -95 to 26.2,
        -40 to 26.2
    ).forEach { (hace, kilos) -> peso(luna, hoyMenos(-hace), kilos, if (hace == -95) "Pesada en casa" else null) }

    val delMes = diasDelMesEnCurso(cuantos = 4)
    val kilosDelMes = listOf(26.3, 26.5, 26.4, 26.4)
    delMes.forEachIndexed { indice, dias ->
        peso(luna, hoyMenos(dias), kilosDelMes.getOrElse(indice) { 26.4 })
    }

    // El tratamiento activo lleva recordatorio a las 9 de la mañana: es el caso que aparece en
    // las capturas del README y conviene que los datos de ejemplo lo representen.
    tratamiento(
        luna, "Oticoear", "4 gotas", "Cada 12 horas",
        desdeHace = 5, hastaEn = 9, hora = "09:00", notificar = true
    )
    tratamientoTerminado(luna, "Metacam", "1,5 ml", "Cada 24 horas", desdeHace = 120, hastaHace = 110)

    revision(
        luna, hace = 350, motivo = "Revisión anual", diagnostico = "Todo correcto",
        notas = "Peso y dentadura bien. Se actualiza la polivalente."
    )
    revision(
        luna, hace = 5, motivo = "Sacude la cabeza y se rasca el oído",
        diagnostico = "Otitis externa leve en el oído derecho", notas = "Se pauta tratamiento ótico 14 días."
    )

    informe(luna, hace = 5, tipo = "Analítica", descripcion = "Hemograma y bioquímica", archivo = "analitica-luna.pdf")
    informe(luna, hace = 349, tipo = "Radiografía", descripcion = "Cadera, control anual", archivo = "radiografia-cadera.jpg")

    nota(luna, "Alergia a la proteína de pollo: evitar piensos con pollo como primer ingrediente.")
    nota(luna, "Le asustan los petardos: en verano conviene dejarle un sitio tranquilo y cerrar persianas.")

    // =========================================================================
    // Nilo: segunda mascota, para que la lista no salga con una sola
    // =========================================================================
    val nilo = insertarMascota(
        nombre = "Nilo",
        especie = "Gato",
        raza = "Europeo común",
        nacimientoHace = 400,
        sexo = "Macho",
        color = "Negro",
        microchip = null,
        pesoIdeal = 4.5
    )

    vacuna(nilo, "Trivalente felina", aplicadaHace = 300, venceEn = 65, vet = "Dra. Ruiz", lote = "F-4410")
    preventivo(nilo, "Pipeta", "Advantage", aplicadoHace = 30, venceEn = 1, lote = "AD-9902")
    // Acaba un poco por encima de su peso ideal, para que la lista enseñe el indicador ▲ verde
    // (Luna sale con ▼ rojo) y las dos situaciones se vean en la misma captura.
    listOf(-240 to 4.2, -150 to 4.5, -60 to 4.6, -3 to 4.7).forEach { (hace, kilos) ->
        peso(nilo, hoyMenos(-hace), kilos)
    }
    nota(nilo, "Come pienso de gato esterilizado; el húmedo le sienta mal.")

    // --- Veterinarios --------------------------------------------------------
    ejecutar(
        """
        INSERT INTO veterinarios(nombreClinica, nombreVeterinario, telefono, direccion, esUrgencias, uuid, actualizadoEn)
        VALUES ('Hospital Veterinario 24h', 'Dra. Ruiz', '911234567', 'Calle Mayor 12, Madrid', 1, $uuidNuevo, $ahoraMillis)
        """
    )
    ejecutar(
        """
        INSERT INTO veterinarios(nombreClinica, nombreVeterinario, telefono, direccion, esUrgencias, uuid, actualizadoEn)
        VALUES ('Clínica del Barrio', 'Dr. López', '918765432', 'Avenida del Parque 4, Madrid', 0, $uuidNuevo, $ahoraMillis)
        """
    )

    driver.close()
    println("Datos de ejemplo generados en ${salida.absolutePath} (Luna id=$luna, Nilo id=$nilo, hoy=$hoy)")
}
