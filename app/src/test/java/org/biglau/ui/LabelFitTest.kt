package org.biglau.ui

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Label ausblenden, wenn es nicht in zwei Zeilen passt" — `PLAN.md` 3.2.
 *
 * Der Plan sagt die Option zu, es gab sie nicht. Und der erste Versuch, sie zu bauen, ging
 * an derselben Falle vorbei, vor der dieses Projekt sonst warnt: er **schätzte** mit einer
 * mittleren Zeichenbreite, statt zu messen. Am Bildschirm nachgesehen kam heraus, dass die
 * Schätzung „Nachrichten" auf dem Standardraster ausgeblendet hätte, obwohl es dort
 * vollständig steht — und dass „Messages" auf vier Spalten abschneidet, obwohl die
 * Schätzung „passt" sagte. Zwei Fehler in beide Richtungen, mit einer Zahl, die für Atkinson
 * gar nicht stimmt.
 *
 * Jetzt misst `BigTile` mit dem `TextMeasurer`, bevor gezeichnet wird. Hier bleibt nur die
 * Rechnung, wie viel Platz da ist.
 */
class LabelFitTest {

    @Test
    fun `die Beschriftung bekommt die Kachel ohne ihre Raender`() {
        // 166 dp breit, 186 dp hoch: Rand ist 186 * 0,06 = 11,16 dp je Seite.
        assertEquals(143.68f, labelWidthDp(166f, 186f), 0.01f)
    }

    /** Der Rand ist gedeckelt, sonst fraesse er auf hohen Kacheln die halbe Breite. */
    @Test
    fun `der Rand bleibt zwischen sechs und sechzehn`() {
        assertEquals(166f - 12f, labelWidthDp(166f, 50f), 0.01f)
        assertEquals(166f - 32f, labelWidthDp(166f, 400f), 0.01f)
    }

    @Test
    fun `auf einer schmalen Kachel bleibt trotzdem eine Breite uebrig`() {
        assertTrue(labelWidthDp(10f, 186f) >= 1f)
    }

    /** Und die Messung selbst steht in der Kachel, nicht als Schätzung daneben. */
    @Test
    fun `gemessen wird mit dem TextMeasurer`() {
        val quelle = Quelltext.file("org/biglau/ui/BigTile.kt").readText()
        assertTrue("rememberTextMeasurer fehlt", "rememberTextMeasurer()" in quelle)
        assertTrue("hasVisualOverflow fehlt", "hasVisualOverflow" in quelle)
    }

    // --- Erst kleiner werden, dann abschneiden (02.09.2026) ---

    /**
     * Bei 200 % App-Schrift auf 1,35-facher Systemschrift stand auf den Kacheln
     * „Einstellun…" und „Verpasste …". Wer 200 % einstellt, tut das nicht zum Spass — ein
     * abgeschnittenes Wort hilft ihm nicht, ein etwas kleineres schon.
     */
    @Test
    fun `die Leiter beginnt beim Wunsch und endet bei siebzig Prozent`() {
        val leiter = labelLadder(40f)
        assertEquals(40f, leiter.first(), 0.01f)
        assertEquals(28f, leiter.last(), 0.01f)
    }

    @Test
    fun `die Leiter wird Stufe fuer Stufe kleiner`() {
        val leiter = labelLadder(24f)
        leiter.zipWithNext().forEach { (gross, klein) ->
            assertTrue("$klein muesste kleiner sein als $gross", klein < gross)
        }
    }

    @Test
    fun `auch die kleinste Stufe bleibt eine Groesse`() {
        // Sonst waere die Beschriftung bei winzigen Kacheln rechnerisch weg, statt zu
        // weichen - und ein Text mit Groesse null ist kein Text, sondern ein Fehler.
        labelLadder(14f).forEach { assertTrue(it > 0f) }
    }
}
