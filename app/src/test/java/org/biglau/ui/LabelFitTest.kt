package org.biglau.ui

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
        val quelle = File("src/main/java/org/biglau/ui/BigTile.kt").readText()
        assertTrue("rememberTextMeasurer fehlt", "rememberTextMeasurer()" in quelle)
        assertTrue("hasVisualOverflow fehlt", "hasVisualOverflow" in quelle)
    }
}
