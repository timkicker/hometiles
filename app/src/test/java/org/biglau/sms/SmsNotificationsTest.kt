package org.biglau.sms

import org.biglau.data.SmsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the notice about a new message.
 *
 * it hangs on the sms role. without it the message lay in the database and nobody knew of it
 * until they opened the list themselves - for a phone in a pocket the same as lost.
 */
class SmsNotificationsTest {

    private fun message(address: String, body: String, incoming: Boolean = true) =
        SmsMessage(1, 1, address, body, 0L, incoming, false)

    /** a channel cannot be changed; another duration must therefore be another channel. */
    @Test
    fun `every vibration length has a channel of its own`() {
        val channels = SmsNotifications.VIBRATION_CHOICES.map { SmsNotifications.channelId(it) }
        assertEquals(channels.size, channels.toSet().size)
        assertTrue(channels.all { it.startsWith("sms-") })
    }

    @Test
    fun `off and on are both on offer`() {
        assertTrue(0 in SmsNotifications.VIBRATION_CHOICES)
        assertTrue(SmsNotifications.VIBRATION_CHOICES.any { it > 0 })
    }

    @Test
    fun `an incoming message reports itself`() {
        assertTrue(SmsNotifications.shouldNotify(message("+43664111001", "Hello"), SmsConfig()))
    }

    /** otherwise hiding would have half the effect: the advertisement would keep ringing. */
    @Test
    fun `what is hidden does not report itself`() {
        val config = SmsConfig(hiddenNumbers = listOf("+43664111001"), hiddenWords = listOf("won"))
        assertFalse(SmsNotifications.shouldNotify(message("+43664111001", "Hello"), config))
        assertFalse(SmsNotifications.shouldNotify(message("+43676222222", "you have WON"), config))
        assertTrue(SmsNotifications.shouldNotify(message("+43676222222", "there at six"), config))
    }

    @Test
    fun `one's own messages do not report themselves`() {
        assertFalse(
            SmsNotifications.shouldNotify(message("+43664111001", "see you soon", incoming = false), SmsConfig()),
        )
    }

    /** two messages from the same sender are one notice, two senders are two. */
    @Test
    fun `one notice per sender`() {
        assertEquals(
            SmsNotifications.notificationId("+43 664 111 001"),
            SmsNotifications.notificationId("+43664111001"),
        )
        assertNotEquals(
            SmsNotifications.notificationId("+43664111001"),
            SmsNotifications.notificationId("+43676222222"),
        )
    }
}
