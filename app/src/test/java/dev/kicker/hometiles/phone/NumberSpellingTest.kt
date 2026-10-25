package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the system half of the number spelling is hooked up the way it works.
 *
 * issue #2, measured on the emulator with sim country "us" on 25.10.2026: a us number typed
 * with +1 came out in the us spelling, the same number typed nationally in bare blocks of
 * three with the last digit alone. the sim reports its country in lower case and the
 * dialling-code tables only know "US", and the dialler used the spelling for complete
 * numbers on a number being typed.
 */
class NumberSpellingTest {

    private val system = Quelltext.file("dev/kicker/hometiles/phone/SystemNumbers.kt").readLines()
        .filterNot { Quelltext.isCommentLine(it) }.joinToString("\n")
    private val dialler = Quelltext.file("dev/kicker/hometiles/phone/DialerActivity.kt").readText()

    @Test
    fun `the sim country goes in upper case`() {
        assertTrue("simCountryIso is passed on as it comes", ".uppercase(Locale.ROOT)" in system)
    }

    @Test
    fun `the as-you-type spelling is hooked up`() {
        assertTrue("SystemNumbers does not set systemFormatTyping", "PhoneNumbers.systemFormatTyping =" in system)
        assertTrue("the dialler shows the typed number with the display spelling", "PhoneNumbers.forTyping(typed)" in dialler)
    }
}
