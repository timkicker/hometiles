package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die eigene gesendete Nachricht.
 *
 * Android legt sie nur dann von selbst ab, wenn die sendende App **nicht** die Standard-App
 * ist. Mit der Rolle verschwände sie sonst in dem Moment, in dem sie hinausgeht.
 */
class SmsOutboxTest {

    private val werte = SmsOutbox.values("+43664111001", "Bin um sechs da", 1_700_000_000_000L)

    @Test
    fun `Empfaenger, Text und Zeit stehen drin`() {
        assertEquals("+43664111001", werte["address"])
        assertEquals("Bin um sechs da", werte["body"])
        assertEquals(1_700_000_000_000L, werte["date"])
    }

    /** Sonst zählte die eigene Nachricht als neu und die Erinnerung erinnerte an sie. */
    @Test
    fun `die eigene Nachricht gilt als gelesen`() {
        assertEquals(1, werte["read"])
        assertEquals(1, werte["seen"])
    }
}
