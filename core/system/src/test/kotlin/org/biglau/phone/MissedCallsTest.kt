package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das Abzeichen für verpasste Anrufe braucht kein Schreibrecht.
 *
 * Vorher zählte BigLau nur das Kennzeichen `NEW = 1` des Systems und setzte es beim Öffnen
 * der Liste zurück. Das verlangt `WRITE_CALL_LOG`, und auf dem Telefon des Nutzers ist
 * genau dieses Recht **nicht erteilt** — die Zahl auf der Kachel wäre dort nie erloschen,
 * egal wie oft er die Liste liest. Ein Abzeichen ist kein Grund, das Recht zum *Ändern* der
 * Anrufliste zu verlangen.
 */
class MissedCallsTest {

    private fun anruf(id: Long, zeit: Long) = CallEntry(
        id = id,
        number = "+43123456789",
        name = null,
        direction = CallDirection.MISSED,
        timestamp = zeit,
        durationSeconds = 0,
    )

    @Test
    fun `beide Bedingungen stehen in der Abfrage`() {
        val bedingung = MissedCalls.selection()
        assertTrue("das Kennzeichen des Systems fehlt: $bedingung", "new = 1" in bedingung.lowercase())
        assertTrue("der Zeitpunkt fehlt: $bedingung", "date >" in bedingung.lowercase())
    }

    @Test
    fun `die Werte stehen in derselben Reihenfolge wie die Fragezeichen`() {
        val werte = MissedCalls.arguments(1_700_000_000_000L)
        assertEquals(2, werte.size)
        assertEquals("1700000000000", werte[1])
    }

    /** Ein negativer Zeitpunkt käme aus einer kaputten Sicherung - er zählt als „noch nie". */
    @Test
    fun `ein negativer Zeitpunkt wird zu null`() {
        assertEquals("0", MissedCalls.arguments(-5L)[1])
    }

    /**
     * Gemerkt wird der **jüngste Anruf der Liste**, nicht „jetzt".
     *
     * Zwischen dem Auslesen und dem Speichern vergeht Zeit. Mit „jetzt" wäre ein Anruf, der
     * genau dazwischen ankommt, als gesehen abgehakt - und der Nutzer hätte ihn nie
     * gesehen. Das ist der einzige Fehler, den dieses Abzeichen machen darf: lieber einmal
     * zu viel anzeigen als einen verschlucken.
     */
    @Test
    fun `gemerkt wird der juengste Anruf, nicht die aktuelle Zeit`() {
        val liste = listOf(anruf(1, 500L), anruf(2, 900L), anruf(3, 700L))
        assertEquals(900L, MissedCalls.seenUpTo(previous = 0L, entries = liste))
    }

    @Test
    fun `eine leere Liste laesst den Zeitpunkt stehen`() {
        assertEquals(1234L, MissedCalls.seenUpTo(previous = 1234L, entries = emptyList()))
    }

    /** Und der Zeitpunkt geht nie zurück - sonst käme eine alte Zahl wieder. */
    @Test
    fun `ein aelterer Anruf setzt den Zeitpunkt nicht zurueck`() {
        assertEquals(1000L, MissedCalls.seenUpTo(previous = 1000L, entries = listOf(anruf(1, 300L))))
    }
}
