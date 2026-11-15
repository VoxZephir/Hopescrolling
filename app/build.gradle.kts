plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
        val donate = (project.findProperty("hopescrolling.donateUrl") as String?).orEmpty()
        buildConfigField("String", "DONATE_URL", "\"$donate\"")
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
        // Clé d'envoi Play Store : jamais dans le dépôt, fournie par la CI
        // (secrets GitHub) ou par ta machine via des variables d'environnement.
        System.getenv("UPLOAD_KEYSTORE_PATH")?.let { path ->
            create("upload") {
                storeFile = file(path)
                storePassword = System.getenv("UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // Deux versions :
    //  - full : GitHub / Obtainium, tout débloqué, sans paiement ni Internet ;
    //  - play : Play Store, avec Premium (Google Play Billing).
    flavorDimensions += "store"
    productFlavors {
        create("full") {
            dimension = "store"
            buildConfigField("boolean", "REQUIRE_PREMIUM", "false")
            signingConfig = signingConfigs.getByName("debug")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "REQUIRE_PREMIUM", "true")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }

    buildTypes {
        // La CI compile la version "release" : optimisée par R8 (plus petite et
        // plus fluide), signée avec la même clé que les versions précédentes.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    // Icônes Material : R8 ne garde que celles utilisées.
    implementation("androidx.compose.material:material-icons-extended")
    // Paiement Premium : uniquement dans la version Play Store.
    "playImplementation"("com.android.billingclient:billing-ktx:7.1.1")
}
