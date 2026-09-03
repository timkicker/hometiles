package org.biglau.sms

import org.biglau.data.SmsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Meldung über eine neue Nachricht.
 *
 * Sie hängt an der SMS-Rolle. Ohne sie lag die Nachricht in der Datenbank und niemand
 * wusste davon, bis er von sich aus die Liste öffnete — für ein Telefon in der Tasche
 * dasselbe wie verloren.
 */
class SmsNotificationsTest {

    private fun nachricht(address: String, body: String, incoming: Boolean = true) =
        SmsMessage(1, 1, address, body, 0L, incoming, false)

    /** Ein Kanal lässt sich nicht ändern; eine andere Dauer muss deshalb ein anderer sein. */
    @Test
    fun `jede Vibrationsdauer hat ihren eigenen Kanal`() {
        val kanaele = SmsNotifications.VIBRATION_CHOICES.map { SmsNotifications.channelId(it) }
        assertEquals(kanaele.size, kanaele.toSet().size)
        assertTrue(kanaele.all { it.startsWith("sms-") })
    }

    @Test
    fun `aus und an stehen beide zur Wahl`() {
        assertTrue(0 in SmsNotifications.VIBRATION_CHOICES)
        assertTrue(SmsNotifications.VIBRATION_CHOICES.any { it > 0 })
    }

    @Test
    fun `eine eingehende Nachricht meldet sich`() {
        assertTrue(SmsNotifications.shouldNotify(nachricht("+43664111001", "Hallo"), SmsConfig()))
    }

    /** Sonst hätte das Ausblenden nur die halbe Wirkung: die Werbung klingelte weiter. */
    @Test
    fun `was ausgeblendet ist, meldet sich nicht`() {
        val config = SmsConfig(hiddenNumbers = listOf("+43664111001"), hiddenWords = listOf("gewonnen"))
        assertFalse(SmsNotifications.shouldNotify(nachricht("+43664111001", "Hallo"), config))
        assertFalse(SmsNotifications.shouldNotify(nachricht("+43676222222", "Sie haben GEWONNEN"), config))
        assertTrue(SmsNotifications.shouldNotify(nachricht("+43676222222", "Bin um sechs da"), config))
    }

    @Test
    fun `eigene Nachrichten melden sich nicht`() {
        assertFalse(
            SmsNotifications.shouldNotify(nachricht("+43664111001", "Bis gleich", incoming = false), SmsConfig()),
        )
    }

    /** Zwei Nachrichten desselben Absenders sind eine Meldung, zwei Absender sind zwei. */
    @Test
    fun `je Absender eine Meldung`() {
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
