package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * the keypad leads the focus itself and lets it out only towards a named anchor.
 *
 * the dialer passed none: by direction key the call row and the call log were out of reach
 * in 0.1.16 already, and the contacts of issue #23 beside or above the keys as well
 * (emulator, 23.11.2026).
 */
class KeypadExitsTest {

    private val dialer = File(Quelltext.appRoot, "dev/kicker/hometiles/phone/DialerActivity.kt").readText()
    private val keypad = File("../core/ui/src/main/kotlin/dev/kicker/hometiles/ui/BigKeypad.kt").readText()

    @Test
    fun `the keypad lets the focus out at the top and on the left`() {
        assertTrue(keypad.contains("above?.let { runCatching { it.requestFocus() } }"))
        assertTrue(keypad.contains("before?.let { runCatching { it.requestFocus() } }"))
    }

    @Test
    fun `down leads to the call row or without a number to the call log`() {
        assertTrue(dialer.contains("below = if (dialable) callRow else logButton"))
        assertTrue(dialer.contains("Modifier.focusRequester(logButton)"))
    }

    @Test
    fun `up and left lead to the contacts where they stand`() {
        assertTrue(dialer.contains("above = if (changeable) changeRow else above"))
        assertTrue(dialer.contains("above = if (side == KeypadSide.ABOVE) listEntry else null"))
        assertTrue(dialer.contains("before = if (side == KeypadSide.BESIDE) listEntry else null"))
        assertTrue(dialer.contains("focusRequester(listEntry).focusGroup()"))
    }
}
