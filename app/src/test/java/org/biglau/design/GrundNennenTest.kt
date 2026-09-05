package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wenn etwas schiefgeht, steht der Grund dabei — soweit er bekannt ist.
 *
 * Achtzehn Meldungen zeigt BigLau über `Notice`. Drei sagten am 3.9.2026 nur „hat nicht
 * geklappt": das Löschen aus der Anrufliste, das Setzen eines Favoriten und das Senden einer
 * Nachricht. Zwei davon sind in Ordnung, und das ist der interessante Teil:
 *
 * * Die **Anrufliste** hat einen vollständigen Berechtigungsablauf davor. Kommt die Meldung
 *   trotzdem, ist die Berechtigung da und der Anbieter hat trotzdem nichts gelöscht — dann
 *   *ist* „hat nicht geklappt" die ganze Wahrheit.
 * * Der **Favorit** wird nur gemeldet, wenn `canWrite()` schon zugestimmt hat.
 * * Das **Senden** dagegen fing jeden Fehler in einem `runCatching` und sagte immer
 *   denselben Satz. Der eine Grund, den ein Mensch beheben kann — die fehlende Berechtigung
 *   — sah aus wie ein Defekt.
 *
 * Bewusst **kein** Berechtigungsdialog von der Sendestelle aus: sie sendet, sie soll nicht
 * auch das Recht dazu beschaffen. Der Satz nennt den Ort, entschieden wird woanders.
 */
class GrundNennenTest {

    private val sms = Quelltext.file("org/biglau/sms/SmsActivity.kt").readText()

    @Test
    fun `ein Fehlschlag beim Senden unterscheidet die fehlende Berechtigung`() {
        val stelle = Quelltext.cut(sms, "R.string.sms_send_failed").take(400) +
            Quelltext.cut(sms, "", "R.string.sms_send_failed").takeLast(700)
        assertTrue(
            "Der Fehlschlag beim Senden zeigt immer denselben Satz. Die fehlende " +
                "SEND_SMS-Berechtigung ist der eine Grund, den man beheben kann - der " +
                "gehört benannt.",
            "sms_send_no_permission" in stelle && "Manifest.permission.SEND_SMS" in stelle,
        )
    }

    /** Und die Sendestelle fragt nicht selbst nach dem Recht zu senden. */
    @Test
    fun `die Sendestelle beschafft sich kein Senderecht`() {
        val versand = Quelltext.cut(sms, "private fun send(")
        assertEquals(
            "In `send` wird eine Berechtigung angefordert. Diese Stelle sendet - sie soll " +
                "nicht auch das Recht dazu beschaffen, sonst steht am Ende einer Kette aus " +
                "Dialogen eine abgeschickte Nachricht, die niemand mehr bewusst ausgelöst hat.",
            false,
            ".launch(Manifest.permission" in versand,
        )
    }

    @Test
    fun `der Hinweis nennt den Ort in beiden Sprachen`() {
        listOf("values" to "app settings", "values-de" to "App-Einstellungen").forEach { (sprache, ort) ->
            val text = Quelltext.textValue("sms_send_no_permission", sprache)
            assertTrue("$sprache: der Hinweis nennt den Ort nicht: $text", ort in text)
        }
    }
}
