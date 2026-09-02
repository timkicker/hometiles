package org.biglau.ui

import kotlinx.serialization.json.Json
import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `die Kopfzeile waechst mit der Textgroesse`() {
        // Der Fehler: gerechnet wurde nur mit der Uhrgroesse. Bei 150 % Textgroesse blieb
        // die Kopfzeile so hoch wie bei 100 % und schnitt die Datumszeile mitten durch -
        // am Emulator gesehen, nachdem der Assistent mit 150 % durchgelaufen war.
        val bei100 = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.0f)
        val bei150 = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.5f, clockScale = 1.0f)
        assertEquals(78f, bei100, 0.01f)
        assertEquals(117f, bei150, 0.01f)
        assertTrue("groessere Schrift braucht mehr Hoehe", bei150 > bei100)
    }

    @Test
    fun `die Kopfzeile waechst auch mit der Uhrgroesse`() {
        val klein = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.0f)
        val gross = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.5f)
        assertTrue("groessere Uhr braucht mehr Hoehe", gross > klein)
    }

    @Test
    fun `beide Faktoren wirken zusammen`() {
        // Beide Regler zugleich hochgedreht ist der Fall, den ein Mensch mit schlechten
        // Augen wirklich einstellt.
        val beides = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.5f, clockScale = 1.5f)
        assertEquals(78f * 1.5f * 1.5f, beides, 0.01f)
    }

    @Test
    fun `ohne Datumszeile ist die Kopfzeile niedriger`() {
        assertTrue(
            ClockFormat.headerHeightDp(hasDate = false, textScale = 1f, clockScale = 1f) <
                ClockFormat.headerHeightDp(hasDate = true, textScale = 1f, clockScale = 1f),
        )
    }

    @Test
    fun `eine unsinnige Uhrgroesse wird begrenzt`() {
        // headerHeightDp geht durch dieselbe Begrenzung wie die Schrift - sonst liefe die
        // Kopfzeile aus dem Bild, waehrend die Schrift darin stehen bliebe.
        assertEquals(
            ClockFormat.headerHeightDp(hasDate = true, textScale = 1f, clockScale = 99f),
            78f * ClockFormat.scale(99f),
            0.01f,
        )
    }

    @Test
    fun `die Uhr passt neben den Ladestand`() {
        // Bei 200 % liefen Uhr und Ladestand ineinander - "9:37" und "100 %" uebereinander,
        // am Emulator gesehen. Auf den 349 dp des Jelly 2 muss beides nebeneinander passen.
        listOf(1.0f, 1.5f, 2.0f).forEach { skala ->
            val breiteLinks = 349f - 16f - 24f - ClockFormat.batteryWidthDp(skala)
            val groesse = ClockFormat.clockSizeSp("10:38 AM", breiteLinks, skala, 1.0f)
            val gebraucht = "10:38 AM".length * 0.62f * groesse
            assertTrue(
                "bei $skala braucht die Uhr $gebraucht dp, frei sind $breiteLinks",
                gebraucht <= breiteLinks + 0.01f,
            )
        }
    }

    @Test
    fun `bei kleiner Schrift bleibt die Uhr so gross wie gewuenscht`() {
        // Die Begrenzung darf nur greifen, wenn es eng wird - sonst waere die Einstellung
        // fuer die Uhrgroesse eine Attrappe.
        assertEquals(26f, ClockFormat.clockSizeSp("9:37", 300f, 1.0f, 1.0f), 0.01f)
        assertEquals(39f, ClockFormat.clockSizeSp("9:37", 300f, 1.0f, 1.5f), 0.01f)
    }

    @Test
    fun `eine lange Uhrzeit wird kleiner als eine kurze`() {
        val kurz = ClockFormat.clockSizeSp("9:37", 120f, 2.0f, 1.0f)
        val lang = ClockFormat.clockSizeSp("12:38 AM", 120f, 2.0f, 1.0f)
        assertTrue("die laengere Zeit braucht die kleinere Schrift", lang < kurz)
    }

    @Test
    fun `die Uhr wird nie unlesbar klein`() {
        assertEquals(14f, ClockFormat.clockSizeSp("12:38 AM", 10f, 1.0f, 1.0f), 0.01f)
    }

    @Test
    fun `der Ladestand waechst mit der Textgroesse`() {
        assertTrue(ClockFormat.batteryWidthDp(2.0f) > ClockFormat.batteryWidthDp(1.0f))
    }
}
