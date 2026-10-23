import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.example.virtualpetkmp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // Es el nombre que aparece en el menú de inicio, en la carpeta instalada y en
            // "Aplicaciones instaladas": el de la app, no el del proyecto.
            packageName = "Virtual Pet"
            packageVersion = "1.8.0"
            description = "Diario de salud y peso para tu mascota"

            // Módulos de la JDK que hay que meter en el runtime empaquetado.
            //
            // **Esto no es opcional:** jpackage solo incluye `java.base` por defecto, y el driver
            // JDBC de SQLite necesita `java.sql` (`java/sql/DriverManager`). Sin él, la app
            // empaquetada compila y se instala bien, pero al abrirla revienta con
            // `NoClassDefFoundError: java/sql/DriverManager`. Se descubrió probando el instalable
            // de verdad: desde Gradle (`run`) nunca falla, porque ahí se usa la JDK completa.
            modules("java.sql", "jdk.unsupported")

            windows {
                // Icono del ejecutable y del acceso directo. Sale del logo nuevo (solo el
                // corazón): el de antes llevaba el nombre debajo y se duplicaba con la etiqueta.
                iconFile.set(project.file("icons/app.ico"))
            }
        }
    }
}