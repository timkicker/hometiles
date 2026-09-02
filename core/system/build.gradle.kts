// :core:system aus PLAN.md 2.1 - die Leseseite des Systems.
//
// 2.1 nennt es die *einzige* Stelle, die Android-Framework-APIs direkt anfasst. So weit ist
// es noch nicht; hier stehen zunaechst die Anbieter-Leser: LauncherApps, Kontakte,
// Anrufliste, Telephony. Sie haben keine Oberflaeche und keine Texte - deshalb koennen sie
// zuerst gehen, ohne dass Ressourcen mitwandern muessen.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.biglau.core.system"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
