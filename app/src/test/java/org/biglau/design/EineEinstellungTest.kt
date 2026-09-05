package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Einstellung gilt fuer alle Nachrichten, nicht fuer manche.
 *
 * Die Vibration bei einer neuen Nachricht ist einstellbar. Der Kanal fuer die **SMS** hielt
 * sich daran; der Kanal fuer den **MMS-Hinweis** nicht - er stand daneben mit dem Vermerk
 * „haengt an keiner Vibrationseinstellung", als waere das eine Eigenschaft und kein Fehler.
 * Wer die Vibration ausschaltete, bekam sie bei einer Bildnachricht trotzdem.
 *
 * Solche Luecken sind schwer zu bemerken, weil sie nur im selteneren Fall auftreten: man
 * stellt etwas ab, es ist wochenlang ruhig, und dann vibriert es doch - und niemand kommt
 * darauf, dass es an der Sorte Nachricht liegt.
 */
class EineEinstellungTest {

    private val quelle = Quelltext.datei("org/biglau/sms/SmsNotifications.kt").readText()

    @Test
    fun `jeder Nachrichtenkanal richtet sich nach der Vibrationseinstellung`() {
        // Nur der Konstruktor am Zeilenanfang - `createNotificationChannel(` und
        // `deleteNotificationChannel(` enthalten denselben Namen und wurden von der ersten
        // Fassung dieser Regel dreifach mitgezaehlt.
        val kanaele = Regex("""(?m)^\s*NotificationChannel\(""")
            .findAll(quelle)
            .map { it.range.last }
            .toList()
        assertTrue("Es gibt keine Kanaele mehr - liest die Regel noch, was sie meint?", kanaele.size >= 2)

        val ohne = kanaele.filterNot { ab ->
            // Der Kanal fuer den Sendefehler ist keine eingehende Nachricht, sondern eine
            // Antwort auf eine eigene Handlung - er darf sich anders verhalten.
            // Der Name des Kanals steht **hinter** der Klammer, nicht davor - die erste
            // Fassung suchte ihn davor und nahm den Fehlerkanal nicht aus.
            // Weit genug: der Kommentar ueber der Zeile gehoert zum Block. Bei 500 Zeichen
            // schob die Begruendung des Fixes den Fix selbst aus dem Fenster, und die Regel
            // meldete genau das, was sie gerade bekommen hatte.
            val block = quelle.substring(ab, minOf(quelle.length, ab + 900))
            "ERROR_CHANNEL" in block.take(80) || "enableVibration" in block
        }
        assertEquals(
            "Hier steht ein Nachrichtenkanal, der die Vibrationseinstellung nicht kennt. " +
                "Eine Einstellung, die nur fuer einen Teil der Nachrichten gilt, ist keine.",
            emptyList<Int>(),
            ohne,
        )
    }

    /**
     * Und die Einstellung muss in der **Kennung** des Kanals stehen.
     *
     * Ein Benachrichtigungskanal ist nach dem Anlegen unveraenderlich:
     * `createNotificationChannel` auf einen vorhandenen aendert Name und Beschreibung, sonst
     * nichts. Ton und Vibration bleiben, wie sie beim allerersten Mal waren.
     *
     * Am 03.09.2026 am Geraet gesehen, und zwar **nachdem** der Fix schon drin war: die
     * Einstellung stand auf 500 ms, der Kanal meldete `mVibrationEnabled=false`. Er stammte
     * aus einer frueheren Probe. Der Weg fuer die SMS kannte die Falle laengst - die
     * Einstellung steckt dort in der Kennung, und alte Kanaele werden weggeraeumt.
     *
     * Eine Einstellung, die erst nach einer Neuinstallation wirkt, ist keine.
     */
    @Test
    fun `die Kennung eines Kanals traegt die Einstellung`() {
        listOf("fun channelId(", "fun mmsChannelId(").forEach { name ->
            val ab = quelle.indexOf(name)
            assertTrue("$name gibt es nicht mehr", ab > 0)
            assertTrue(
                "$name steckt die Vibrationsstaerke nicht in die Kennung - dann behaelt " +
                    "ein einmal angelegter Kanal seine alte Einstellung fuer immer.",
                "vibrationMs" in quelle.substring(ab, ab + 120),
            )
        }
        listOf("CHANNEL_PREFIX", "MMS_CHANNEL_PREFIX").forEach { praefix ->
            assertTrue(
                "Zu $praefix wird nicht aufgeraeumt - dann sammeln sich in den " +
                    "Systemeinstellungen mehrere Kanaele mit demselben Namen.",
                Regex("""startsWith\($praefix\)""").containsMatchIn(quelle),
            )
        }
    }

    @Test
    fun `der MMS-Hinweis bekommt die Einstellung ueberhaupt gereicht`() {
        assertTrue(
            "showMmsHint kennt die Einstellungen nicht - dann kann es sich nicht nach " +
                "ihnen richten.",
            "fun showMmsHint(context: Context, config: SmsConfig)" in quelle,
        )
        val empfaenger = Quelltext.datei("org/biglau/sms/SmsComponents.kt").readText()
        assertTrue(
            "Der WAP-Push-Empfaenger reicht die Einstellungen nicht weiter.",
            "showMmsHint(context, ConfigStore" in empfaenger,
        )
    }
}
