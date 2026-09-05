// :core:ui from PLAN.md 2.1 - the design system: colours, typeface, grid measures, haptics.
//
// what **every** screen needs and what knows no text. the font files travel with it:
// `Fonts.kt` points at them, and a resource lying somewhere other than the source naming it
// is the next silent trap.
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
    // the backspace arrow on the keypad.
    implementation(libs.androidx.material.icons.extended)
    // `ContactAvatar` shows the contact photo; the same library :app already uses.
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    // `BigLauActivity` is the base of every screen and switches the language.
    api(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
