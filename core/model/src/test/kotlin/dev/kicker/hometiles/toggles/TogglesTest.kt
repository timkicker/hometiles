package dev.kicker.hometiles.toggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TogglesTest {

    @Test
    fun `the flashlight we switch ourselves`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 30))
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 34))
    }

    @Test
    fun `the ringer mode can still be set`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.RINGER, 30))
    }

    @Test
    fun `since android 10 no app may switch wifi`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.WIFI, 28))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 29))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 34))
    }

    @Test
    fun `bluetooth falls away from android 13`() {
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.BLUETOOTH, 30))
        assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.BLUETOOTH, 33))
    }

    @Test
    fun `airplane mode could never be switched by an app`() {
        listOf(24, 29, 30, 34).forEach {
            assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.AIRPLANE, it))
        }
    }

    @Test
    fun `only what switches at once may promise it`() {
        // a tile named after switching, when only a panel opens, promises what does not happen.
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 30))
        assertTrue(Toggles.actionFor(ToggleKind.WIFI, 30) != ToggleAction.SWITCH)
        assertTrue(Toggles.actionFor(ToggleKind.AIRPLANE, 30) != ToggleAction.SWITCH)
    }

    @Test
    fun `on the target device with api 30 it holds`() {
        // Jelly 2, Android 11: flashlight and ringer real, wifi a panel, bluetooth still
        // real, airplane mode settings only.
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.FLASHLIGHT, 30))
        assertEquals(ToggleAction.PANEL, Toggles.actionFor(ToggleKind.WIFI, 30))
        assertEquals(ToggleAction.SWITCH, Toggles.actionFor(ToggleKind.BLUETOOTH, 30))
        assertEquals(ToggleAction.SETTINGS, Toggles.actionFor(ToggleKind.AIRPLANE, 30))
    }
}
