package org.biglau.ui

import org.biglau.data.Behaviour
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.4: „Bestätigungsdialog statt kurzer Einblendung."
 *
 * Eine Einblendung ist nach zwei Sekunden weg. Wer langsam liest, weiß danach nur, dass
 * etwas aufgeblitzt ist - nicht, was.
 */
class NoticeStyleTest {

    @Test
    fun `ohne die einstellung bleibt es bei der einblendung`() {
        assertEquals(NoticeStyle.TOAST, Notice.styleFor(false))
    }

    @Test
    fun `mit der einstellung wartet die meldung`() {
        assertEquals(NoticeStyle.DIALOG, Notice.styleFor(true))
    }

    // Die Vorgabe ist die Einblendung: eine Meldung, die jedes Mal einen Knopf verlangt,
    // ist fuer die meisten eine Zumutung. Die Wahl gehoert dem, der sie braucht.
    @Test
    fun `die vorgabe ist die einblendung`() {
        assertEquals(false, Behaviour().confirmMessages)
        assertEquals(NoticeStyle.TOAST, Notice.styleFor(Behaviour().confirmMessages))
    }

    /**
     * Und die Meldung selbst ist das Größte auf diesem Bildschirm.
     *
     * Am Gerät gesehen: die Nachricht stand in gewöhnlicher Textgröße über einem großen
     * „Diese Meldung schließen" — der Knopf rief, die Nachricht flüsterte. Auf einem
     * Bildschirm, den es nur wegen dieser einen Zeile gibt, ist das die falsche Rangfolge.
     *
     * `BigHeading` bringt zwei Dinge mit, die eine Meldung braucht: die Überschriftengröße
     * und die Stufenleiter, die **kleiner wird, bevor sie trennt** — eine Meldung darf lang
     * sein, und mitten im Wort getrennt liest sie sich wie ein Fehler.
     */
    @Test
    fun `die Meldung steht in Ueberschriftengroesse`() {
        val quelle = org.biglau.Quelltext.file("org/biglau/ui/NoticeActivity.kt").readText()
        assertEquals(true, "BigHeading(text)" in quelle)
    }
}
