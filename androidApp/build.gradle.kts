plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
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
        applicationId = "com.example.virtualpetkmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
