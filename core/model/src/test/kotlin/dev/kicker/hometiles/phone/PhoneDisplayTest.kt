package dev.kicker.hometiles.phone

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * how a phone number stands on the screen.
 *
 * seen on the user's device (02.09.2026): an austrian mobile number stood there as
 * "+436 801 234 567". "+436" is no country - austria is "+43", and whoever reads that out
 * reads it wrong. the grouping into blocks of three knew no dialling codes, and that was
 * written down as guesswork without a library. the library does lie on the device.
 *
 * what is checked here is **the switch**, not the library: that the system's answer comes
 * first, that an empty answer falls back, and that the fallback stays the old one. there is
 * no android in a unit test; without that separation the rule would do something else on the
 * device than in the test.
 */
class PhoneDisplayTest {

    @After
    fun reset() {
        PhoneNumbers.systemFormat = { _, _ -> null }
        PhoneNumbers.region = null
    }

    @Test
    fun `the system's answer comes before the grouping`() {
        PhoneNumbers.systemFormat = { _, _ -> "+43 680 1234567" }
        assertEquals("+43 680 1234567", PhoneNumbers.forDisplay("+436801234567"))
    }

    @Test
    fun `the system gets the cleaned number and the country`() {
        var seen: Pair<String, String?>? = null
        PhoneNumbers.region = "at"
        PhoneNumbers.systemFormat = { number, country -> seen = number to country; null }
        PhoneNumbers.forDisplay("+43 (680) 1234567")
        assertEquals("+436801234567" to "at", seen)
    }

    @Test
    fun `an empty answer falls back to the grouping`() {
        PhoneNumbers.systemFormat = { _, _ -> "   " }
        assertEquals("+436 801 234 567", PhoneNumbers.forDisplay("+436801234567"))
    }

    @Test
    fun `without an answer the old fallback stands`() {
        PhoneNumbers.systemFormat = { _, _ -> null }
        assertEquals("069 912 345 6", PhoneNumbers.forDisplay("0699 123456"))
    }

    /** a letter identifier stays as it is - it was never a number. */
    @Test
    fun `a sender without digits stays what it is`() {
        PhoneNumbers.systemFormat = { _, _ -> "unsinn" }
        assertEquals("ADAC", PhoneNumbers.forDisplay(" ADAC "))
    }

    /** short numbers are not touched: 112 stays 112, even if the system offers something. */
    @Test
    fun `short numbers stay unchanged`() {
        PhoneNumbers.systemFormat = { _, _ -> "1 12" }
        assertEquals("112", PhoneNumbers.forDisplay("112"))
    }
}
