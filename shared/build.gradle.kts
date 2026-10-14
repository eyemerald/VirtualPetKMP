import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.example.virtualpetkmp.shared"

        // La versión se escribe en el catálogo como "37" o como "37.0", y las dos formas son
        // naturales porque el SDK instala la API 37 en `platforms/android-37.0`. Antes esto era
        // un `.toInt()` a secas, que reventaba con un NumberFormatException poco claro en cuanto
        // alguien escribía el punto.
        //
        // Las versiones menores distintas de cero se rechazan a propósito: el DSL del módulo de
        // librería KMP (`com.android.kotlin.multiplatform.library`) no expone `compileSdkMinor`
        // —el de aplicación sí—, así que aceptarlas aquí haría que los dos módulos compilaran
        // contra plataformas distintas.
        val apiCompilacion = libs.versions.android.compileSdk.get()
        require(apiCompilacion.substringAfter('.', "0") == "0") {
            "compileSdk = \"$apiCompilacion\": este proyecto solo compila con versión menor 0 " +
                "(escribe \"37\" o \"37.0\")"
        }
        compileSdk = apiCompilacion.substringBefore('.').toInt()

        // Necesario para que los recursos de Compose (composeResources) se empaqueten
        // como assets de Android. Sin esto, el target androidLibrary de AGP 9 no ejecuta
        // el pipeline de assets y Res.drawable.* falla en runtime con MissingResourceException.
        androidResources {
            enable = true
        }
    }
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation("io.github.rikoappdev:compose-pdf:0.9.0")
        }
        jvmMain.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
            implementation(libs.sqldelight.coroutines.extensions)
        }
        jvmTest.dependencies {
            // Los tests de carga de imágenes usan el decodificador de Skia (skiko), que en
            // JVM necesita los binarios nativos que aporta la distribución de escritorio.
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

/**
 * Los tests leen textos de `composeResources`, que resuelve el idioma del sistema: sin fijarlo,
 * la misma prueba pasaría o fallaría según el idioma del ordenador de quien la ejecute. Se fija
 * el español (el idioma en el que están escritas las comprobaciones) y una región con kilos y
 * fecha día/mes, que es lo que esperan los tests de peso y de fechas.
 */
tasks.withType<Test>().configureEach {
    systemProperty("user.language", "es")
    systemProperty("user.country", "ES")
}

sqldelight {
    databases {
        create("VirtualPetDatabase") {
            packageName.set("com.example.virtualpetkmp.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

/**
 * Herramienta de desarrollo: genera una base de datos con datos de ejemplo (mascota, vacunas,
 * pesos, tratamientos, notas y veterinarios) para preparar las capturas del README.
 *
 * No forma parte de la app. Uso:
 *   ./gradlew :shared:generarDatosEjemplo -PejemploSalida=C:/ruta/datos-ejemplo.db
 */
tasks.register<JavaExec>("generarDatosEjemplo") {
    group = "documentación"
    description = "Genera una base de datos de ejemplo para las capturas del README"
    val fuente = kotlin.jvm().compilations.getByName("main")
    classpath = fuente.runtimeDependencyFiles + fuente.output.allOutputs
    mainClass.set("com.example.virtualpetkmp.tools.GenerarDatosEjemploKt")
    args = listOf(
        (project.findProperty("ejemploSalida") as String?)
            ?: layout.buildDirectory.file("datos-ejemplo.db").get().asFile.absolutePath
    )
}
