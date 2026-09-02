// :core:data aus PLAN.md 2.1 - die gespeicherte Konfiguration.
//
// Anders als :core:model ist das ein Android-Modul, und zwar aus genau einem Grund:
// ConfigStore braucht `Context`, um an `filesDir` zu kommen. Alles, was ohne Android
// auskommt - das Modell selbst, das Lesen und Schreiben der Datei, die Sicherung -,
// bleibt drueben, wo der Compiler die Reinheit erzwingt.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.biglau.core.data"
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
}

dependencies {
    api(project(":core:model"))
    // Der StateFlow und der Schreib-Mutex. In :app kam das bisher nur zufaellig ueber
    // androidx mit; ein Modul, das es benutzt, nennt es selbst.
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
