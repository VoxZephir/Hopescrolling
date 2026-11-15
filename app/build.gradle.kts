plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.hopescrolling"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.hopescrolling"
        minSdk = 26
        targetSdk = 35
        // Fourni par la CI (numéro du run) ; 1 pour une compilation locale.
        val ciVersion = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionCode = ciVersion
        versionName = "0.$ciVersion"
    }

    signingConfigs {
        // Clé de debug versionnée : chaque APK compilé par la CI a la même
        // signature, donc on peut installer les mises à jour par-dessus.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
