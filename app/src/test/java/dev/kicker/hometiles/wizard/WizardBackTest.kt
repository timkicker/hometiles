package dev.kicker.hometiles.wizard

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the back key leads out of the wizard.
 *
 * before it swallowed the key entirely: `BackHandler(enabled = true)` with no way out.
 * whoever called the wizard again from the settings and wanted out again got there only
 * through the home key or through all four steps.
 *
 * everywhere else in the app back goes back; nobody remembers an exception to that.
 */
class WizardBackTest {

    private val source = Quelltext.file("dev/kicker/hometiles/wizard/WizardActivity.kt").readText()

    /** in the first step there is no step before - closing has to take hold there. */
    @Test
    fun `before the first step lies nothing`() {
        assertNull(WizardSteps.previous(WizardStep.WELCOME, WizardState(isHomeApp = false, hasContacts = false, hasCallPhone = false)))
    }

    @Test
    fun `the wizard closes when back goes no further`() {
        val block = Quelltext.cut(source, "BackHandler(enabled = true)", "}")
        assertTrue("no finish() on the way back: $block", "finish()" in block)
    }
}
