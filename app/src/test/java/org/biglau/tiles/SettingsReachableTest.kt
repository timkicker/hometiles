package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die schlimmste Sackgasse der ganzen App.
 *
 * Wer einen Screen zum Startbildschirm macht, auf dem keine Einstellungen-Kachel liegt,
 * kommt nie wieder in die Einstellungen - und damit auch nie wieder zurück. Es hilft dann
 * nur noch ein anderer Launcher oder ein Rechner mit adb. Genau das ist beim Ausprobieren
 * am 01.09.2026 passiert, und es hat einen Eingriff in die Konfigurationsdatei gebraucht.
 */
class SettingsReachableTest {

    private fun funktion(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    private fun sprung(x: Int, y: Int, ziel: String) =
        Cell(x, y, button = Button(action = ButtonAction.GoToScreen(ziel)))

    private fun ordner(x: Int, y: Int, ziel: String) =
        Cell(x, y, button = Button(action = ButtonAction.Folder(ziel)))

    @Test
    fun `eine Einstellungen-Kachel auf dem Screen selbst genuegt`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(funktion(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(screen)), "a"))
    }

    @Test
    fun `ohne sie ist es eine Sackgasse`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(funktion(0, 0, Builtin.DIALER)))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(screen)), "a"))
    }

    @Test
    fun `ein Weg ueber eine Sprungkachel zaehlt`() {
        val a = Screen(id = "a", name = "A", cells = listOf(sprung(0, 0, "b")))
        val b = Screen(id = "b", name = "B", cells = listOf(funktion(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, b)), "a"))
    }

    @Test
    fun `ein Weg ueber einen Ordner auch`() {
        val a = Screen(id = "a", name = "A", cells = listOf(ordner(0, 0, "f")))
        val f = Screen(id = "f", name = "F", kind = ScreenKind.FOLDER, cells = listOf(funktion(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, f)), "a"))
    }

    @Test
    fun `ein Ring laeuft nicht endlos`() {
        // Zwei Screens, die aufeinander zeigen, und nirgends Einstellungen.
        val a = Screen(id = "a", name = "A", cells = listOf(sprung(0, 0, "b")))
        val b = Screen(id = "b", name = "B", cells = listOf(sprung(0, 0, "a")))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, b)), "a"))
    }

    @Test
    fun `die App-Liste zaehlt nicht als Weg`() {
        // BigLau steht zwar in der App-Liste, aber ein Start von dort fuehrt auf den
        // Startbildschirm - nicht in die Einstellungen.
        val a = Screen(id = "a", name = "A", cells = listOf(funktion(0, 0, Builtin.APP_LIST)))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a)), "a"))
    }

    @Test
    fun `eine Sprungkachel ins Leere zaehlt nicht`() {
        val a = Screen(id = "a", name = "A", cells = listOf(sprung(0, 0, "gibtsnicht")))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a)), "a"))
    }

    @Test
    fun `der heutige Startbildschirm ist in Ordnung`() {
        val heim = Screen(
            id = "home",
            name = "Start",
            cells = listOf(
                funktion(0, 0, Builtin.DIALER),
                funktion(1, 0, Builtin.MESSAGES),
                funktion(0, 1, Builtin.CONTACTS),
                funktion(1, 1, Builtin.CAMERA),
                funktion(0, 2, Builtin.APP_LIST),
                funktion(1, 2, Builtin.SETTINGS),
            ),
        )
        val zwei = Screen(id = "screen2", name = "Screen 2", cells = listOf(funktion(1, 2, Builtin.HOME_SCREEN)))
        val config = LauncherConfig(screens = listOf(heim, zwei), homeScreenId = "home")
        assertTrue(ScreenEdits.settingsReachable(config, "home"))
        // Und der Screen, in dem ich heute Nacht steckengeblieben bin, eben nicht.
        assertFalse(ScreenEdits.settingsReachable(config, "screen2"))
    }
}

/**
 * Der Ausweg: eine Einstellungen-Kachel anlegen, statt den Wechsel bloß zu verbieten.
 *
 * Verbieten wäre bevormundend und würde das Versprechen brechen, dass alles frei belegbar
 * ist. Eine Kachel anzubieten löst dasselbe Problem und lässt die Entscheidung beim Nutzer.
 */
class SettingsTileFallbackTest {

    private fun funktion(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    @Test
    fun `die Kachel landet auf dem ersten freien Platz`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(funktion(0, 0, Builtin.DIALER)))
        val config = LauncherConfig(screens = listOf(screen))
        val danach = ScreenEdits.withSettingsTile(config, "a")!!
        assertTrue(ScreenEdits.settingsReachable(danach, "a"))
        val neu = danach.screens.first().cells.first { it.x == 1 && it.y == 0 }
        assertTrue((neu.button.action as ButtonAction.Action).builtin == Builtin.SETTINGS)
    }

    @Test
    fun `auf einem vollen Screen geht es nicht`() {
        // Dann darf auch der Wechsel nicht stattfinden - sonst ist der Weg zurueck weg.
        val voll = Screen(
            id = "a",
            name = "A",
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> funktion(x, y, Builtin.DIALER) } },
        )
        assertNull(ScreenEdits.withSettingsTile(LauncherConfig(screens = listOf(voll)), "a"))
    }
}
