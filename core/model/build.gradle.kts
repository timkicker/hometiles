// :core:model from PLAN.md 2.1 - data classes and their serialisation, nothing else.
//
// a plain kotlin module, deliberately **without** the android plugin: that way the rule "no
// android dependency" is not written down but enforced by the compiler. an `import android.`
// here falls over while building, not in a test later.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// the round trip test against a real configuration gets its path handed through - the same
// hand-through as in :app, since the test moved here with the model.
tasks.withType<Test> {
    System.getenv("HOMETILES_REAL_CONFIG")?.let { environment("HOMETILES_REAL_CONFIG", it) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
