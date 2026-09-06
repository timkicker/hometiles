package dev.kicker.hometiles.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a spoken name works without a state too.
 *
 * `BigRow` can replace a name (`labelSpeech`) for a label with something drawn in it. until
 * 04.09.2026 the semantics block hung on the state alone: a row with a replaced name but
 * without a state got **no** `contentDescription` at all, and the name passed in fell away
 * silently. a parameter that does nothing in half the cases is worse than none - people rely
 * on it.
 */
class SpokenNameTest {

    private val source = File("src/main/kotlin/dev/kicker/hometiles/ui/BigRow.kt").readText()

    @Test
    fun `the announcement does not hang on the state alone`() {
        val branch = source.lines()
            .dropWhile { !it.contains("val announcement = when") }
            .takeWhile { !it.trimStart().startsWith("Row(") }
            .joinToString("\n")
        assertTrue(
            "BigRow has no branch that announces the replaced name without a state:\n" + branch,
            "labelSpeech != null" in branch,
        )
    }

    @Test
    fun `the semantics block hangs on the announcement`() {
        assertTrue(
            "the semantics block still checks the state instead of the announcement - then " +
                "labelSpeech without a state stays without effect.",
            "if (announcement != null) {" in source,
        )
    }
}
