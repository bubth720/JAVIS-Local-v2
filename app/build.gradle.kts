plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stark.jarvislocal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stark.jarvislocal"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    androidResources { noCompress += listOf("gguf", "bin") }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
    implementation("dev.ffmpegkit-maintained:whisper-android:1.0.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
