plugins {
    id("com.android.application") version "8.3.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.caam.nothingelse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.caam.nothingelse"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Configure signing from environment variables if provided
            // KEYSTORE_FILE - path to keystore file
            // KEYSTORE_PASSWORD - store password
            // KEY_ALIAS - key alias
            // KEY_PASSWORD - key password
            signingConfig = signingConfigs.create("envRelease").apply {
                // Only set storeFile if KEYSTORE_FILE is present in env
                val ksFile = System.getenv("KEYSTORE_FILE")
                if (!ksFile.isNullOrEmpty()) {
                    storeFile = file(ksFile)
                }
                val storePass = System.getenv("KEYSTORE_PASSWORD")
                if (!storePass.isNullOrEmpty()) {
                    storePassword = storePass
                }
                val alias = System.getenv("KEY_ALIAS")
                if (!alias.isNullOrEmpty()) {
                    keyAlias = alias
                }
                val keyPass = System.getenv("KEY_PASSWORD")
                if (!keyPass.isNullOrEmpty()) {
                    keyPassword = keyPass
                }
            }
        }
    }

    buildFeatures {
        // Enable Jetpack Compose compiler integration
        compose = true
    }

    composeOptions {
        // Align Compose Compiler with Kotlin 1.9.22 per Android compatibility table
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("com.google.android.material:material:1.10.0")
}
