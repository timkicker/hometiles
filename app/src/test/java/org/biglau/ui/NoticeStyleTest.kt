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
}
