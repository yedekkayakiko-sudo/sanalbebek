plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ortakyasam.spike"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ortakyasam.spike"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2-deneme"
    }

    // Sabit imza: her yeni APK eskisinin üstüne kurulabilsin (Android imzası değişince güncellemeyi reddeder).
    // Bu yalnızca deneme anahtarıdır; Play'e çıkarken gerçek anahtar kullanılır.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
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
