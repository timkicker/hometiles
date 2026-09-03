package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Nummernsperre (`PLAN.md` 4.6).
 *
 * Der heikle Teil ist der Vergleich. Dieselbe Person erscheint als „+43 664 1234567",
 * „0043 664 1234567" und „0664 1234567" — wer auf Gleichheit vergleicht, sperrt in zwei von
 * drei Fällen nicht. Eine Sperre, die manchmal nicht greift, ist schlimmer als keine, weil
 * man sich auf sie verlässt.
 */
class CallBlockingTest {

    private val liste = listOf("+436641234567")

    @Test
    fun `dieselbe Nummer in drei Schreibweisen ist dieselbe Sperre`() {
        assertTrue(CallBlocking.isBlocked("+43 664 1234567", liste))
        assertTrue(CallBlocking.isBlocked("0043 664 1234567", liste))
        assertTrue(CallBlocking.isBlocked("0664 1234567", liste))
        assertTrue(CallBlocking.isBlocked("06641234567", liste))
    }

    @Test
    fun `eine andere Nummer bleibt frei`() {
        assertFalse(CallBlocking.isBlocked("+436641234568", liste))
        assertFalse(CallBlocking.isBlocked("+436649999999", liste))
    }

    @Test
    fun `Notrufnummern lassen sich nicht sperren`() {
        // PLAN.md 4.6: sie gehen immer durch. Eine versehentlich gesperrte 112 waere der
        // teuerste Fehler, den diese App machen kann.
        listOf("112", "144", "133").forEach { notruf ->
            assertFalse(notruf, CallBlocking.isBlocked(notruf, listOf(notruf)))
        }
    }

    @Test
    fun `eine unterdrueckte Nummer ist nicht sperrbar`() {
        // Sonst traefe eine einzige Sperre jeden anonymen Anrufer auf einmal - derselbe
        // Fehler wie damals beim Gruppieren der Anrufliste.
        assertFalse(CallBlocking.isBlocked("", liste))
        assertFalse(CallBlocking.isBlocked("Unbekannt", liste))
    }

    @Test
    fun `zu kurze Eintraege werden nicht uebernommen`() {
        // Sonst sperrte "123" jede Nummer, die zufaellig so endet.
        assertEquals(emptyList<String>(), CallBlocking.parse("123, 45"))
        assertEquals(listOf("123", "45"), CallBlocking.rejected("123, 45"))
    }

    @Test
    fun `die Zeile wird zerlegt und Dubletten fallen weg`() {
        val liste = CallBlocking.parse("+436641234567, 0664 1234567; 0680 7654321")
        assertEquals(2, liste.size)
    }

    @Test
    fun `Speichern und Anzeigen sind umkehrbar`() {
        val text = "+436641234567, +436807654321"
        assertEquals(text, CallBlocking.format(CallBlocking.parse(text)))
    }

    @Test
    fun `eine leere Liste sperrt nichts`() {
        assertFalse(CallBlocking.isBlocked("+436641234567", emptyList()))
    }

    // --- Abweisen, bevor es klingelt (02.09.2026) ---

    /**
     * Die Sperre lief bis hierher erst in `onCallAdded` - also nachdem Android geklingelt
     * und die Gespraechsansicht gebunden hatte, und in der Anrufliste stand der Anruf als
     * **abgelehnt**, so als haette der Nutzer ihn weggedrueckt. Mit `CallScreening` fragt
     * Android vorher; am Emulator geprueft: `mCallBlockReason = 1`, Eintrag mit
     * `type=6` (blockiert) statt `type=5` (abgelehnt), kein Klingeln.
     */
    @Test
    fun `eine gesperrte Nummer wird eingehend abgewiesen`() {
        assertTrue(
            CallBlocking.blocksIncoming("+436641234567", incoming = true, blocked = liste),
        )
    }

    @Test
    fun `abgehend wird nie abgewiesen`() {
        // Sonst waehlt der Nutzer eine Nummer, die er selbst gesperrt hat, und nichts
        // geschieht - ohne dass ihm jemand sagt warum.
        assertFalse(
            CallBlocking.blocksIncoming("+436641234567", incoming = false, blocked = liste),
        )
    }

    @Test
    fun `eine freie Nummer kommt durch`() {
        assertFalse(
            CallBlocking.blocksIncoming("+436649998888", incoming = true, blocked = liste),
        )
    }

    @Test
    fun `der Notruf kommt auch eingehend durch`() {
        assertFalse(CallBlocking.blocksIncoming("112", incoming = true, blocked = listOf("112")))
    }
}
