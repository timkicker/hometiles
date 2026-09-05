// :core:ui aus PLAN.md 2.1 - das Design-System: Farben, Schrift, Rastermasse, Haptik.
//
// Hier liegt, was **jeder** Bildschirm braucht und was keinen Text kennt. Die Schriftdateien
// wandern mit: `Fonts.kt` verweist auf sie, und eine Ressource, die woanders liegt als der
// Quelltext, der sie nennt, ist die naechste stille Falle.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

    namespace = "org.biglau.core.ui"
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
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":core:model"))
    // `Notice` fragt die Einstellung „Meldungen warten, bis du sie wegtippst".
    api(project(":core:data"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.material3)
    // Der Rückschritt-Pfeil auf dem Tastenfeld.
    implementation(libs.androidx.material.icons.extended)
    // `ContactAvatar` zeigt das Kontaktfoto; dieselbe Bibliothek, die :app schon nutzt.
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    // `BigLauActivity` ist die Grundlage jedes Bildschirms und hängt die Sprache um.
    api(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
