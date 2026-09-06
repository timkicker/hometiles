package dev.kicker.hometiles.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the search for the largest font size that still fits one line.
 *
 * it sits behind `fittedSingleLineDp`, where it cannot be checked - that would need a screen.
 * here it can be: the question "does this fit?" comes in as a function, and the test can
 * answer it and count along.
 */
class LargestFittingTest {

    @Test
    fun `if the wish fits, the wish stands`() {
        assertEquals(40f, largestFitting(40f, 12f) { true }, 0.01f)
    }

    @Test
    fun `if nothing fits, the floor stands`() {
        assertEquals(12f, largestFitting(40f, 12f) { false }, 0.01f)
    }

    @Test
    fun `it finds the largest fitting step`() {
        // everything up to 23 dp fits, above that nothing.
        assertEquals(23f, largestFitting(64f, 12f) { it <= 23f }, 0.01f)
    }

    /**
     * and with few questions. the clock changes its text every minute; step by step that
     * would be over forty text measurements down from 64 dp, halving it is a handful. the
     * number stands here so a step backwards shows up.
     */
    @Test
    fun `it takes few measurements`() {
        var questions = 0
        val result = largestFitting(64f, 12f) { questions++; it <= 23f }
        assertEquals(23f, result, 0.01f)
        assertTrue("too many measurements: $questions", questions <= 8)
    }

    @Test
    fun `a wish below the floor gives the floor`() {
        assertEquals(12f, largestFitting(10f, 12f) { true }, 0.01f)
    }
}
