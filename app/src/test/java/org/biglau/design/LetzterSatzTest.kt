package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der letzte Satz des Assistenten muss stimmen.
 *
 * Er endete mit „Halten Sie eine Kachel gedrueckt, um zu aendern, was sie tut." - und das
 * ist der Satz, den man behaelt. Er stimmt aber nicht immer: wer sich das Vorlesen oder das
 * Popup beim Langdruck einschaltet, erreicht den Editor so nicht mehr.
 * `LongPress.needsEditModeEntry` sagt genau das, und die Einstellungen bieten dann
 * ausdruecklich den anderen Weg an (`a11y_editor_moved`). Nur der Assistent versprach
 * weiter den Langdruck.
 *
 * Und es trifft die, um die es geht: das Vorlesen beim Langdruck schaltet sich niemand aus
 * Versehen ein, sondern weil er schlecht sieht - also genau der Mensch, fuer den diese App
 * gebaut ist. Er bekaeme als Abschluss einen Rat, der bei ihm nicht funktioniert.
 */
class LetzterSatzTest {

    private val assistent = Quelltext.ohneKommentare("org/biglau/wizard/WizardActivity.kt")

    @Test
    fun `der Schlusssatz fragt, ob der Langdruck ueberhaupt hinfuehrt`() {
        val ab = assistent.indexOf("WizardStep.DONE")
        assertTrue("Den Schlussschritt gibt es nicht mehr", ab > 0)
        val schritt = assistent.substring(ab, minOf(assistent.length, ab + 700))
        assertTrue(
            "Der Assistent verspricht den Langdruck ohne nachzusehen, ob er zum Editor " +
                "fuehrt. Bei eingeschaltetem Vorlesen tut er das nicht.",
            "needsEditModeEntry" in schritt,
        )
        assertTrue(
            "Es gibt keinen zweiten Satz fuer den Fall, dass der Langdruck nicht hinfuehrt.",
            "wizard_done_body_edit_mode" in schritt,
        )
    }

    @Test
    fun `beide Saetze gibt es in beiden Sprachen`() {
        val fehlt = listOf("wizard_done_body", "wizard_done_body_edit_mode").flatMap { name ->
            listOf("values", "values-de").filterNot { sprache ->
                Quelltext.texte(sprache).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertTrue("Ein Schlusssatz fehlt in einer Sprache: $fehlt", fehlt.isEmpty())
    }
}
