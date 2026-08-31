package org.biglau.toggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TogglesTest {

    @Test
    fun `die Taschenlampe legen wir selbst um`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 30))
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 34))
    }

    @Test
    fun `der Klingelmodus laesst sich weiterhin setzen`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.RINGER, 30))
    }

    @Test
    fun `WLAN darf seit Android 10 keine App mehr schalten`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.WIFI, 28))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 29))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 34))
    }

    @Test
    fun `Bluetooth faellt ab Android 13 weg`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.BLUETOOTH, 30))
        assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.BLUETOOTH, 33))
    }

    @Test
    fun `Flugmodus konnte nie eine App schalten`() {
        listOf(24, 29, 30, 34).forEach {
            assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.AIRPLANE, it))
        }
    }

    @Test
    fun `nur was sofort schaltet darf das auch versprechen`() {
        // Wer die Kachel "WLAN einschalten" nennt, obwohl sich nur eine Blende oeffnet,
        // verspricht etwas, das nicht eintritt.
        assertTrue(Toggles.switchesImmediately(ToggleKind.FLASHLIGHT, 30))
        assertTrue(!Toggles.switchesImmediately(ToggleKind.WIFI, 30))
        assertTrue(!Toggles.switchesImmediately(ToggleKind.AIRPLANE, 30))
    }

    @Test
    fun `auf dem Zielgeraet mit API 30 gilt`() {
        // Jelly 2, Android 11: Taschenlampe und Klingelmodus echt, WLAN als Blende,
        // Bluetooth noch echt, Flugmodus nur Einstellungen.
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 30))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 30))
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.BLUETOOTH, 30))
        assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.AIRPLANE, 30))
    }
}
