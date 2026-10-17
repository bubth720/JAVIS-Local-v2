plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Selected source files are generated from CORE, never forked or imported as an APK.
val sharedCore by tasks.registering(Sync::class) {
    from(rootProject.file("app/src/main/java")) {
        include("com/stark/jarvislocal/InferenceContract.kt", "com/stark/jarvislocal/Models.kt")
    }
    into(layout.buildDirectory.dir("generated/core-contract"))
}
android {
    namespace = "com.stark.jarvislocal.fixture"
    compileSdk = 35
    defaultConfig {
        applicationId = "fr.jarvis.fixture.core"
        minSdk = 29
        targetSdk = 35
        buildConfigField("String", "CONTRACT_SHA", "\"fab6a66a6b99302884849cd1d8aef97cd761082dc20680a4b036d7bc99d5c3c9\"")
        versionCode = 1
        versionName = "0.0.1-experimental"
    }
    buildFeatures { aidl = true; buildConfig = true }
    // Unset means unsigned: never silently reuse the historical/debug signing identity.
    val fixtureKey = providers.environmentVariable("JARVIS_FIXTURE_KEYSTORE").orNull
    signingConfigs {
        if (fixtureKey != null) create("pilot") {
            storeFile = file(fixtureKey)
            storePassword = providers.environmentVariable("JARVIS_FIXTURE_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("JARVIS_FIXTURE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("JARVIS_FIXTURE_KEY_PASSWORD").orNull
        }
    }
    buildTypes.getByName("debug") { signingConfig = if (fixtureKey != null) signingConfigs.getByName("pilot") else null }
    sourceSets["main"].java.srcDir(sharedCore.map { it.destinationDir })
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
tasks.named("preBuild") { dependsOn(sharedCore) }
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
