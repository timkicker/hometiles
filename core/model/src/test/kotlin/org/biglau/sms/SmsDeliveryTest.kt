package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * incoming messages do not get lost.
 *
 * re-enacted on the emulator (02.09.2026): BigLau given the sms role, a message sent, and it
 * was nowhere. android delivers `SMS_DELIVER` only to the default app, and whoever holds the
 * role has to store it themselves.
 */
class SmsDeliveryTest {

    private fun part(a: String, b: String, t: Long = 1L) = SmsDelivery.Part(a, b, t)

    @Test
    fun `a single message stays one`() {
        val whole = SmsDelivery.merge(listOf(part("+43664", "Hello")))
        assertEquals(1, whole.size)
        assertEquals("Hello", whole.first().body)
    }

    /** otherwise three half messages would stand under one another instead of one whole. */
    @Test
    fun `parts of a long message come back together`() {
        val whole = SmsDelivery.merge(
            listOf(part("+43664", "first part ", 100), part("+43664", "and second.", 200)),
        )
        assertEquals(1, whole.size)
        assertEquals("first part and second.", whole.first().body)
        assertEquals(100L, whole.first().timestamp)
    }

    @Test
    fun `different senders stay apart`() {
        val whole = SmsDelivery.merge(listOf(part("+43664", "A"), part("+43676", "B")))
        assertEquals(listOf("A", "B"), whole.map { it.body })
    }

    @Test
    fun `nothing in means nothing to store`() {
        assertEquals(emptyList<SmsDelivery.Incoming>(), SmsDelivery.merge(emptyList()))
    }

    /** stored twice would be as wrong as not at all. */
    @Test
    fun `writing happens only as the default app`() {
        assertTrue(SmsDelivery.mayWrite("org.biglau", "org.biglau"))
        assertFalse(SmsDelivery.mayWrite("com.google.android.apps.messaging", "org.biglau"))
        assertFalse(SmsDelivery.mayWrite(null, "org.biglau"))
    }
}
