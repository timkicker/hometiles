package dev.kicker.hometiles.phone

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a number in the dialler while it is typed.
 *
 * issue #2: a us number typed nationally stood in bare blocks of three, the last digit on
 * its own. the system's spelling only knows complete numbers, so the as-you-type one comes
 * first here. as in PhoneDisplayTest only the switch is checked; the spelling itself lies on
 * the device, so the answers below are plain words rather than numbers.
 */
class PhoneTypingTest {

    @After
    fun reset() {
        PhoneNumbers.systemFormat = { _, _ -> null }
        PhoneNumbers.systemFormatTyping = { _, _ -> null }
        PhoneNumbers.region = null
    }

    @Test
    fun `the as-you-type spelling comes first`() {
        PhoneNumbers.systemFormat = { _, _ -> "complete spelling" }
        PhoneNumbers.systemFormatTyping = { _, _ -> "typed spelling" }
        assertEquals("typed spelling", PhoneNumbers.forTyping("15550101"))
    }

    @Test
    fun `it gets the cleaned number and the country`() {
        var seen: Pair<String, String?>? = null
        PhoneNumbers.region = "US"
        PhoneNumbers.systemFormatTyping = { number, country -> seen = number to country; null }
        PhoneNumbers.forTyping("1 555-0101")
        assertEquals("15550101" to "US", seen)
    }

    @Test
    fun `without an answer the display spelling stands`() {
        PhoneNumbers.systemFormat = { _, _ -> "complete spelling" }
        assertEquals("complete spelling", PhoneNumbers.forTyping("15550101"))
    }
}
