package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the wizard's last sentence has to be true.
 *
 * it ended by promising that holding a tile changes what the tile does - the sentence one
 * keeps. it is not always true: whoever switches on the spoken feedback or the long-press
 * popup no longer reaches the editor that way. `LongPress.needsEditModeEntry` says exactly
 * that, and the settings then offer the other way expressly (`a11y_editor_moved`). only the
 * wizard kept promising the long press.
 *
 * and it hits the people it is about: nobody switches on the spoken feedback by accident but
 * because they see badly - the very person this app is built for. they would get a closing
 * piece of advice that does not work for them.
 */
class FinalSentenceTest {

    private val wizard = Quelltext.withoutComments("dev/kicker/hometiles/wizard/WizardActivity.kt")

    @Test
    fun `the closing sentence asks whether the long press leads there at all`() {
        val from = wizard.indexOf("WizardStep.DONE")
        assertTrue("the closing step does not exist any more", from > 0)
        val step = wizard.substring(from, minOf(wizard.length, from + 700))
        assertTrue(
            "the wizard promises the long press without looking whether it leads to the " +
                "editor. with the spoken feedback on it does not.",
            "needsEditModeEntry" in step,
        )
        assertTrue(
            "there is no second sentence for the case that the long press does not lead there.",
            "wizard_done_body_edit_mode" in step,
        )
    }

    @Test
    fun `both sentences exist in both languages`() {
        val missing = listOf("wizard_done_body", "wizard_done_body_edit_mode").flatMap { name ->
            listOf("values", "values-de").filterNot { language ->
                Quelltext.texts(language).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertTrue("a closing sentence is missing in one language: $missing", missing.isEmpty())
    }
}
