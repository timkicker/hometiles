package org.biglau.sms

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Hinweis gehört in die Unterhaltung, nicht davor.
 *
 * Fest über der Liste gesetzt, nahm „BigLau ist nicht Ihre Nachrichten-App …" bei 200 %
 * Textgröße fünf Zeilen — und von der Unterhaltung blieb ein Streifen von **zwei
 * Bildpunkten**. Am Emulator gesehen. Ein Hinweis, der den Inhalt verdrängt, für den er
 * gilt, ist keiner mehr.
 *
 * Im Blättern kostet er nichts: die Unterhaltung öffnet bei der neuesten Nachricht, und wer
 * nach oben schaut, findet ihn dort, wo die ältesten stehen.
 */
class ConversationLayoutTest {

    @Test
    fun `der Hinweis steht innerhalb der Liste`() {
        val quelle = Quelltext.datei("org/biglau/sms/SmsActivity.kt").readText()
        val liste = quelle.indexOf("LazyColumn(\n            state = listState")
        val hinweis = quelle.indexOf("R.string.sms_not_default")
        assertTrue("LazyColumn der Unterhaltung nicht gefunden", liste > 0)
        assertTrue("Hinweis nicht gefunden", hinweis > 0)
        assertTrue(
            "Der Hinweis steht vor der Liste - bei grosser Schrift verdraengt er sie",
            hinweis > liste,
        )
    }
}
