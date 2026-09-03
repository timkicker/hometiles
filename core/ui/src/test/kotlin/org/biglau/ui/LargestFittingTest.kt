package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Suche nach der größten Schriftgröße, die noch in eine Zeile passt.
 *
 * Sie steckt hinter `fittedSingleLineDp`, und dort ist sie nicht zu prüfen — dafür bräuchte
 * es einen Bildschirm. Hier ist sie es: die Frage „passt das?" kommt als Funktion herein,
 * und der Test kann sie beantworten und dabei mitzählen.
 */
class LargestFittingTest {

    @Test
    fun `passt der Wunsch, bleibt es beim Wunsch`() {
        assertEquals(40f, largestFitting(40f, 12f) { true }, 0.01f)
    }

    @Test
    fun `passt nichts, bleibt die Untergrenze`() {
        assertEquals(12f, largestFitting(40f, 12f) { false }, 0.01f)
    }

    @Test
    fun `gefunden wird die groesste passende Stufe`() {
        // Alles bis 23 dp passt, darüber nicht.
        assertEquals(23f, largestFitting(64f, 12f) { it <= 23f }, 0.01f)
    }

    /**
     * Und zwar mit wenigen Fragen. Die Uhr wechselt jede Minute ihren Text; Schritt für
     * Schritt wären es von 64 dp abwärts über vierzig Textvermessungen, halbierend sind es
     * eine Handvoll. Die Zahl steht hier, damit ein Rückschritt auffällt.
     */
    @Test
    fun `es braucht wenige Messungen`() {
        var fragen = 0
        val ergebnis = largestFitting(64f, 12f) { fragen++; it <= 23f }
        assertEquals(23f, ergebnis, 0.01f)
        assertTrue("zu viele Messungen: $fragen", fragen <= 8)
    }

    @Test
    fun `ein Wunsch unter der Untergrenze gibt die Untergrenze`() {
        assertEquals(12f, largestFitting(10f, 12f) { true }, 0.01f)
    }
}
