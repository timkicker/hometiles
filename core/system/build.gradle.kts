// :core:system from PLAN.md 2.1 - the reading side of the system.
//
// 2.1 calls it the *only* place that touches android framework apis directly. it is not
// there yet; for now these are the provider readers: LauncherApps, contacts, call log,
// telephony. they have no surface, which is why they could move first.
//
// "and no texts" stood here until 04.09.2026 and stopped being true that same day: with
// `CallDirectionLabels` the `values/` folders moved along. every rule that searches texts has
// to read this module too - `Quelltext.texts` does.
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

    namespace = "dev.kicker.hometiles.core.system"
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
