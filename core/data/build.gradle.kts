// :core:data from PLAN.md 2.1 - the stored configuration.
//
// unlike :core:model this is an android module, for exactly one reason: ConfigStore needs a
// `Context` to reach `filesDir`. everything that works without android - the model itself,
// reading and writing the file, the backup - stays over there, where the compiler enforces
// the purity.
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
    // the state flow and the write mutex. in :app this came along by accident through
    // androidx; a module that uses it names it itself.
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
