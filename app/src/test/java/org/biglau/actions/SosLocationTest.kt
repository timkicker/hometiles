package org.biglau.actions

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Im Notruf zählt die Position von **jetzt**, nicht die von gestern.
 *
 * Die Notruf-SMS nahm die zuletzt bekannte Position. Auf einem Telefon, das in der Tasche
 * liegt, ist die oft Stunden alt — oder es gibt gar keine, weil zufällig lange keine App
 * danach gefragt hat. Am Emulator war sie **immer** leer (`last location=null`), und genau
 * daran ist die erste Prüfung des Kartenlinks gescheitert: `adb emu geo fix` allein füllt sie
 * nicht.
 *
 * Der Countdown ist das Fenster dafür: er dauert ohnehin einige Sekunden, in denen das Telefon
 * suchen kann. Danach wird die Anfrage abgemeldet — ein Empfänger, der weiterläuft, kostet
 * Strom.
 *
 * Mit der Suche während des Countdowns kam der Fix am Emulator sofort an, und im Probe-Text
 * stand: `Ich brauche Hilfe.` + `https://maps.google.com/?q=48.20849,16.37208` — **mit Punkt**
 * als Dezimaltrennzeichen, auf einer deutschsprachigen Oberfläche. Das war bis dahin nur
 * gerechnet, nicht gesehen.
 */
class SosLocationTest {

    private val bildschirm =
        Quelltext.ohneKommentare("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `waehrend des Countdowns wird gesucht`() {
        assertTrue("keine Ortung im Notrufbildschirm", "SosLocation(" in bildschirm)
        assertTrue("die Suche beginnt nicht", "locator.start()" in bildschirm)
    }

    @Test
    fun `nur wenn der Standort ueberhaupt mitgeschickt werden soll`() {
        assertTrue(
            "es wird auch dann geortet, wenn niemand den Standort will",
            "if (sos.sendLocation) locator.start()" in bildschirm,
        )
    }

    @Test
    fun `die Suche hoert wieder auf`() {
        val aufraeumen = Quelltext.ausschnitt(bildschirm, "onDispose {", "}")
        assertTrue("die Ortung laeuft weiter: $aufraeumen", "locator.stop()" in aufraeumen)
    }
}
