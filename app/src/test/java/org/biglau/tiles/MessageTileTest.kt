package org.biglau.tiles

import org.biglau.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Kachel „neue Nachricht an feste Nummer" aus `PLAN.md` 4.3.
 *
 * Sie war der letzte offene Punkt der Aktionsliste und stand lange zurückgestellt, weil
 * jede Prüfung eine Nachricht erzeugt hätte. Das tut sie nicht: die Kachel öffnet den
 * Schreiben-Bildschirm mit eingetragenem Empfänger, abgeschickt wird erst durch einen
 * Tipp auf Senden.
 */
class MessageTileTest {

    @Test
    fun `aus einer Nummer wird eine Nachrichten-Kachel`() {
        val action = MessageTile.actionFor("+43 664 111 001")
        assertEquals("+43664111001", action?.number)
        assertEquals(ContactMode.SMS, action?.mode)
    }

    /** Auf der Kachel steht die Nummer in Blöcken - dasselbe Bild wie in der Anrufliste. */
    @Test
    fun `die Nummer steht lesbar auf der Kachel`() {
        assertEquals("+436 641 110 01", MessageTile.actionFor("+43664111001")?.name)
    }

    @Test
    fun `Schreibweisen fallen weg, die Ziffern bleiben`() {
        assertEquals("0664111001", MessageTile.actionFor("0664/111-001")?.number)
    }

    /** Eine Kachel, die einen leeren Schreiben-Bildschirm öffnet, tut nie etwas. */
    @Test
    fun `ohne Ziffer entsteht keine Kachel`() {
        assertNull(MessageTile.actionFor(""))
        assertNull(MessageTile.actionFor("   "))
        assertNull(MessageTile.actionFor("Oma"))
        assertNull(MessageTile.actionFor("+"))
    }
}
