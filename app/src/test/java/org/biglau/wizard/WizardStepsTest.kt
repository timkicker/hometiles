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
    fun `ein frisches Geraet bekommt alle Schritte`() {
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
    fun `erledigte Schritte werden uebersprungen`() {
        // Wer BigLau schon als Startbildschirm gesetzt hat, soll nicht gefragt werden, ob
        // er das tun moechte - solche Schritte lehren, Assistenten wegzuklicken.
        val steps = WizardSteps.stepsFor(ready)
        assertTrue(WizardStep.HOME_ROLE !in steps)
        assertTrue(WizardStep.PERMISSIONS !in steps)
    }

    @Test
    fun `Textgroesse und Aussehen kommen immer`() {
        listOf(fresh, ready).forEach { state ->
            val steps = WizardSteps.stepsFor(state)
            assertTrue(WizardStep.TEXT_SIZE in steps)
            assertTrue(WizardStep.THEME in steps)
        }
    }

    @Test
    fun `eine fehlende Berechtigung genuegt fuer den Schritt`() {
        val partial = WizardState(isHomeApp = true, hasContacts = true, hasCallPhone = false)
        assertTrue(WizardStep.PERMISSIONS in WizardSteps.stepsFor(partial))
    }

    @Test
    fun `der Weg fuehrt vorwaerts durch die Schritte`() {
        assertEquals(WizardStep.TEXT_SIZE, WizardSteps.next(WizardStep.WELCOME, fresh))
        assertEquals(WizardStep.THEME, WizardSteps.next(WizardStep.TEXT_SIZE, fresh))
    }

    @Test
    fun `uebersprungene Schritte kommen auch beim Vorwaertsgehen nicht vor`() {
        assertEquals(WizardStep.DONE, WizardSteps.next(WizardStep.THEME, ready))
    }

    @Test
    fun `nach dem letzten Schritt geht es nicht weiter`() {
        assertNull(WizardSteps.next(WizardStep.DONE, fresh))
    }

    @Test
    fun `vor dem ersten Schritt gibt es nichts`() {
        assertNull(WizardSteps.previous(WizardStep.WELCOME, fresh))
    }

    @Test
    fun `zurueck fuehrt auf den vorigen gezeigten Schritt`() {
        assertEquals(WizardStep.THEME, WizardSteps.previous(WizardStep.DONE, ready))
        assertEquals(WizardStep.HOME_ROLE, WizardSteps.previous(WizardStep.DONE, fresh))
    }

    @Test
    fun `die Position zaehlt nur gezeigte Schritte`() {
        // "Schritt 2 von 4" - der Nutzer soll sehen, dass es ein Ende gibt, und die Zahl
        // darf keine Schritte mitzaehlen, die gar nicht kommen.
        assertEquals(2 to 4, WizardSteps.position(WizardStep.TEXT_SIZE, ready))
        assertEquals(2 to 6, WizardSteps.position(WizardStep.TEXT_SIZE, fresh))
    }

    @Test
    fun `ein unbekannter Schritt faellt auf den Anfang zurueck`() {
        assertEquals(1 to 4, WizardSteps.position(WizardStep.HOME_ROLE, ready))
    }
}

/**
 * Der Assistent darf keine Einbahnstrasse in die andere Richtung sein.
 *
 * „Einrichtung erneut durchlaufen" setzte frueher `wizardDone` zurueck. Wer dann abbrach,
 * sah bei jedem Start wieder den Assistenten - der fertige Startbildschirm lag hinter einer
 * Frage, die er nie stellen wollte. Dieselbe Falle wie beim Screen ohne Heim-Kachel und bei
 * der ausgeblendeten App.
 */
class WizardReentryTest {

    @Test
    fun `nur eine nie beendete Einrichtung meldet sich beim Start`() {
        assertTrue(WizardSteps.showOnLaunch(wizardDone = false))
        assertFalse(WizardSteps.showOnLaunch(wizardDone = true))
    }

    @Test
    fun `ein erneuter Durchlauf aendert die Marke nicht`() {
        // Der Weg aus den Einstellungen oeffnet den Assistenten, ohne wizardDone anzufassen.
        val fertig = LauncherConfig(wizardDone = true)
        assertFalse(WizardSteps.showOnLaunch(fertig.wizardDone))
    }
}
