package org.biglau.wizard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.biglau.data.LauncherConfig
import org.junit.Assert.assertFalse
import org.junit.Test

class WizardStepsTest {

    private val fresh = WizardState(isHomeApp = false, hasContacts = false, hasCallPhone = false)
    private val ready = WizardState(isHomeApp = true, hasContacts = true, hasCallPhone = true)

    @Test
    fun `a fresh device gets all steps`() {
        assertEquals(
            listOf(
                WizardStep.WELCOME,
                WizardStep.TEXT_SIZE,
                WizardStep.THEME,
                WizardStep.PERMISSIONS,
                WizardStep.HOME_ROLE,
                WizardStep.DONE,
            ),
            WizardSteps.stepsFor(fresh),
        )
    }

    @Test
    fun `steps already done are skipped`() {
        // whoever has set BigLau as the home screen must not be asked whether they want to -
        // steps like that teach people to click wizards away.
        val steps = WizardSteps.stepsFor(ready)
        assertTrue(WizardStep.HOME_ROLE !in steps)
        assertTrue(WizardStep.PERMISSIONS !in steps)
    }

    @Test
    fun `text size and look always come`() {
        listOf(fresh, ready).forEach { state ->
            val steps = WizardSteps.stepsFor(state)
            assertTrue(WizardStep.TEXT_SIZE in steps)
            assertTrue(WizardStep.THEME in steps)
        }
    }

    @Test
    fun `one missing permission is enough for the step`() {
        val partial = WizardState(isHomeApp = true, hasContacts = true, hasCallPhone = false)
        assertTrue(WizardStep.PERMISSIONS in WizardSteps.stepsFor(partial))
    }

    @Test
    fun `the way leads forward through the steps`() {
        assertEquals(WizardStep.TEXT_SIZE, WizardSteps.next(WizardStep.WELCOME, fresh))
        assertEquals(WizardStep.THEME, WizardSteps.next(WizardStep.TEXT_SIZE, fresh))
    }

    @Test
    fun `skipped steps do not appear when going forward either`() {
        assertEquals(WizardStep.DONE, WizardSteps.next(WizardStep.THEME, ready))
    }

    @Test
    fun `after the last step it does not go on`() {
        assertNull(WizardSteps.next(WizardStep.DONE, fresh))
    }

    @Test
    fun `before the first step there is nothing`() {
        assertNull(WizardSteps.previous(WizardStep.WELCOME, fresh))
    }

    @Test
    fun `back leads to the previous shown step`() {
        assertEquals(WizardStep.THEME, WizardSteps.previous(WizardStep.DONE, ready))
        assertEquals(WizardStep.HOME_ROLE, WizardSteps.previous(WizardStep.DONE, fresh))
    }

    @Test
    fun `the position counts only shown steps`() {
        // "step 2 of 4" - one should see that there is an end, and the number must not count
        // steps that never come.
        assertEquals(2 to 4, WizardSteps.position(WizardStep.TEXT_SIZE, ready))
        assertEquals(2 to 6, WizardSteps.position(WizardStep.TEXT_SIZE, fresh))
    }

    @Test
    fun `an unknown step falls back to the start`() {
        assertEquals(1 to 4, WizardSteps.position(WizardStep.HOME_ROLE, ready))
    }
}

/**
 * the wizard must not be a one-way street in the other direction.
 *
 * running the setup again used to reset `wizardDone`. whoever then broke off saw the wizard at
 * every start - the finished home screen lay behind a question they never wanted to answer.
 */
class WizardReentryTest {

    @Test
    fun `only a setup never finished reports at the start`() {
        assertTrue(WizardSteps.showOnLaunch(wizardDone = false))
        assertFalse(WizardSteps.showOnLaunch(wizardDone = true))
    }

    @Test
    fun `a second run does not change the mark`() {
        // the way from the settings opens the wizard without touching wizardDone.
        val done = LauncherConfig(wizardDone = true)
        assertFalse(WizardSteps.showOnLaunch(done.wizardDone))
    }
}
