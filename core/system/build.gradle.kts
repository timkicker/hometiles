// :core:system aus PLAN.md 2.1 - die Leseseite des Systems.
//
// 2.1 nennt es die *einzige* Stelle, die Android-Framework-APIs direkt anfasst. So weit ist
// es noch nicht; hier stehen zunaechst die Anbieter-Leser: LauncherApps, Kontakte,
// Anrufliste, Telephony. Sie haben keine Oberflaeche - deshalb konnten sie zuerst gehen.
//
// "Und keine Texte" stand hier bis zum 04.09.2026 und stimmt seit demselben Tag nicht mehr:
// mit `CallDirectionLabels` sind `values/` und `values-de/` mitgewandert. Jede Regel, die
// Texte sucht, muss dieses Modul also mitlesen - `Quelltext.texte` tut das.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    // What lint finds today is written down in `lint-baseline.xml`, so that a **new**
    // finding turns the build red instead of drowning in the old ones. The baseline is not
    // an excuse: what stands in it is listed in PLAN.md P8 as work, and the french plural
    // forms in it are a job for the native review.
    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = false
        abortOnError = true
    }

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
