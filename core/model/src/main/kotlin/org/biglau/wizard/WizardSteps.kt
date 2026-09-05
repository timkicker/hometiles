package org.biglau.wizard

enum class WizardStep { WELCOME, TEXT_SIZE, THEME, PERMISSIONS, HOME_ROLE, DONE }

/** what is already set up at first launch. */
data class WizardState(
    val isHomeApp: Boolean,
    val hasContacts: Boolean,
    val hasCallPhone: Boolean,
)

/**
 * which steps the wizard shows. it skips what is already done: asking someone to set the home
 * app they have already set teaches them to click wizards away instead of reading them.
 */
object WizardSteps {

    /**
     * only while setup has never run to the end. "run setup again" used to clear that mark,
     * so anyone who then cancelled met the wizard on every launch, with their finished home
     * screen behind a question they never wanted.
     */
    fun showOnLaunch(wizardDone: Boolean): Boolean = !wizardDone

    fun stepsFor(state: WizardState): List<WizardStep> = buildList {
        add(WizardStep.WELCOME)
        add(WizardStep.TEXT_SIZE)
        add(WizardStep.THEME)
        if (!state.hasContacts || !state.hasCallPhone) add(WizardStep.PERMISSIONS)
        if (!state.isHomeApp) add(WizardStep.HOME_ROLE)
        add(WizardStep.DONE)
    }

    fun next(current: WizardStep, state: WizardState): WizardStep? {
        val steps = stepsFor(state)
        val index = steps.indexOf(current)
        return if (index < 0 || index == steps.lastIndex) null else steps[index + 1]
    }

    fun previous(current: WizardStep, state: WizardState): WizardStep? {
        val steps = stepsFor(state)
        val index = steps.indexOf(current)
        return if (index <= 0) null else steps[index - 1]
    }

    /** "step 2 of 5" - one should see that there is an end to it. */
    fun position(current: WizardStep, state: WizardState): Pair<Int, Int> {
        val steps = stepsFor(state)
        val index = steps.indexOf(current)
        return (if (index < 0) 1 else index + 1) to steps.size
    }
}
