package org.biglau.ui

import kotlinx.serialization.json.Json
import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PLAN.md 4.2: „Uhr auf dem Homescreen: aus / Uhrzeit / Uhrzeit+Datum /
 * Uhrzeit+Datum+Wochentag".
 *
 * Der Wochentag ist die Stufe, die am meisten hilft und am meisten Platz kostet. Wer den
 * Tag nicht sicher weiß — und das ist häufiger, als man denkt, wenn die Tage gleich
 * aussehen —, liest ihn hier ab.
 */
class ClockFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Der Fall, der eine leere Kachel ergeben hätte: „keine Uhr" auf einer Uhr-Kachel.
     * Die Kachel sähe aus, als sei etwas kaputt, und man kann ihr nicht ansehen, dass es
     * eine Einstellung war.
     */
    @Test
    fun `die uhr-kachel zeigt immer die zeit`() {
        assertEquals(true, ClockFormat.showsTime(ClockDisplay.OFF, onTile = true))
        assertEquals(false, ClockFormat.showsTime(ClockDisplay.OFF, onTile = false))
    }

    @Test
    fun `ohne datum keine datumszeile`() {
        assertNull(ClockFormat.datePattern(ClockDisplay.OFF, onTile = false))
        assertNull(ClockFormat.datePattern(ClockDisplay.TIME, onTile = false))
        assertNull(ClockFormat.datePattern(ClockDisplay.TIME, onTile = true))
    }

    @Test
    fun `der wochentag kommt nur in der letzten stufe dazu`() {
        val kopf = ClockFormat.datePattern(ClockDisplay.TIME_DATE, onTile = false)!!
        val kopfMitTag = ClockFormat.datePattern(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false)!!
        assertEquals(false, kopf.contains("E"))
        assertEquals(true, kopfMitTag.contains("E"))
    }

    // In der Kopfzeile ist eine Zeile Platz, auf der Kachel eine ganze Flaeche. "Dienstag"
    // passt dort, in der Kopfzeile muss "Di." reichen.
    @Test
    fun `die kachel darf die langen namen`() {
        assertEquals("EEEE, d. MMMM", ClockFormat.datePattern(ClockDisplay.TIME_DATE_WEEKDAY, onTile = true))
        assertEquals("EEE, d. MMM", ClockFormat.datePattern(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false))
    }

    @Test
    fun `alte konfigurationen werden uebernommen`() {
        assertEquals(
            ClockDisplay.TIME_DATE_WEEKDAY,
            json.decodeFromString<Appearance>("""{"clockShowsDate":true}""").clock,
        )
        assertEquals(
            ClockDisplay.TIME,
            json.decodeFromString<Appearance>("""{"clockShowsDate":false}""").clock,
        )
    }

    @Test
    fun `withClock haelt beide felder gleich`() {
        assertEquals(false, Appearance().withClock(ClockDisplay.TIME).clockShowsDate)
        assertEquals(false, Appearance().withClock(ClockDisplay.OFF).clockShowsDate)
        assertEquals(true, Appearance().withClock(ClockDisplay.TIME_DATE).clockShowsDate)
        assertEquals(
            ClockDisplay.TIME_DATE,
            Appearance().withClock(ClockDisplay.TIME_DATE).clock,
        )
    }

    // Die Vorgabe ist, was die App bisher gemalt hat.
    @Test
    fun `die vorgabe zeigt alles`() {
        assertEquals(ClockDisplay.TIME_DATE_WEEKDAY, Appearance().clock)
    }
}

/**
 * PLAN.md 4.2: die Uhr „Größe frei".
 *
 * Die Kopfzeile ist die einzige Stelle, an der die Uhrzeit steht, wenn das Vollbild die
 * Systemleiste wegnimmt. Wer sie dort nicht lesen kann, hat keine zweite.
 */
class ClockScaleTest {

    @Test
    fun `die vorgabe aendert nichts`() {
        assertEquals(1.0f, org.biglau.data.Appearance().clockScale, 0.001f)
        assertEquals(1.0f, ClockFormat.scale(1.0f), 0.001f)
    }

    // Eine importierte Datei kann alles enthalten. Eine Uhr in Groesse null waere eine
    // leere Kopfzeile, eine in Groesse zehn schoebe alles andere heraus.
    @Test
    fun `unmoegliche werte werden beschnitten`() {
        assertEquals(ClockFormat.SCALE_MIN, ClockFormat.scale(0f), 0.001f)
        assertEquals(ClockFormat.SCALE_MAX, ClockFormat.scale(10f), 0.001f)
    }

    @Test
    fun `jede angebotene stufe liegt im erlaubten bereich`() {
        for (wert in ClockFormat.SCALES) {
            assertEquals(wert, ClockFormat.scale(wert), 0.001f)
        }
    }

    @Test
    fun `die vorgabe steht auch zur wahl`() {
        assertEquals(true, ClockFormat.SCALES.contains(org.biglau.data.Appearance().clockScale))
    }
}
