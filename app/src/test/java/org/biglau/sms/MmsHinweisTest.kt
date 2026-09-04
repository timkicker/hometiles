package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Bildnachricht, die nicht ankommt, sagt Bescheid.
 *
 * `PLAN.md` 6 verlangt es für MMS ausdrücklich: „bei Fehlschlag **sichtbar an den Nutzer
 * melden statt still schlucken". Der Empfänger tat bis zum 3.9.2026 genau das Gegenteil: er
 * merkte sich, dass sich etwas geändert hat, und schwieg. Wer ein Bild erwartet, wartet auf
 * etwas, das nie kommt — und hält das Telefon für kaputt, nicht die App für ehrlich.
 *
 * Der Hinweis nennt beides: den Grund (kein Netzzugang) und den Ausweg (die Nachrichten-App
 * des Telefons). Eine Meldung, die ein Problem nennt, ohne einen Weg zu zeigen, ist nur die
 * halbe Antwort — dieselbe Regel wie beim Notruf-Knopf und bei der toten Kachel.
 */
class MmsHinweisTest {

    private val empfaenger = Quelltext.datei("org/biglau/sms/SmsComponents.kt").readText()
    private val meldungen = Quelltext.datei("org/biglau/sms/SmsNotifications.kt").readText()

    @Test
    fun `der WAP-Push-Empfaenger schweigt nicht mehr`() {
        val stelle = Quelltext.ausschnitt(empfaenger, "class WapPushDeliverReceiver", "\n}")
        assertTrue(
            "WapPushDeliverReceiver meldet die Bildnachricht nicht mehr - dann kommt sie " +
                "still an und niemand erfährt davon.",
            "showMmsHint(" in stelle,
        )
    }

    /**
     * Der Kanal darf **nicht** mit dem Präfix der SMS-Kanäle anfangen.
     *
     * `show` räumt beim Anlegen seines Kanals alle anderen mit demselben Präfix weg — das
     * ist Absicht, damit nicht für jede Vibrationsdauer ein alter Kanal liegenbleibt. Hiesse
     * der MMS-Kanal `sms-…`, verschwände der Hinweis beim nächsten SMS-Eingang mitsamt
     * seinem Kanal.
     */
    @Test
    fun `der MMS-Kanal faellt nicht der Aufraeumschleife zum Opfer`() {
        // Gefragt ist die Kennung, nicht wie die Konstante heisst. Die erste Fassung suchte
        // `MMS_CHANNEL = "…"`; am 03.09.2026 wurde daraus `MMS_CHANNEL_PREFIX`, weil die
        // Vibrationsstaerke in die Kennung musste - und die Regel fiel um, obwohl die Sache
        // dieselbe geblieben war.
        val kanal = Regex("""MMS_CHANNEL(?:_PREFIX)? = "([^"]+)"""")
            .find(meldungen)?.groupValues?.get(1)
        assertTrue("Den MMS-Kanal gibt es nicht mehr", kanal != null)
        val praefix = Regex("""[^_]CHANNEL_PREFIX = "([^"]+)"""").find(meldungen)!!.groupValues[1]
        assertEquals(
            "Der MMS-Kanal beginnt mit dem Präfix der SMS-Kanäle und wird beim nächsten " +
                "SMS-Hinweis weggeräumt.",
            false,
            kanal!!.startsWith(praefix),
        )
    }

    /**
     * Der Hinweis nennt einen Ausweg, der auch **funktioniert**.
     *
     * Bis zum 04.09.2026 stand dort: „Öffne sie in der Nachrichten-App des Telefons."
     * Das war richtig, solange eine andere App die Standard-App war. Eine MMS kann aber
     * **nur die Standard-App** holen — sobald BigLau die Rolle hält und sie nicht holt,
     * kann es auch keine andere. Der Ausweg zeigte auf eine Tür, die zu ist.
     *
     * Der Ausweg, der bleibt, ist die Rolle zurückzugeben. Deshalb prüft die Regel jetzt
     * zweierlei: der Hinweis nennt die Einstellungen, und er schickt niemanden mehr zu
     * einer anderen Nachrichten-App.
     *
     * Die erste Fassung dieser Regel prüfte die Wörter „network" und „Nachrichten-App" —
     * und hätte den neuen, richtigen Text abgelehnt, weil er sie nicht mehr enthält. Sie
     * hing am Wortlaut, nicht an der Sache.
     */
    @Test
    fun `der Hinweis nennt einen Ausweg, den es noch gibt`() {
        listOf(
            "values" to Triple("settings", listOf("in the messaging app"), "cannot"),
            "values-de" to Triple("Einstellungen", listOf("in der Nachrichten-App"), "nicht"),
        ).forEach { (sprache, was) ->
            val (ausweg, sackgassen, grund) = was
            val text = Quelltext.textWert("mms_arrived_body", sprache)
            assertTrue(
                "$sprache: der Hinweis nennt keinen Ausweg - er muss auf die Einstellungen " +
                    "zeigen, wo sich die Rolle zurueckgeben laesst.",
                ausweg in text,
            )
            assertTrue(
                "$sprache: der Hinweis nennt keinen Grund.",
                grund in text,
            )
            sackgassen.forEach {
                assertTrue(
                    "$sprache: der Hinweis schickt zu einer anderen Nachrichten-App. Die " +
                        "kann eine MMS nicht holen, solange BigLau die Rolle haelt.",
                    it !in text,
                )
            }
        }
    }
}
