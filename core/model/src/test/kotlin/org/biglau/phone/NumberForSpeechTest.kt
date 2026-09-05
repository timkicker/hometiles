package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a phone number is not a number.
 *
 * read as ordinary text a screen reader turns "123" into "one hundred and twenty-three" -
 * and whoever wants to check the number before dialling cannot. the dial pad shows the typed
 * number as a plain `Text` without a description of its own.
 *
 * digit by digit with spaces is the version every screen reader reads singly.
 */
class NumberForSpeechTest {

    @Test
    fun `every digit stands for itself`() {
        assertEquals("1 2 3", PhoneNumbers.forSpeech("123"))
        assertEquals("0 6 6 4 1 1 1 0 0 1", PhoneNumbers.forSpeech("0664111001"))
    }

    @Test
    fun `the grouping for the eye falls away`() {
        // the gaps are a reading aid for the sighted; the ear gets a pause after every digit
        // anyway. were the grouping to stay, a screen reader would read the blocks as
        // numbers again.
        assertEquals(
            PhoneNumbers.forSpeech("0664111001"),
            PhoneNumbers.forSpeech(PhoneNumbers.forDisplay("0664111001")),
        )
    }

    @Test
    fun `the plus stays`() {
        assertEquals("+ 4 3 6 6 4", PhoneNumbers.forSpeech("+43664"))
    }

    @Test
    fun `a letter identifier stays a word`() {
        // "ADAC" is no number but a sender. read letter by letter it would be
        // incomprehensible - and `clean` leaves nothing of it anyway, so the text itself
        // stands there. the same rule as in forDisplay.
        assertEquals("ADAC", PhoneNumbers.forSpeech("ADAC"))
    }

    @Test
    fun `nothing stays nothing`() {
        assertEquals("", PhoneNumbers.forSpeech(""))
    }
}
