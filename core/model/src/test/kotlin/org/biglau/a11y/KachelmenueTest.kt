package org.biglau.a11y

import org.biglau.data.Accessibility
import org.biglau.data.PressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Liste der Menuetaste zeigt alle drei, egal was eingestellt ist.
 *
 * PLAN.md 10.3.4. Das ist der ganze Zweck der Liste, und es ist der einzige Punkt, an dem sie
 * sich von [LongPress.decide] unterscheiden muss.
 *
 * Der Fall, um den es geht: wer das Vorlesen einschaltet, bekommt beim langen Druck nur noch
 * das Vorlesen. Am Finger ist das richtig und umstellbar. Mit Tasten ist es eine Sackgasse,
 * denn die Einstellung, die es zurueckdreht, steht hinter dem Editor, und in den Editor kommt
 * man dann nicht mehr.
 */
class KachelmenueTest {

    @Test
    fun `die Liste zeigt alle drei`() {
        assertEquals(
            listOf(Menuepunkt.BEARBEITEN, Menuepunkt.VORLESEN, Menuepunkt.GROSS_ZEIGEN),
            Kachelmenue.punkte(),
        )
    }

    /**
     * Der Kern: was der lange Druck je nach Einstellung weglaesst, laesst die Liste nie weg.
     */
    @Test
    fun `keine Einstellung nimmt der Liste einen Punkt`() {
        listOf(
            Accessibility(),
            Accessibility(speakOnLongPress = true),
            Accessibility(popupOnLongPress = true),
            Accessibility(speakOnLongPress = true, popupOnLongPress = true),
        ).forEach { einstellung ->
            listOf(PressMode.SHORT, PressMode.LONG).forEach { modus ->
                val amLangdruck = LongPress.decide(einstellung, editMode = false, pressMode = modus)
                assertTrue(
                    "Der lange Druck laesst hier etwas weg, das ist in Ordnung: $amLangdruck",
                    amLangdruck.size < 3,
                )
                assertEquals(
                    "Die Liste muss trotzdem alle drei zeigen, sonst ist sie fuer Tasten " +
                        "genauso eng wie der lange Druck",
                    3,
                    Kachelmenue.punkte().size,
                )
            }
        }
    }

    @Test
    fun `die Zweitbelegung steht oben`() {
        val mit = Kachelmenue.punkte(hasSecondAction = true)
        assertEquals("sie gehoert zu genau dieser Kachel und geht vor", Menuepunkt.ZWEITE_AKTION, mit.first())
        assertEquals(4, mit.size)
    }

    @Test
    fun `ohne Zweitbelegung steht sie nicht da`() {
        assertTrue(Menuepunkt.ZWEITE_AKTION !in Kachelmenue.punkte(hasSecondAction = false))
    }
}
