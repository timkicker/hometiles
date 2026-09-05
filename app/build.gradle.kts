import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Der Round-Trip-Test gegen eine echte Konfiguration bekommt ihren Pfad durchgereicht.
tasks.withType<Test> {
    System.getenv("BIGLAU_REAL_CONFIG")?.let { environment("BIGLAU_REAL_CONFIG", it) }
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

    namespace = "org.biglau"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.biglau"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        resourceConfigurations += listOf("en", "de", "fr", "es", "it")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Bewusst keine Signatur: ein mit dem Debug-Schluessel signiertes Release waere
            // eine Luege ueber seine Herkunft. F-Droid signiert selbst, alle anderen sollen
            // ihren eigenen Schluessel eintragen.
        }
        debug {
            applicationIdSuffix = ".debug"
        }
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:system"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    // Legt das mitgelieferte Startprofil (src/main/baseline-prof.txt) beim ersten Start in
    // die Profildatei der App. Ohne diese Bibliothek liegt das Profil zwar im Archiv, und
    // bis Android 8 nimmt es der Installer selbst - auf dem Zielgerät (Android 11) nicht.
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}

/**
 * Knapp die Hälfte der Tests in `:app` liest Dateien statt Verhalten: das Manifest, die
 * Texte, den Quelltext der anderen Module. Gradle weiss davon nichts — für die Testaufgabe
 * zählen nur die Klassenpfade. Am 3.9.2026 gemessen: ein absichtlich kaputtgemachtes
 * Manifest liess die Regel dazu `UP-TO-DATE` durchgehen, also grün, ohne zu laufen. Genau
 * die Sorte stiller Erosion, gegen die diese Regeln geschrieben wurden.
 *
 * Deshalb stehen hier die Verzeichnisse, die sie wirklich lesen.
 */
tasks.withType<Test>().configureEach {
    inputs.file("src/main/AndroidManifest.xml").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../README.md").withPathSensitivity(PathSensitivity.RELATIVE)
    // `DeviceMetricsTest` liest den Plan - dieselbe Luecke wie am 03.09.2026 bei README und
    // Manifest: Gradle hielt den Testlauf fuer aktuell, obwohl sich die gelesene Datei
    // geaendert hatte. Am 10:18 nachgetragen, nachdem ich den Plan aendern konnte, ohne dass
    // ein einziger Test noch einmal lief.
    // Am 04.09.2026 dieselbe Luecke eine Datei weiter: `ProsaStricheTest` liest STATUS.md
    // und README.md, beide standen hier nicht. Ein Eintrag in STATUS.md, danach ein Testlauf,
    // und Gradle meldete den Erfolg von vorhin. Die Regel lief nicht. Die Lehre von 10:18
    // war notiert, aber nur fuer die eine Datei angewendet, an der sie auffiel.
    inputs.file("../PLAN.md").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../STATUS.md").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../README.md").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../gradle/libs.versions.toml").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../LICENSE-Atkinson-Hyperlegible.txt").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/baselineProfiles").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("../tools").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("../fastlane").withPathSensitivity(PathSensitivity.RELATIVE)
    listOf(
        "src/main/res",
        "../core/model/src/main/kotlin",
        "../core/data/src/main/kotlin",
        "../core/system/src/main/kotlin",
        "../core/ui/src/main/kotlin",
        "../core/ui/src/main/res",
    ).forEach { ort ->
        inputs.dir(ort).withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
