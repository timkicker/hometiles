package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a withheld number cannot be called back.
 *
 * found at the emulator: an entry without a number asked whether to call an empty pair of
 * quotes. `PhoneNumbers.isDialable` existed and was used in three other places - only not at
 * the one where a tap becomes a call.
 */
class CallBackTest {

    @Test
    fun `a withheld number is not dialable`() {
        assertFalse(PhoneNumbers.isDialable(""))
        assertFalse(PhoneNumbers.isDialable("   "))
        // this is how withheld numbers arrive on some networks.
        assertFalse(PhoneNumbers.isDialable("unknown"))
        assertFalse(PhoneNumbers.isDialable("-"))
    }

    @Test
    fun `an ordinary number stays dialable`() {
        assertTrue(PhoneNumbers.isDialable("+43660111001"))
        assertTrue(PhoneNumbers.isDialable("0660 111 001"))
    }

    /** without a number only the replacement text is left - the row must not be empty. */
    @Test
    fun `without number and name the replacement text stands there`() {
        val without = CallView(
            status = CallStatus.RINGING,
            number = "",
            name = null,
            startedAtMillis = null,
        )
        assertEquals("Unknown", CallActions.headline(without, "Unknown"))
    }
}
