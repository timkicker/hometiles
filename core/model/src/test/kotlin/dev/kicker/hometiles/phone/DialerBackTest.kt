package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/** issue #2: back on a call log opened from its own tile led to the keypad. */
class DialerBackTest {

    @Test
    fun `a log opened from its tile is left with back`() {
        assertEquals(
            DialerBackStep.LEAVE,
            DialerBack.step(questionOpen = false, onKeypad = false, onLog = true, openedOnLog = true),
        )
    }

    @Test
    fun `a log reached from the keypad goes back to the keypad`() {
        assertEquals(
            DialerBackStep.TO_KEYPAD,
            DialerBack.step(questionOpen = false, onKeypad = false, onLog = true, openedOnLog = false),
        )
    }

    @Test
    fun `an open question closes first`() {
        assertEquals(
            DialerBackStep.CLOSE_QUESTION,
            DialerBack.step(questionOpen = true, onKeypad = false, onLog = true, openedOnLog = true),
        )
    }

    // the speed dial page is only reached from the keypad, whatever opened the dialler.
    @Test
    fun `the speed dial page goes back to the keypad`() {
        assertEquals(
            DialerBackStep.TO_KEYPAD,
            DialerBack.step(questionOpen = false, onKeypad = false, onLog = false, openedOnLog = true),
        )
    }

    @Test
    fun `the keypad is left with back`() {
        assertEquals(
            DialerBackStep.LEAVE,
            DialerBack.step(questionOpen = false, onKeypad = true, onLog = false, openedOnLog = false),
        )
    }
}
