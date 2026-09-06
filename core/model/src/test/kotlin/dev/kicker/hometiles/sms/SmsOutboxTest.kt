package dev.kicker.hometiles.sms

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * one's own sent message.
 *
 * android stores it by itself only when the sending app is **not** the default app. with the
 * role it would otherwise vanish in the moment it goes out.
 */
class SmsOutboxTest {

    private val values = SmsOutbox.values("+43664111001", "there at six", 1_700_000_000_000L)

    @Test
    fun `recipient, text and time stand in it`() {
        assertEquals("+43664111001", values["address"])
        assertEquals("there at six", values["body"])
        assertEquals(1_700_000_000_000L, values["date"])
    }

    /** otherwise one's own message would count as new and the reminder would remind of it. */
    @Test
    fun `one's own message counts as read`() {
        assertEquals(1, values["read"])
        assertEquals(1, values["seen"])
    }
}
