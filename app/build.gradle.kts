import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// the round trip test against a real configuration gets its path handed through.
tasks.withType<Test> {
    System.getenv("HOMETILES_REAL_CONFIG")?.let { environment("HOMETILES_REAL_CONFIG", it) }
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

    namespace = "dev.kicker.hometiles"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.kicker.hometiles"
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
            // deliberately unsigned: a release signed with the debug key would be a lie
            // about where it came from. F-Droid signs its own builds, everybody else brings
            // their own key.
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
    // puts the shipped startup profile (src/main/baselineProfiles/) into the app's profile
    // file on first launch. without this library the profile does lie in the archive, and up
    // to android 8 the installer takes it itself - on the target device (android 11) it does
    // not.
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}

/**
 * nearly half the rules in `:app` read files rather than behaviour: the manifest, the texts,
 * the source of the other modules. gradle knows nothing of that - for the test task only the
 * class paths count. measured on 03.09.2026: a deliberately broken manifest let its rule pass
 * as `UP-TO-DATE`, green without running. exactly the silent erosion these rules exist
 * against.
 *
 * so the files they really read stand here.
 */
tasks.withType<Test>().configureEach {
    inputs.file("src/main/AndroidManifest.xml").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../README.md").withPathSensitivity(PathSensitivity.RELATIVE)
    // `DeviceMetricsTest` reads the plan - the same hole as on 03.09.2026 with the README
    // and the manifest: gradle held the run to be current although the file it reads had
    // changed. added at 10:18, after the plan could be changed without a single test running
    // again.
    // on 04.09.2026 the same hole one file further: `ProseDashesTest` reads STATUS.md and
    // README.md, neither of which stood here. an entry in STATUS.md, then a test run, and
    // gradle reported the success from before. the rule did not run. the lesson from 10:18
    // was written down but applied only to the one file where it showed.
    inputs.file("../PLAN.md").withPathSensitivity(PathSensitivity.RELATIVE)
    // the working log is the one input that may be absent: it is not in the repository, so
    // a fresh clone has none. declared as required it stopped the whole task before a single
    // rule ran - the first CI run on 06.09.2026 failed exactly there, on a machine that had
    // never seen the file.
    // the working log is the one input that may be absent: it is not in the repository, so
    // a fresh clone has none. declared as a plain input it stopped the whole task before a
    // single rule ran - the first CI run on 06.09.2026 failed exactly there, on a machine
    // that had never seen the file. `optional(true)` was not enough, gradle still checked
    // it; so it is registered only where it lies. where it lies, changing it re-runs the
    // rules, which is what the entry was for.
    rootProject.file("STATUS.md").takeIf { it.isFile }?.let {
        inputs.file(it).withPathSensitivity(PathSensitivity.RELATIVE)
    }
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
