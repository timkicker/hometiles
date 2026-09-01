package org.biglau.security

import org.biglau.data.Security
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.5: „PIN zusätzlich für: Kachel-Editor, App-Liste, Deinstallation,
 * Anrufliste löschen".
 *
 * Der Editor war gebaut. Die App-Liste und das Leeren der Anrufliste kommen hier dazu.
 * **Deinstallation nicht**: die gibt es in dieser App gar nicht, und ein Schloss vor einer
 * Tür, die es nicht gibt, ist keine Sicherheit, sondern eine Zeile in den Einstellungen,
 * die nichts tut.
 */
class PinScopeTest {

    private val mitPin = Security(pin = "irgendein-hash")

    /**
     * Ohne gesetzte PIN schützt nichts — auch nicht, wenn der Schalter an ist. Ein
     * eingeschalteter Schutz ohne Schloss wäre eine Zusage, die beim ersten Antippen
     * zerfällt.
     */
    @Test
    fun `ohne pin schuetzt nichts`() {
        val ohne = Security(pin = null, pinProtectsAppList = true, pinProtectsEditor = true)
        assertEquals(false, Pin.protects(ohne.pin, ohne.pinProtectsAppList))
        assertEquals(false, Pin.protectsEditor(ohne.pin, ohne.pinProtectsEditor))
    }

    @Test
    fun `mit pin und schalter schuetzt es`() {
        assertEquals(true, Pin.protects(mitPin.pin, true))
        assertEquals(false, Pin.protects(mitPin.pin, false))
    }

    /**
     * Die App-Liste ist von Haus aus offen: sie ist der Weg zu jeder App, die auf keiner
     * Kachel liegt, und wer eine PIN nur für die Einstellungen setzt, will sich nicht aus
     * seinen eigenen Apps aussperren.
     */
    @Test
    fun `die app-liste ist von haus aus offen`() {
        assertEquals(false, Security().pinProtectsAppList)
    }

    /**
     * Das Leeren der Anrufliste dagegen ist von Haus aus geschützt, sobald eine PIN steht:
     * es ist nicht rückgängig zu machen, und wer eine PIN setzt, will genau solche
     * Schritte gesichert haben.
     */
    @Test
    fun `das leeren der anrufliste ist von haus aus geschuetzt`() {
        assertEquals(true, Security().pinProtectsCallLogDelete)
        assertEquals(true, Pin.protects(mitPin.pin, mitPin.pinProtectsCallLogDelete))
    }

    // Und ohne PIN bleibt es trotzdem offen - sonst waere die Anrufliste fuer jeden ohne
    // PIN unloeschbar.
    @Test
    fun `ohne pin bleibt die anrufliste loeschbar`() {
        val ohne = Security()
        assertEquals(false, Pin.protects(ohne.pin, ohne.pinProtectsCallLogDelete))
    }
}

/**
 * Wird die PIN entfernt, gehen die Schutzschalter mit.
 *
 * Aufgefallen beim Prüfen am Gerät: die Schalter stehen nur da, solange eine PIN gesetzt
 * ist. Bleiben sie beim Entfernen stehen, kommt man nicht mehr an sie heran — und eine
 * später gesetzte PIN sperrt ungefragt Türen zu, die vorher offen waren.
 */
class PinRemovalTest {

    @Test
    fun `ohne pin stehen die schalter wieder auf vorgabe`() {
        val zurueck = Security()
        assertEquals(null, zurueck.pin)
        assertEquals(true, zurueck.pinProtectsEditor)
        assertEquals(false, zurueck.pinProtectsAppList)
        assertEquals(true, zurueck.pinProtectsCallLogDelete)
    }

    // Und dann schuetzt keiner von ihnen etwas.
    @Test
    fun `ohne pin schuetzt keiner der schalter`() {
        val ohne = Security()
        assertEquals(false, Pin.protects(ohne.pin, ohne.pinProtectsEditor))
        assertEquals(false, Pin.protects(ohne.pin, ohne.pinProtectsAppList))
        assertEquals(false, Pin.protects(ohne.pin, ohne.pinProtectsCallLogDelete))
    }
}
