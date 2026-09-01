package org.biglau.ui

import org.biglau.data.Appearance
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2 „Statusleiste: sichtbar / Vollbild".
 *
 * Auf diesem Gerät sind die beiden Leisten zusammen rund vierzig von 605 dp - sieben
 * Prozent, die den Kacheln fehlen. Auf einem großen Telefon wäre das Zierrat.
 */
class SystemBarsTest {

    @Test
    fun `die vorgabe zeigt die leisten`() {
        assertEquals(false, Appearance().fullScreen)
        assertEquals(SystemBars.Behaviour.VISIBLE, SystemBars.behaviourFor(false))
    }

    /**
     * Der Fall, auf den es ankommt: verstecken ja, wegsperren nein.
     *
     * Wer die Leisten wegnimmt und dabei das Herunterziehen sperrt, sperrt die
     * Benachrichtigungen weg - und wer nicht weiß, dass man dafür wischen kann, kommt nie
     * wieder an sie heran. Ein paar dp sind das nicht wert, deshalb gibt es gar keinen
     * Zustand, der hart versteckt.
     */
    @Test
    fun `versteckt heisst immer wisch-zum-holen`() {
        assertEquals(SystemBars.Behaviour.HIDDEN_SWIPE_SHOWS, SystemBars.behaviourFor(true))
        assertEquals(2, SystemBars.Behaviour.entries.size)
    }
}
