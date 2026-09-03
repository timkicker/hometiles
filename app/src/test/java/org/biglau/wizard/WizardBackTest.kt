package org.biglau.wizard

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aus dem Assistenten führt die Zurück-Taste heraus.
 *
 * Vorher verschluckte er sie ganz: `BackHandler(enabled = true)` ohne Ausweg. Wer ihn aus
 * den Einstellungen noch einmal aufrief und wieder heraus wollte, kam nur über die
 * Heim-Taste oder durch alle vier Schritte. Am Emulator nachgestellt — zweimal Zurück, und
 * er stand weiter auf Schritt 1.
 *
 * Überall sonst in der App führt Zurück zurück; eine Ausnahme davon merkt sich niemand.
 */
class WizardBackTest {

    private val quelle = Quelltext.datei("org/biglau/wizard/WizardActivity.kt").readText()

    /** Im ersten Schritt gibt es keinen Schritt davor - dort muss das Schliessen greifen. */
    @Test
    fun `vor dem ersten Schritt liegt nichts`() {
        assertNull(WizardSteps.previous(WizardStep.WELCOME, WizardState(isHomeApp = false, hasContacts = false, hasCallPhone = false)))
    }

    @Test
    fun `der Assistent schliesst sich, wenn es nicht weiter zurueckgeht`() {
        val block = quelle.substringAfter("BackHandler(enabled = true)").substringBefore("}")
        assertTrue("Kein finish() im Zurueck-Weg: $block", "finish()" in block)
    }
}
