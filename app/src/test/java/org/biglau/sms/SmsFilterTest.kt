package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Nachrichtenfilter (`PLAN.md` 4.7).
 *
 * Er **verbirgt**, er sperrt nicht — BigLau hält die SMS-Rolle nicht. Das steht so im
 * KDoc, in den Einstellungen und hier, damit niemand die Grenze für dichter hält, als sie
 * ist.
 */
class SmsFilterTest {

    private var id = 0L
    private fun msg(
        body: String = "Hallo",
        address: String = "+436641234567",
        incoming: Boolean = true,
    ) = SmsMessage(id++, 1L, address, body, 1000L, incoming, true)

    @Test
    fun `ohne Filter bleibt alles stehen`() {
        val liste = listOf(msg(), msg("Zweite"))
        assertEquals(liste, SmsFilter.apply(liste, emptyList(), emptyList()))
    }

    @Test
    fun `eine gefilterte Nummer verschwindet aus der Liste`() {
        val liste = listOf(msg(address = "+436641234567"), msg(address = "+436809999999"))
        val uebrig = SmsFilter.apply(liste, listOf("0664 1234567"), emptyList())
        assertEquals(1, uebrig.size)
        assertEquals("+436809999999", uebrig.first().address)
    }

    @Test
    fun `ein Wort wirkt mitten im Text und unabhaengig von der Schreibweise`() {
        // Werbung haengt ihre Woerter gern an: "GEWINNSPIEL!!!"
        assertTrue(SmsFilter.hidden(msg("Sie haben GEWONNEN!!!"), emptyList(), listOf("gewonnen")))
        assertTrue(SmsFilter.hidden(msg("herzlichen glückwunsch"), emptyList(), listOf("Glückwunsch")))
        assertFalse(SmsFilter.hidden(msg("Bis gleich"), emptyList(), listOf("gewonnen")))
    }

    @Test
    fun `eigene Nachrichten werden nie verborgen`() {
        // Was man selbst geschrieben hat, verbirgt man nicht vor sich - auch nicht, wenn
        // das gefilterte Wort darin vorkommt.
        assertFalse(
            SmsFilter.hidden(msg("Ich habe gewonnen", incoming = false), emptyList(), listOf("gewonnen")),
        )
    }

    @Test
    fun `eine zu kurze Nummer filtert nicht alles weg`() {
        // Derselbe Fehler wie bei der Anrufsperre: "123" duerfte nicht jede Nachricht
        // treffen, die zufaellig so endet.
        val liste = listOf(msg(address = "+436641234567"))
        assertEquals(liste, SmsFilter.apply(liste, listOf("123"), emptyList()))
    }

    @Test
    fun `die Wortliste wird zerlegt und entdoppelt`() {
        assertEquals(listOf("Gewinn", "Werbung"), SmsFilter.parseWords("Gewinn, gewinn; Werbung"))
        assertEquals("Gewinn, Werbung", SmsFilter.formatWords(listOf("Gewinn", "Werbung")))
    }

    @Test
    fun `ein leeres Wort filtert nichts`() {
        // Sonst verschwaende die ganze Liste, weil jeder Text die leere Zeichenkette enthaelt.
        assertFalse(SmsFilter.hidden(msg("Hallo"), emptyList(), listOf("", "   ")))
    }
}
