// :core:model aus PLAN.md 2.1 - Datenklassen und ihre Serialisierung, sonst nichts.
//
// Das ist ein reines Kotlin-Modul, absichtlich **ohne** Android-Plugin: damit ist die
// Regel „keine Android-Abhaengigkeit" nicht aufgeschrieben, sondern vom Compiler
// durchgesetzt. Ein `import android.…` hier faellt beim Bauen um, nicht erst in einem Test.
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

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
