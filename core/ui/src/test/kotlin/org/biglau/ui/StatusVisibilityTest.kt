package org.biglau.ui

import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2 verlangt eine „eigene große Batterie- und Signalanzeige (fürs Vollbild)".
 * Der Grund dahinter ist eine Frage: sieht der Nutzer noch, wie spät es ist und wie voll
 * der Akku?
 *
 * Im Vollbild ist die Systemleiste weg; die Kopfzeile der App trägt beides. Beides zugleich
 * auszuschalten ist erlaubt — es ist ein Startbildschirm, kein Cockpit —, aber es ist die
 * Sorte Einstellung, die man versehentlich trifft und dann nicht mehr zuordnet: das Telefon
 * zeigt einfach keine Uhrzeit mehr, und man sucht den Fehler beim Telefon.
 */
class StatusVisibilityTest {

    @Test
    fun `ohne vollbild traegt die systemleiste alles`() {
        val ohne = Appearance(fullScreen = false, showHeader = false)
        assertEquals(true, StatusVisibility.showsTime(ohne))
        assertEquals(true, StatusVisibility.showsBattery(ohne))
        assertEquals(false, StatusVisibility.warns(ohne))
    }

    @Test
    fun `im vollbild traegt die kopfzeile alles`() {
        val mitKopf = Appearance(fullScreen = true, showHeader = true)
        assertEquals(true, StatusVisibility.showsTime(mitKopf))
        assertEquals(true, StatusVisibility.showsBattery(mitKopf))
        assertEquals(false, StatusVisibility.warns(mitKopf))
    }

    // Der Fall, um den es geht.
    @Test
    fun `vollbild ohne kopfzeile zeigt nichts mehr`() {
        val blind = Appearance(fullScreen = true, showHeader = false)
        assertEquals(false, StatusVisibility.showsTime(blind))
        assertEquals(false, StatusVisibility.showsBattery(blind))
        assertEquals(true, StatusVisibility.warns(blind))
    }

    /**
     * Und der halbe Fall: Kopfzeile an, Uhr aus. Dann steht der Ladestand da und die
     * Uhrzeit nicht — die Warnung muss das unterscheiden, sonst nennt sie etwas, das
     * sichtbar dasteht, und wird unglaubwürdig.
     */
    @Test
    fun `mit kopfzeile aber ohne uhr fehlt nur die zeit`() {
        val ohneUhr = Appearance(
            fullScreen = true,
            showHeader = true,
            clockDisplay = ClockDisplay.OFF,
        )
        assertEquals(false, StatusVisibility.showsTime(ohneUhr))
        assertEquals(true, StatusVisibility.showsBattery(ohneUhr))
        assertEquals(true, StatusVisibility.warns(ohneUhr))
    }

    // Die Vorgabe warnt nie - sonst stuende auf einer frisch eingerichteten App eine rote
    // Zeile, und rote Zeilen, die immer da sind, liest niemand mehr.
    @Test
    fun `die vorgabe warnt nicht`() {
        assertEquals(false, StatusVisibility.warns(Appearance()))
    }
}
