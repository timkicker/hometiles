package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every foreign library stands there by name, with a reason.
 *
 * the README promises open source, without an account, without ads, without network access.
 * the missing `INTERNET` permission secures the last part (`SlopRulesTest`); a library for
 * crash reports or usage numbers comes in through a line in `libs.versions.toml`, which
 * nobody reads.
 *
 * hence the complete list here. whatever is added makes this fall over, and whoever adds it
 * writes what for beside it.
 */
class DependenciesTest {

    private val catalogue = File("../gradle/libs.versions.toml").readText()

    /** alias -> what for. */
    private val allowed = mapOf(
        "androidx-core-ktx" to "the android framework's basics",
        "androidx-lifecycle-runtime-ktx" to "the screens' lifecycle",
        "androidx-lifecycle-viewmodel-compose" to "state across a turn of the device",
        "androidx-activity-compose" to "compose inside an activity",
        "androidx-compose-bom" to "holds the compose versions together",
        "androidx-ui" to "compose itself - layout, input, drawing",
        "androidx-ui-graphics" to "colours and shapes",
        "androidx-ui-tooling" to "debug build only: the preview in the studio",
        "androidx-ui-tooling-preview" to "the @Preview annotation",
        "androidx-material3" to "material 3 - the design system underneath",
        "androidx-material-icons-extended" to "the icons on the tiles",
        "kotlinx-coroutines-core" to "concurrency without hand-made threads",
        "kotlinx-serialization-json" to "reading and writing the configuration file",
        "coil-compose" to "contact photos; brings OkHttp along, see PLAN.md P8",
        "androidx-profileinstaller" to "installs the startup profile, see PLAN.md P8",
        "junit" to "the tests - well over a thousand of them",
    )

    /** what must never appear in this app. */
    private val forbidden = listOf(
        "firebase", "crashlytics", "analytics", "admob", "ads", "gms", "facebook",
        "sentry", "appcenter", "mixpanel", "amplitude", "adjust", "onesignal",
    )

    private fun aliases(): List<String> {
        val part = Quelltext.cut(catalogue, "[libraries]", "[plugins]")
        return Regex("""^([a-z0-9-]+)\s*=\s*\{""", RegexOption.MULTILINE)
            .findAll(part).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `no library stands in the catalogue that does not stand here`() {
        assertEquals(
            "a foreign library has been added. in this app that is a decision, not a " +
                "detail: into the list in DependenciesTest with a reason, or out again.",
            allowed.keys.sorted(),
            aliases().sorted(),
        )
    }

    @Test
    fun `nothing for ads, accounts or usage numbers`() {
        val hits = forbidden.filter { it in catalogue.lowercase() }
        assertEquals(
            "the README promises: without an account, without ads. this breaks it.",
            emptyList<String>(),
            hits,
        )
    }

    @Test
    fun `every library is used as well`() {
        val buildFiles = File("..").walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.name == "build.gradle.kts" }
            .joinToString("\n") { it.readText() }
        val unused = aliases().filterNot { alias ->
            "libs.${alias.replace('-', '.')}" in buildFiles
        }
        assertEquals(
            "stands in the catalogue and is used by no module - a leftover",
            emptyList<String>(),
            unused,
        )
    }

    @Test
    fun `every reason is one`() {
        allowed.forEach { (alias, reason) ->
            assertTrue("$alias: reason too short", reason.length > 15)
        }
    }
}
