package org.biglau.ui

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * hide the label when it does not fit in two lines - `PLAN.md` 3.2.
 *
 * the plan promised the option and it did not exist. the first attempt to build it walked
 * into the very trap this project otherwise warns about: it **estimated** with an average
 * character width instead of measuring. checked on screen, the estimate would have hidden a
 * label that stands there in full, and called another one fitting although it is cut - two
 * faults in both directions, with a number that does not hold for atkinson at all.
 *
 * now `BigTile` measures with the `TextMeasurer` before drawing. what is left here is the
 * arithmetic of how much room there is.
 */
class LabelFitTest {

    @Test
    fun `the label gets the tile without its margins`() {
        // 166 dp wide, 186 dp tall: the margin is 186 * 0.06 = 11.16 dp per side.
        assertEquals(143.68f, labelWidthDp(166f, 186f), 0.01f)
    }

    /** the margin is capped, or it would eat half the width on tall tiles. */
    @Test
    fun `the margin stays between six and sixteen`() {
        assertEquals(166f - 12f, labelWidthDp(166f, 50f), 0.01f)
        assertEquals(166f - 32f, labelWidthDp(166f, 400f), 0.01f)
    }

    @Test
    fun `a narrow tile still leaves some width`() {
        assertTrue(labelWidthDp(10f, 186f) >= 1f)
    }

    /** and the measuring itself stands in the tile, not as an estimate beside it. */
    @Test
    fun `it measures with the TextMeasurer`() {
        val source = Quelltext.file("org/biglau/ui/BigTile.kt").readText()
        assertTrue("rememberTextMeasurer is gone", "rememberTextMeasurer()" in source)
        assertTrue("hasVisualOverflow is gone", "hasVisualOverflow" in source)
    }

    // --- shrink first, cut afterwards (02.09.2026) ---

    /**
     * at 200 % app type on 1.35x system type the tiles read as cut-off fragments. whoever
     * sets 200 % does not do it for fun - a cut word does not help them, a slightly smaller
     * one does.
     */
    @Test
    fun `the ladder starts at the wish and ends at seventy percent`() {
        val ladder = labelLadder(40f)
        assertEquals(40f, ladder.first(), 0.01f)
        assertEquals(28f, ladder.last(), 0.01f)
    }

    @Test
    fun `the ladder gets smaller step by step`() {
        val ladder = labelLadder(24f)
        ladder.zipWithNext().forEach { (large, small) ->
            assertTrue("$small should be smaller than $large", small < large)
        }
    }

    @Test
    fun `even the smallest step stays a size`() {
        // otherwise the label would be gone by arithmetic on tiny tiles instead of yielding -
        // and text at size zero is no text but a fault.
        labelLadder(14f).forEach { assertTrue(it > 0f) }
    }
}
