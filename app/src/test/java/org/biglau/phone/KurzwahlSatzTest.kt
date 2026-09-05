package org.biglau.phone

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was eine Kurzwahl tut, steht dort, wo man sie einrichtet.
 *
 * Der Satz gab es schon - „Eine Kurzwahltaste halten ruft sofort an" -, aber nur auf dem
 * Tastenfeld und erst, **wenn schon eine belegt ist**. Wer die erste einrichtet, erfuhr also
 * hinterher, worauf er sich eingelassen hat.
 *
 * Und es ist nicht irgendeine Auskunft: der Langdruck auf eine Kurzwahl ist die **einzige**
 * Stelle, an der BigLau ohne Rueckfrage waehlt. `PLAN.md` 3.1, Leitsatz 5 verlangt die
 * Rueckfrage sonst ueberall, und `CallConfirmTest` haelt sie in der Anrufliste fest. Ob die
 * Kurzwahl selbst fragen soll, steht in `STATUS.md` als deine Entscheidung; bis dahin muss
 * wenigstens dastehen, was passiert.
 */
class KurzwahlSatzTest {

    private val waehler = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")

    @Test
    fun `die Belegung sagt, was der Langdruck spaeter tut`() {
        // AssignList steht am Dateiende, also gibt es keine naechste Funktion als Grenze.
        // Bis zum Ende ist hier genau richtig - der Abschnitt ist die letzte Funktion.
        val liste = Quelltext.cut(waehler, "private fun AssignList(")
        assertTrue(
            "Die Belegung nennt den Satz nicht. Man waehlt einen Kontakt und erfaehrt erst " +
                "danach, dass ein Langdruck ihn ohne Rueckfrage anruft.",
            "R.string.dialer_speeddial_hint_call" in liste,
        )
    }

    /**
     * Und beide Stellen sagen ihn mit **einem** Text.
     *
     * Zwei Fassungen desselben Satzes laufen auseinander; das ist heute Nacht schon zweimal
     * aufgefallen (der MMS-Trost, der Rat zum Neubelegen).
     */
    @Test
    fun `beide Stellen nehmen denselben Satz`() {
        val stellen = Regex("R\\.string\\.dialer_speeddial_hint_call")
            .findAll(waehler).count()
        assertEquals(
            "Der Satz steht nicht an beiden Stellen - Tastenfeld und Belegung -, oder er " +
                "steht dort in zwei Fassungen.",
            2,
            stellen,
        )
    }
}
