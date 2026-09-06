package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * which tests may skip themselves?
 *
 * `Assume.assumeTrue` makes a test green without it running, and gradle says nothing about
 * it that would show in the summary. `RealConfigRoundTripTest` had never run since it was
 * written: the file it needs rightly does not lie in the repository. so moving a grown
 * configuration - the reason the backup exists - was unchecked.
 *
 * the test stays, because it sees more on the real file than any copy. but the list of those
 * that may skip themselves stands here, and every entry needs a reason.
 */
class SkippedTestsTest {

    /** file -> why this test may skip itself. */
    private val allowed = mapOf(
        "RealConfigRoundTripTest.kt" to
            "needs a phone's real configuration; that must not go into the repository. the " +
            "always-running copy beside it: GrownConfigTest.",
    )

    @Test
    fun `only named tests may skip themselves`() {
        val skipping = Quelltext.testFiles()
            // the rule itself writes down the name it looks for and would otherwise find
            // itself.
            .filter { it.name != "SkippedTestsTest.kt" }
            .filter { it.readText().contains("assumeTrue") }
            .map { it.name }
            .sorted()

        assertEquals(
            "a test that skips itself protects nothing. either it always runs, or it goes " +
                "into the list in SkippedTestsTest with a reason.",
            allowed.keys.sorted(),
            skipping,
        )
    }

    @Test
    fun `every skipped test has an always-running copy`() {
        val names = Quelltext.testFiles().map { it.name }.toSet()
        assertEquals(true, "GrownConfigTest.kt" in names)
    }
}
