package org.biglau.ui

import org.biglau.phone.INCALL_MAX_TEXT_SCALE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * screens that do not scroll cap the text size.
 *
 * seen twice at the emulator, both times at 200 %: the pin keys were still 21 dp high, and the
 * speaker button was cut off mid-word. the cap works upward only, and it is no substitute for
 * a label that is too long anyway.
 */
class TextScaleCapTest {

    @Test
    fun `upward it is capped`() {
        assertEquals(1.25f, cappedTextScale(2.0f, 1.25f), 0.001f)
        assertEquals(1.5f, cappedTextScale(2.0f, 1.5f), 0.001f)
    }

    @Test
    fun `downward never`() {
        assertEquals(0.75f, cappedTextScale(0.75f, 1.25f), 0.001f)
        assertEquals(1.0f, cappedTextScale(1.0f, 1.25f), 0.001f)
    }

    @Test
    fun `the caps of the fixed screens stay below the largest step`() {
        // 2.0 is the largest selectable step; a cap above it would be none.
        assertTrue(PIN_MAX_TEXT_SCALE < 2.0f)
        assertTrue(INCALL_MAX_TEXT_SCALE < 2.0f)
    }
}
