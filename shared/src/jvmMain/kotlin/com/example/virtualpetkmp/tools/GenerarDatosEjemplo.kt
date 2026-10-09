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
 * aspecto: una vacuna vencida, una próxima, preventivo al día y un tratamiento activo.
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

    // --- Mascota -------------------------------------------------------------
    // La ruta de la foto es la del almacenamiento interno de la app en Android. Si el
    // fichero no existe, la app muestra la inicial del nombre, así que no rompe nada.
    val rutaFoto = "/data/data/com.example.virtualpetkmp/files/luna.jpg"
    driver.execute(
        null,
        """
        INSERT INTO mascotas(nombre, especie, raza, fechaNacimiento, sexo, color, microchip, foto, peso_ideal)
        VALUES ('Luna', 'Perro', 'Labrador Retriever', '${hoyMenos(920)}', 'Hembra', 'Dorado',
                '981000123456789', '$rutaFoto', 27.0)
        """.trimIndent(),
        0
    )
    // El id se lee por nombre y no con last_insert_rowid(): el driver JDBC puede usar otra
    // conexión y esa función devolvería 0.
    val idMascota = db.mascotasQueries.selectAll().executeAsList()
        .first { it.nombre == "Luna" }
        .id

    // --- Vacunas: una pasada, una próxima y una al día ------------------------
    fun vacuna(nombre: String, aplicadaHace: Int, venceEn: Int, vet: String, lote: String) {
        driver.execute(
            null,
            """
            INSERT INTO vacunas(mascotaId, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote)
            VALUES ($idMascota, '$nombre', '${hoyMenos(aplicadaHace)}', '${hoyMas(venceEn)}', '$vet', '$lote')
            """.trimIndent(),
            0
        )
    }
    // Rabia: dosis anual pasada (vencida). Polivalente: vence dentro de 11 días (próxima).
    // Tos de las perreras: reciente y al día. Así la tarjeta muestra las tres situaciones.
    vacuna("Rabia", aplicadaHace = 365, venceEn = -1, vet = "Dra. Ruiz", lote = "R-2291")
    vacuna("Polivalente (DHPPi + Lepto)", aplicadaHace = 354, venceEn = 11, vet = "Dra. Ruiz", lote = "P-8842")
    vacuna("Tos de las perreras", aplicadaHace = 40, venceEn = 320, vet = "Dr. López", lote = "T-1073")

    // --- Preventivos ----------------------------------------------------------
    driver.execute(
        null,
        """
        INSERT INTO preventivos(mascotaId, tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote)
        VALUES ($idMascota, 'Pipeta', 'Frontline Tri-Act', '${hoyMenos(22)}', '${hoyMas(8)}', 'Dra. Ruiz', 'FT-5521')
        """.trimIndent(),
        0
    )
    driver.execute(
        null,
        """
        INSERT INTO preventivos(mascotaId, tipo, nombre, fechaAplicacion, fechaProximaDosis, veterinario, lote)
        VALUES ($idMascota, 'Desparasitación interna', 'Drontal Plus', '${hoyMenos(95)}', '${hoyMas(85)}', 'Dra. Ruiz', 'DP-3310')
        """.trimIndent(),
        0
    )

    // --- Pesos: subida realista, con el último por DEBAJO del ideal -----------
    // (días transcurridos hasta hoy, kilos)
    val pesadas = listOf(
        -330 to 24.8,
        -270 to 25.1,
        -210 to 25.4,
        -150 to 25.9,
        -95 to 26.2,
        -40 to 26.4,
        -6 to 26.4
    )
    pesadas.forEach { (hace, kilos) ->
        driver.execute(
            null,
            "INSERT INTO pesos(mascotaId, fecha, peso, notas) VALUES ($idMascota, '${hoyMenos(-hace)}', $kilos, NULL)",
            0
        )
    }

    driver.execute(
        null,
        "UPDATE pesos SET notas = 'Pesada en casa' WHERE mascotaId = $idMascota AND fecha = '${hoyMenos(95)}'",
        0
    )

    // --- Tratamiento activo y uno terminado ----------------------------------
    driver.execute(
        null,
        """
        INSERT INTO tratamientos(mascotaId, nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin)
        VALUES ($idMascota, 'Oticoear', '4 gotas', 'Cada 12 horas', '${hoyMenos(5)}', '${hoyMas(9)}')
        """.trimIndent(),
        0
    )
    driver.execute(
        null,
        """
        INSERT INTO tratamientos(mascotaId, nombreMedicamento, dosis, frecuencia, fechaInicio, fechaFin)
        VALUES ($idMascota, 'Metacam', '1,5 ml', 'Cada 24 horas', '${hoyMenos(120)}', '${hoyMenos(110)}')
        """.trimIndent(),
        0
    )

    // --- Revisiones -----------------------------------------------------------
    driver.execute(
        null,
        """
        INSERT INTO revisiones(mascotaId, fecha, motivo, diagnostico, notas, veterinario)
        VALUES ($idMascota, '${hoyMenos(350)}', 'Revisión anual', 'Todo correcto',
                'Peso y dentadura bien. Se actualiza la polivalente.', 'Dra. Ruiz')
        """.trimIndent(),
        0
    )
    driver.execute(
        null,
        """
        INSERT INTO revisiones(mascotaId, fecha, motivo, diagnostico, notas, veterinario)
        VALUES ($idMascota, '${hoyMenos(5)}', 'Sacude la cabeza y se rasca el oído',
                'Otitis externa leve en el oído derecho', 'Se pauta tratamiento ótico 14 días.', 'Dra. Ruiz')
        """.trimIndent(),
        0
    )

    // --- Notas ---------------------------------------------------------------
    driver.execute(
        null,
        "INSERT INTO notas(mascotaId, texto) VALUES ($idMascota, 'Alergia a la proteína de pollo: evitar piensos con pollo como primer ingrediente.')",
        0
    )
    driver.execute(
        null,
        "INSERT INTO notas(mascotaId, texto) VALUES ($idMascota, 'Le asustan los petardos: en verano conviene dejarle un sitio tranquilo y cerrar persianas.')",
        0
    )

    // --- Veterinarios --------------------------------------------------------
    driver.execute(
        null,
        """
        INSERT INTO veterinarios(nombreClinica, nombreVeterinario, telefono, direccion, esUrgencias)
        VALUES ('Hospital Veterinario 24h', 'Dra. Ruiz', '911234567', 'Calle Mayor 12, Madrid', 1)
        """.trimIndent(),
        0
    )
    driver.execute(
        null,
        """
        INSERT INTO veterinarios(nombreClinica, nombreVeterinario, telefono, direccion, esUrgencias)
        VALUES ('Clínica del Barrio', 'Dr. López', '918765432', 'Avenida del Parque 4, Madrid', 0)
        """.trimIndent(),
        0
    )

    driver.close()
    println("Datos de ejemplo generados en ${salida.absolutePath} (mascota id=$idMascota, hoy=$hoy)")
}
