import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/**
 * Firma de la APK de release, leída de `keystore.properties` (que no se sube al repositorio).
 *
 * Si el fichero no está, el proyecto se sigue compilando y `assembleRelease` deja la APK **sin
 * firmar**: es lo que hace falta para que cualquiera pueda compilar el repo sin la clave privada,
 * y así el aviso sale al final (la APK se llama `...-unsigned.apk`) en vez de reventar el build.
 */
val propiedadesFirma = Properties().apply {
    val fichero = rootProject.file("keystore.properties")
    if (fichero.exists()) {
        fichero.inputStream().use { load(it) }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    // `namespace` es el paquete **del código** (el del `R` generado y el de las clases). No se
    // cambia: mover los paquetes del código no aporta nada y toca miles de líneas.
    namespace = "com.example.virtualpetkmp"

    // Misma lectura que en `:shared`, para que los dos módulos compilen contra lo mismo: se
    // admite "37" y "37.0" (el SDK instala la API 37 en `platforms/android-37.0`, así que el
    // punto es lo natural) y se rechaza con un mensaje claro una versión menor distinta de cero
    // en vez de compilar en silencio contra otra plataforma.
    //
    // Antes esto era un `.toInt()` a secas, que reventaba con un NumberFormatException poco
    // claro en cuanto alguien escribía el punto.
    val apiCompilacion = libs.versions.android.compileSdk.get()
    require(apiCompilacion.substringAfter('.', "0") == "0") {
        "compileSdk = \"$apiCompilacion\": este proyecto solo compila con versión menor 0 " +
            "(escribe \"37\" o \"37.0\")"
    }
    compileSdk = apiCompilacion.substringBefore('.').toInt()

    defaultConfig {
        // `applicationId` es la **identidad de la app** para Android y para Play: el id del
        // paquete instalado, el de la ficha en la tienda y la clave de la firma. Es lo único que
        // cambia para publicar. Ojo: para Android, un `applicationId` distinto es **otra app**,
        // así que no hereda los datos de la instalación anterior (de ahí el traspaso con `.vpet`).
        applicationId = "io.github.eyemerald.virtualpet"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Se sube en cada entrega a los testers: Android deja instalar la nueva versión ENCIMA de
        // la que ya tienen (y conservar sus datos) solo si trae un versionCode mayor. Con el mismo
        // número también deja, pero el móvil no puede distinguir cuál tiene instalada.
        versionCode = 13
        versionName = "1.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (propiedadesFirma.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // Sin ofuscar: la app no tiene reflexión propia y, para una APK que se reparte a
            // mano, un fallo por R8 cuesta más de diagnosticar que el poco tamaño que ahorra.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
