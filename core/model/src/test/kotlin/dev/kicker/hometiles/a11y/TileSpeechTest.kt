package dev.kicker.hometiles.a11y

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * what a tile announces, PLAN.md 3.6.
 *
 * the occasion stands in [TileSpeech]: the notification count and the signal bars were drawn
 * and silent.
 */
class TileSpeechTest {

    @Test
    fun `a label alone stays the label`() {
        assertEquals("Phone", TileSpeech.describe("Phone"))
    }

    @Test
    fun `state and count come after the label`() {
        assertEquals(
            "Signal. 3 of 4 bars. 2 new notices",
            TileSpeech.describe("Signal", "3 of 4 bars", "2 new notices"),
        )
    }

    /** without this it would say "Phone. . 2 new notices" - the screen reader reads the gap. */
    @Test
    fun `empty parts fall away`() {
        assertEquals("Phone. 2 new notices", TileSpeech.describe("Phone", "  ", "2 new notices"))
        assertEquals("Phone", TileSpeech.describe("Phone", null, null))
        assertEquals("Phone", TileSpeech.describe("Phone", "", ""))
    }

    /** a text already ending in a full stop gets no second one. */
    @Test
    fun `no double full stop`() {
        assertEquals("Battery. 84 %", TileSpeech.describe("Battery", "84 %."))
    }
}
