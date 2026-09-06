package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the shipped startup profile is there, and it describes the start.
 *
 * freshly installed android compiles nothing: the app stands at `run-from-apk` and
 * interprets every line on the first start. measured on the Jelly 2 on 04.09.2026 - five
 * starts each with `am start -W -S`, median: **3018 ms** that way, **688 ms** on `quicken`.
 * that is the distance this is about; `StartupNumbersTest` holds what belongs to such a
 * number.
 *
 * two things can break silently: the file lands in the wrong place (AGP 8 reads
 * `src/main/baselineProfiles/`, no longer `src/main/baseline-prof.txt`), or
 * `profileinstaller` is missing and the profile then lies in the archive but is never
 * installed on android 9 to 11.
 */
class StartupProfileTest {

    private val profile: List<File> = File("src/main/baselineProfiles")
        .listFiles { d -> d.extension == "txt" }?.toList().orEmpty()

    private val rules: List<String> = profile
        .flatMap { it.readLines() }
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }

    @Test
    fun `there is a startup profile where AGP 8 reads`() {
        assertTrue(
            "no startup profile in src/main/baselineProfiles/ - AGP 8 reads only there, " +
                "src/main/baseline-prof.txt is silently ignored",
            profile.isNotEmpty(),
        )
        assertTrue("startup profile without rules", rules.size >= 10)
    }

    @Test
    fun `the entry stands in the profile`() {
        listOf("MainActivity", "HomeTilesApp").forEach { name ->
            assertTrue(
                "$name is missing from the startup profile - that is exactly the start",
                rules.any { name in it },
            )
        }
    }

    @Test
    fun `the profile describes only our own source`() {
        val foreign = rules.filterNot { it.contains("dev/kicker/hometiles/") }
        assertEquals(
            "foreign packages in our own startup profile. the libraries bring their " +
                "profiles themselves (compose does); kept twice it becomes a list nobody " +
                "keeps up to date.",
            emptyList<String>(),
            foreign,
        )
    }

    @Test
    fun `profileinstaller is wired in`() {
        val build = File("build.gradle.kts").readText()
        assertTrue(
            "without androidx.profileinstaller the profile lies in the archive and is " +
                "never installed on android 9 to 11 - so never on the target device.",
            "profileinstaller" in build,
        )
    }
}
