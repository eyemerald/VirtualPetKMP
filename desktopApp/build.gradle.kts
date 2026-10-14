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
            packageVersion = "1.0.0"
            description = "Diario de salud y peso para tu mascota"

            windows {
                // Icono del ejecutable y del acceso directo. Sale del logo nuevo (solo el
                // corazón): el de antes llevaba el nombre debajo y se duplicaba con la etiqueta.
                iconFile.set(project.file("icons/app.ico"))
            }
        }
    }
}