package org.biglau.sms

import org.biglau.data.SmsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the repeated reminder, PLAN.md 4.7.
 *
 * the case prevented here: a reminder that does not stop. it hangs on "unread", and that
 * reading really sets that was not the case until today.
 */
class SmsReminderTest {

    private fun message(
        id: Long,
        address: String,
        body: String = "Hello",
        read: Boolean = false,
        incoming: Boolean = true,
        timestamp: Long = id,
    ) = SmsMessage(id, id, address, body, timestamp, incoming, read)

    @Test
    fun `off is off`() {
        assertFalse(SmsReminder.active(SmsConfig(repeatMinutes = 0)))
        assertEquals(0L, SmsReminder.delayMs(0))
        assertTrue(SmsReminder.active(SmsConfig(repeatMinutes = 5)))
        assertEquals(300_000L, SmsReminder.delayMs(5))
    }

    @Test
    fun `read messages no longer remind`() {
        val open = SmsReminder.due(
            listOf(message(1, "+43664111001", read = true), message(2, "+43676222222")),
            SmsConfig(),
        )
        assertEquals(listOf("+43676222222"), open.map { it.address })
    }

    @Test
    fun `one's own messages do not remind`() {
        assertTrue(SmsReminder.due(listOf(message(1, "+43664111001", incoming = false)), SmsConfig()).isEmpty())
    }

    /** three unread from the same number are one reminder, not three. */
    @Test
    fun `the newest per sender`() {
        val open = SmsReminder.due(
            listOf(
                message(1, "+43664111001", "first"),
                message(2, "+43664111001", "second"),
                message(3, "+43676222222", "other"),
            ),
            SmsConfig(),
        )
        assertEquals(2, open.size)
        assertEquals("other", open.first().body)
        assertEquals("second", open.last().body)
    }

    /** otherwise the advertisement one did not want to see would come back every five minutes. */
    @Test
    fun `what is hidden does not remind`() {
        val open = SmsReminder.due(
            listOf(message(1, "+43664111001"), message(2, "+43676222222", "you have won")),
            SmsConfig(hiddenWords = listOf("won")),
        )
        assertEquals(listOf("+43664111001"), open.map { it.address })
    }

    /**
     * the first reminder brought twelve notices at once. on a real phone that happens as soon
     * as someone makes BigLau the default app with a backlog of unread messages.
     */
    @Test
    fun `at most three notices at once`() {
        val many = (1..10).map { message(it.toLong(), "+4366411100$it") }
        val open = SmsReminder.due(many, SmsConfig())
        assertEquals(SmsReminder.MAX_AT_ONCE, open.size)
        // and the newest ones at that.
        assertEquals(listOf(10L, 9L, 8L), open.map { it.timestamp })
    }

    @Test
    fun `without unread ones there is nothing to remind about`() {
        assertTrue(SmsReminder.due(emptyList(), SmsConfig()).isEmpty())
    }
}
