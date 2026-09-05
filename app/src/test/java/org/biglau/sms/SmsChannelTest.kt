package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Kanäle der Nachrichten-Meldung räumen sich selbst weg.
 *
 * Android lässt die Vibration eines Benachrichtigungskanals nach dem Anlegen nicht mehr
 * ändern. BigLau legt deshalb je Vibrationsdauer einen eigenen Kanal an (`sms-500`) und
 * löscht beim nächsten Mal die anderen. Das ist der übliche Weg — und er hängt daran, dass
 * die Kennung und das Aufräumen **dasselbe Präfix** benutzen.
 *
 * Liefen die beiden auseinander, sammelten sich in den Systemeinstellungen des Nutzers
 * stumme Kanäle an, die nie wieder jemand benutzt. Sichtbar wäre das nur dort — in der App
 * nicht, in keinem Test, in keiner Meldung.
 */
class SmsChannelTest {

    private val quelle = Quelltext.withoutComments("org/biglau/sms/SmsNotifications.kt")

    @Test
    fun `Kennung und Aufraeumen benutzen dasselbe Praefix`() {
        assertTrue(
            "channelId baut die Kennung nicht aus CHANNEL_PREFIX",
            "\"\$CHANNEL_PREFIX\$vibrationMs\"" in quelle,
        )
        assertTrue(
            "das Aufräumen sucht nicht nach CHANNEL_PREFIX - dann bleiben alte Kanäle liegen",
            "startsWith(CHANNEL_PREFIX)" in quelle,
        )
        // Die Definition selbst ist die eine erlaubte Stelle - sie **ist** das Präfix.
        // Die erste Fassung dieser Prüfung zählte sie mit und fiel über ihren eigenen
        // Gegenstand; das dritte Mal heute Nacht, dass eine Regel sich selbst findet.
        val hartGeschrieben = quelle.lines()
            .filterNot { "const val CHANNEL_PREFIX" in it }
            .count { "\"sms-\"" in it }
        assertEquals(
            "hart geschriebenes \"sms-\" neben der Definition - dann laufen die Stellen " +
                "eines Tages auseinander",
            0,
            hartGeschrieben,
        )
    }

    @Test
    fun `jede angebotene Vibrationsdauer ergibt eine eigene Kennung`() {
        val dauern = SmsNotifications.VIBRATION_CHOICES
        assertEquals(
            "zwei Dauern mit derselben Kennung - dann behielte eine die Vibration der anderen",
            dauern.size,
            dauern.map { SmsNotifications.channelId(it) }.toSet().size,
        )
    }
}
