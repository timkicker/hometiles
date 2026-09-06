package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the worst dead end in the whole app.
 *
 * whoever makes a screen the home screen that carries no settings tile never gets into the
 * settings again - and so never back either. only another launcher or a computer with adb
 * helps then. that happened while trying things out on 01.09.2026 and took an edit of the
 * configuration file.
 */
class SettingsReachableTest {

    private fun builtin(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    private fun jump(x: Int, y: Int, target: String) =
        Cell(x, y, button = Button(action = ButtonAction.GoToScreen(target)))

    private fun folder(x: Int, y: Int, target: String) =
        Cell(x, y, button = Button(action = ButtonAction.Folder(target)))

    @Test
    fun `a settings tile on the screen itself is enough`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(builtin(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(screen)), "a"))
    }

    @Test
    fun `without it it is a dead end`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(builtin(0, 0, Builtin.DIALER)))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(screen)), "a"))
    }

    @Test
    fun `a way through a jump tile counts`() {
        val a = Screen(id = "a", name = "A", cells = listOf(jump(0, 0, "b")))
        val b = Screen(id = "b", name = "B", cells = listOf(builtin(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, b)), "a"))
    }

    @Test
    fun `a way through a folder too`() {
        val a = Screen(id = "a", name = "A", cells = listOf(folder(0, 0, "f")))
        val f = Screen(id = "f", name = "F", kind = ScreenKind.FOLDER, cells = listOf(builtin(0, 0, Builtin.SETTINGS)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, f)), "a"))
    }

    @Test
    fun `a ring does not run forever`() {
        // two screens pointing at each other, and settings nowhere.
        val a = Screen(id = "a", name = "A", cells = listOf(jump(0, 0, "b")))
        val b = Screen(id = "b", name = "B", cells = listOf(jump(0, 0, "a")))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a, b)), "a"))
    }

    /**
     * it did **not** count before: HomeTiles stands in the app list, but starting it from there
     * led to the home screen, not into the settings. since the app list offers "HomeTiles
     * settings" at the end it is a way - the one the user got out of their dead end by.
     *
     * the coupling is deliberate and written down at both ends: whoever removes that row from
     * the app list has to take this case with it. `SettingsReachableFromDrawerTest` in `:app`
     * holds the other side.
     */
    @Test
    fun `the app list counts as a way`() {
        val a = Screen(id = "a", name = "A", cells = listOf(builtin(0, 0, Builtin.APP_LIST)))
        assertTrue(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a)), "a"))
    }

    @Test
    fun `a jump tile into nothing does not count`() {
        val a = Screen(id = "a", name = "A", cells = listOf(jump(0, 0, "gibtsnicht")))
        assertFalse(ScreenEdits.settingsReachable(LauncherConfig(screens = listOf(a)), "a"))
    }

    @Test
    fun `today's home screen is in order`() {
        val home = Screen(
            id = "home",
            name = "Start",
            cells = listOf(
                builtin(0, 0, Builtin.DIALER),
                builtin(1, 0, Builtin.MESSAGES),
                builtin(0, 1, Builtin.CONTACTS),
                builtin(1, 1, Builtin.CAMERA),
                builtin(0, 2, Builtin.APP_LIST),
                builtin(1, 2, Builtin.SETTINGS),
            ),
        )
        val second = Screen(id = "screen2", name = "Screen 2", cells = listOf(builtin(1, 2, Builtin.HOME_SCREEN)))
        val config = LauncherConfig(screens = listOf(home, second), homeScreenId = "home")
        assertTrue(ScreenEdits.settingsReachable(config, "home"))
        // and the screen one gets stuck in, precisely not.
        assertFalse(ScreenEdits.settingsReachable(config, "screen2"))
    }
}

/**
 * the way out: offer a settings tile instead of merely forbidding the change.
 *
 * forbidding would be patronising and would break the promise that everything is freely
 * assignable. offering a tile solves the same problem and leaves the decision with the user.
 */
class SettingsTileFallbackTest {

    private fun builtin(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    @Test
    fun `the tile lands on the first free slot`() {
        val screen = Screen(id = "a", name = "A", cells = listOf(builtin(0, 0, Builtin.DIALER)))
        val config = LauncherConfig(screens = listOf(screen))
        val after = ScreenEdits.withSettingsTile(config, "a")!!
        assertTrue(ScreenEdits.settingsReachable(after, "a"))
        val new = after.screens.first().cells.first { it.x == 1 && it.y == 0 }
        assertTrue((new.button.action as ButtonAction.Action).builtin == Builtin.SETTINGS)
    }

    @Test
    fun `on a full screen it does not work`() {
        // then the change must not happen either - otherwise the way back is gone.
        val full = Screen(
            id = "a",
            name = "A",
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> builtin(x, y, Builtin.DIALER) } },
        )
        assertNull(ScreenEdits.withSettingsTile(LauncherConfig(screens = listOf(full)), "a"))
    }
}
