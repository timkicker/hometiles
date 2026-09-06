package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whatever may not wrap has to measure.
 *
 * `softWrap = false` with `maxLines = 1` and no ellipsis means what does not fit is **cut
 * off silently**. no sign, no exception, no red test - on the user's phone (03.09.2026) the
 * clock therefore read "2:33" instead of "2:36 AM" at 200 % text size.
 *
 * the size came from a calculation with an average character width of 0.60 of the type size.
 * that is an estimate, and it does not hold for a wider typeface - the same lesson that has
 * long stood as a comment beside the tile labels.
 *
 * this rule holds: a line that may not wrap gets its size from `fittedSingleLineDp`, which
 * measures with the style actually drawn and shrinks until it fits. the calculation may
 * deliver the *wish*, not the last word.
 */
class FittedTextTest {

    /**
     * lines with `softWrap = false`, with file and line number.
     *
     * searched with a pattern, not with the literal `"softWrap = false,"`. the first version
     * compared against that and hung on a comma: the last parameter of a call has none, and a
     * reformatting would have halved the rule silently. `countsEveryPlace` below guards it.
     */
    private val pattern = Regex("""softWrap\s*=\s*false""")

    private fun withoutWrap(): List<Triple<java.io.File, Int, List<String>>> =
        Quelltext.files().flatMap { file ->
            val lines = file.readLines()
            lines.withIndex()
                .filter { pattern.containsMatchIn(it.value) && !it.value.trim().startsWith("*") }
                .map { Triple(file, it.index, lines.subList(maxOf(0, it.index - 20), it.index)) }
        }

    /**
     * the rule sees every place the word occurs. without this counter-count there is no way
     * to notice the pattern missing a spelling: the rule stays green and checks less.
     */
    @Test
    fun `countsEveryPlace`() {
        val raw = Quelltext.files().sumOf { file ->
            file.readLines().count {
                "softWrap" in it && !Quelltext.isCommentLine(it)
            }
        }
        assertEquals("the pattern does not match every spelling of softWrap", raw, withoutWrap().size)
    }

    @Test
    fun `such lines exist at all`() {
        assertTrue("no line with softWrap = false left - the rule runs into nothing", withoutWrap().size >= 4)
    }

    @Test
    fun `no unwrappable line takes the estimated size`() {
        val hits = withoutWrap().filter { (_, _, before) ->
            before.any { "fontSize = dpSp(singleLineSizeSp(" in it || "fontSize = dpSp(\n" in it } &&
                before.none { "fittedSingleLineDp(" in it }
        }.map { (file, index, _) -> "${file.name}:${index + 1}" }
        assertEquals("estimates instead of measuring: $hits", emptyList<String>(), hits)
    }

    /** and each of them really measures - otherwise leaving out the calculation would do. */
    @Test
    fun `every unwrappable line measures`() {
        val without = withoutWrap()
            .filter { (file, _, _) -> file.name != "TextSizing.kt" }
            .filterNot { (_, _, before) -> before.any { "fittedSingleLineDp(" in it } }
            .map { (file, index, _) -> "${file.name}:${index + 1}" }
        assertEquals("does not measure: $without", emptyList<String>(), without)
    }
}
