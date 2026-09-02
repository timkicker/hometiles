package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eingehende SMS gehen nicht verloren.
 *
 * Am Emulator nachgestellt (02.09.2026): BigLau die SMS-Rolle gegeben, eine Nachricht
 * geschickt — und sie war nirgends. Android stellt `SMS_DELIVER` nur der Standard-App zu,
 * und wer die Rolle hält, muss selbst speichern.
 */
class SmsDeliveryTest {

    private fun teil(a: String, b: String, t: Long = 1L) = SmsDelivery.Part(a, b, t)

    @Test
    fun `eine einzelne Nachricht bleibt eine`() {
        val ganz = SmsDelivery.merge(listOf(teil("+43664", "Hallo")))
        assertEquals(1, ganz.size)
        assertEquals("Hallo", ganz.first().body)
    }

    /** Sonst stünden drei halbe Nachrichten untereinander statt einer ganzen. */
    @Test
    fun `Teile einer langen Nachricht kommen wieder zusammen`() {
        val ganz = SmsDelivery.merge(
            listOf(teil("+43664", "Erster Teil ", 100), teil("+43664", "und zweiter.", 200)),
        )
        assertEquals(1, ganz.size)
        assertEquals("Erster Teil und zweiter.", ganz.first().body)
        assertEquals(100L, ganz.first().timestamp)
    }

    @Test
    fun `verschiedene Absender bleiben getrennt`() {
        val ganz = SmsDelivery.merge(listOf(teil("+43664", "A"), teil("+43676", "B")))
        assertEquals(listOf("A", "B"), ganz.map { it.body })
    }

    @Test
    fun `nichts drin heisst nichts zu speichern`() {
        assertEquals(emptyList<SmsDelivery.Incoming>(), SmsDelivery.merge(emptyList()))
    }

    /** Doppelt gespeichert wäre so falsch wie gar nicht. */
    @Test
    fun `geschrieben wird nur als Standard-App`() {
        assertTrue(SmsDelivery.mayWrite("org.biglau", "org.biglau"))
        assertFalse(SmsDelivery.mayWrite("com.google.android.apps.messaging", "org.biglau"))
        assertFalse(SmsDelivery.mayWrite(null, "org.biglau"))
    }
}
