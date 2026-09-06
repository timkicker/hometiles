package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `known emergency numbers are recognised`() {
        listOf("112", "911", "999", "110", "144", "122", "133").forEach {
            assertTrue("$it should have counted as an emergency number", PhoneNumbers.looksLikeEmergency(it))
        }
    }

    @Test
    fun `spellings with separators count too`() {
        assertTrue(PhoneNumbers.looksLikeEmergency("1-1-2"))
        assertTrue(PhoneNumbers.looksLikeEmergency(" 112 "))
        assertTrue(PhoneNumbers.looksLikeEmergency("+112"))
    }

    @Test
    fun `ordinary numbers are no emergency`() {
        listOf("+436601234567", "0512999888", "1122", "11", "").forEach {
            assertTrue("$it must not have been an emergency number", !PhoneNumbers.looksLikeEmergency(it))
        }
    }

    @Test
    fun `the platform is always right`() {
        // when android says emergency we believe it - even for a number not on the list.
        assertTrue(PhoneNumbers.looksLikeEmergency("0800112", platformSaysYes = true))
    }

    @Test
    fun `dialling characters are kept`() {
        assertEquals("*100#", PhoneNumbers.clean("*100#"))
        assertEquals("+436601234567", PhoneNumbers.clean("+43 660 123 45 67"))
    }

    @Test
    fun `letters fly out`() {
        assertEquals("0512", PhoneNumbers.clean("0512 (Innsbruck)"))
    }

    @Test
    fun `only what has a digit is dialable`() {
        assertTrue(PhoneNumbers.isDialable("112"))
        assertTrue(!PhoneNumbers.isDialable(""))
        assertTrue(!PhoneNumbers.isDialable("***"))
        assertTrue(!PhoneNumbers.isDialable("no number"))
    }

    @Test
    fun `long numbers are grouped in threes for reading`() {
        // deliberately without country-code logic: without a library that would be
        // guesswork and wrong on every second foreign number.
        assertEquals("+436 601 234 567", PhoneNumbers.forDisplay("+436601234567"))
        assertEquals("051 299 988 8", PhoneNumbers.forDisplay("0512999888"))
    }

    @Test
    fun `short numbers stay in one piece`() {
        assertEquals("112", PhoneNumbers.forDisplay("112"))
        assertEquals("123456", PhoneNumbers.forDisplay("123456"))
    }

    // --- senders that are no number (02.09.2026) ---

    /**
     * banks, parcel services and sign-in codes come as a letter id. `clean` leaves nothing of
     * them - so an empty row stood in the message list, on exactly the messages one looks for
     * most.
     */
    @Test
    fun `a letter id stays readable`() {
        assertEquals("ADAC", PhoneNumbers.forDisplay("ADAC"))
        assertEquals("Bank Austria", PhoneNumbers.forDisplay("Bank Austria"))
    }

    @Test
    fun `space around an id falls away`() {
        assertEquals("ADAC", PhoneNumbers.forDisplay("  ADAC  "))
    }

    @Test
    fun `a mixed id shows its digits`() {
        assertEquals("22580", PhoneNumbers.forDisplay("Info-22580"))
    }

    @Test
    fun `with nothing at all it stays empty`() {
        assertEquals("", PhoneNumbers.forDisplay(""))
        assertEquals("", PhoneNumbers.forDisplay("   "))
    }

    @Test
    fun `ordinary numbers stay as they were`() {
        assertEquals("+436 641 110 01", PhoneNumbers.forDisplay("+43664111001"))
        assertEquals("112", PhoneNumbers.forDisplay("112"))
    }
}
