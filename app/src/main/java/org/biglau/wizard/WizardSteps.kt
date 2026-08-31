package org.biglau.wizard

enum class WizardStep { WELCOME, TEXT_SIZE, THEME, PERMISSIONS, HOME_ROLE, DONE }

/** Was beim Start schon eingerichtet ist. */
data class WizardState(
    val isHomeApp: Boolean,
    val hasContacts: Boolean,
    val hasCallPhone: Boolean,
)

/**
 * Welche Schritte der Assistent zeigt.
 *
 * Er ueberspringt, was schon erledigt ist. Wer BigLau bereits als Startbildschirm gesetzt
 * hat, soll nicht gefragt werden, ob er das tun moechte - solche Schritte lehren den Nutzer,
 * Assistenten wegzuklicken statt sie zu lesen.
 *
 * Textgroesse und Aussehen kommen immer, weil sie keine Vorbedingung haben und weil sie das
 * sind, wofuer man diese App ueberhaupt installiert.
 */
object WizardSteps {

    /**
     * Zeigt der Start den Assistenten?
     *
     * Nur, solange die Einrichtung nie zu Ende gelaufen ist. „Einrichtung erneut
     * durchlaufen" setzte diese Marke frueher zurueck - wer dann abbrach, bekam bei
     * jedem Start wieder den Assistenten, und der fertige Startbildschirm lag hinter
     * einer Frage, die er nie stellen wollte. Ein erneuter Durchlauf oeffnet den
     * Assistenten deshalb direkt und ruehrt die Marke nicht an.
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

    /** "Schritt 2 von 5" - der Nutzer soll sehen, dass es ein Ende gibt. */
    fun position(current: WizardStep, state: WizardState): Pair<Int, Int> {
        val steps = stepsFor(state)
        val index = steps.indexOf(current)
        return (if (index < 0) 1 else index + 1) to steps.size
    }
}
