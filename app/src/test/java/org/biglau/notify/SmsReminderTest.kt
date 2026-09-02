package org.biglau.notify

import org.biglau.data.SmsConfig
import org.biglau.sms.SmsMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die wiederholte Erinnerung, `PLAN.md` 4.7.
 *
 * Der Fall, der hier verhindert wird: eine Erinnerung, die nicht aufhört. Sie hängt an
 * „ungelesen" — und dass Lesen das auch wirklich setzt, war bis heute nicht der Fall.
 */
class SmsReminderTest {

    private fun nachricht(
        id: Long,
        address: String,
        body: String = "Hallo",
        read: Boolean = false,
        incoming: Boolean = true,
        timestamp: Long = id,
    ) = SmsMessage(id, id, address, body, timestamp, incoming, read)

    @Test
    fun `aus ist aus`() {
        assertFalse(SmsReminder.active(SmsConfig(repeatMinutes = 0)))
        assertEquals(0L, SmsReminder.delayMs(0))
        assertTrue(SmsReminder.active(SmsConfig(repeatMinutes = 5)))
        assertEquals(300_000L, SmsReminder.delayMs(5))
    }

    @Test
    fun `gelesene Nachrichten erinnern nicht mehr`() {
        val offen = SmsReminder.due(
            listOf(nachricht(1, "+43664111001", read = true), nachricht(2, "+43676222222")),
            SmsConfig(),
        )
        assertEquals(listOf("+43676222222"), offen.map { it.address })
    }

    @Test
    fun `eigene Nachrichten erinnern nicht`() {
        assertTrue(SmsReminder.due(listOf(nachricht(1, "+43664111001", incoming = false)), SmsConfig()).isEmpty())
    }

    /** Drei ungelesene von derselben Nummer sind eine Erinnerung, nicht drei. */
    @Test
    fun `je Absender die neueste`() {
        val offen = SmsReminder.due(
            listOf(
                nachricht(1, "+43664111001", "erste"),
                nachricht(2, "+43664111001", "zweite"),
                nachricht(3, "+43676222222", "andere"),
            ),
            SmsConfig(),
        )
        assertEquals(2, offen.size)
        assertEquals("andere", offen.first().body)
        assertEquals("zweite", offen.last().body)
    }

    /** Sonst käme die Werbung, die man nicht sehen wollte, alle fünf Minuten wieder. */
    @Test
    fun `was ausgeblendet ist, erinnert nicht`() {
        val offen = SmsReminder.due(
            listOf(nachricht(1, "+43664111001"), nachricht(2, "+43676222222", "Sie haben gewonnen")),
            SmsConfig(hiddenWords = listOf("gewonnen")),
        )
        assertEquals(listOf("+43664111001"), offen.map { it.address })
    }

    /**
     * Am Emulator hineingelaufen: die erste Erinnerung brachte zwölf Meldungen auf einmal.
     * Am echten Gerät passiert das, sobald jemand BigLau mit einem Rückstand ungelesener
     * Nachrichten zur Standard-App macht.
     */
    @Test
    fun `hoechstens drei Meldungen auf einmal`() {
        val viele = (1..10).map { nachricht(it.toLong(), "+4366411100$it") }
        val offen = SmsReminder.due(viele, SmsConfig())
        assertEquals(SmsReminder.MAX_AT_ONCE, offen.size)
        // Und zwar die neuesten.
        assertEquals(listOf(10L, 9L, 8L), offen.map { it.timestamp })
    }

    @Test
    fun `ohne ungelesene gibt es nichts zu erinnern`() {
        assertTrue(SmsReminder.due(emptyList(), SmsConfig()).isEmpty())
    }
}
