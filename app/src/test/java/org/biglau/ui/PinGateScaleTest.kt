package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * on the pin entry the keys count for more than the words.
 *
 * at 200 percent text size heading and confirm button grew so far that only a strip was left
 * for the keypad: the digit keys measured **21 dp** high. on a screen where one has to hit
 * exactly, and for someone who did not set 200 percent for fun. with the cap they are 58 dp.
 */
class PinGateScaleTest {

    @Test
    fun `large settings are capped`() {
        assertEquals(PIN_MAX_TEXT_SCALE, pinTextScale(2.0f), 0.001f)
        assertEquals(PIN_MAX_TEXT_SCALE, pinTextScale(1.5f), 0.001f)
    }

    @Test
    fun `up to the cap the setting holds`() {
        assertEquals(1.0f, pinTextScale(1.0f), 0.001f)
        assertEquals(1.25f, pinTextScale(1.25f), 0.001f)
    }

    @Test
    fun `it never gets smaller than set`() {
        // whoever chose 75 percent gets 75 percent - the cap is an upper bound, not a default.
        assertEquals(0.75f, pinTextScale(0.75f), 0.001f)
    }
}
