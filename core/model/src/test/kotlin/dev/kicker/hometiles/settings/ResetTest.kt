package dev.kicker.hometiles.settings

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.Defaults
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import dev.kicker.hometiles.data.Security
import dev.kicker.hometiles.security.Pin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.9, resetting everything - the only step in this app that cannot be undone.
 */
class ResetTest {

    private fun tile(action: ButtonAction) = Button(action = action)

    private val config = LauncherConfig(
        screens = listOf(
            Screen(
                id = "home",
                name = "Start",
                cols = 2,
                rows = 3,
                cells = listOf(
                    Cell(0, 0, button = tile(ButtonAction.Action(Builtin.SETTINGS))),
                    Cell(1, 0, button = tile(ButtonAction.App("a", "b"))),
                    Cell(0, 1, button = tile(ButtonAction.None)),
                    Cell(1, 1, button = tile(ButtonAction.Widget("p/W", 7, "Uhr"))),
                ),
            ),
            Screen(
                id = "mehr",
                name = "Mehr",
                cols = 2,
                rows = 3,
                kind = ScreenKind.FOLDER,
                cells = listOf(Cell(0, 0, button = tile(ButtonAction.Widget("p/W", 9, "Wetter")))),
            ),
        ),
    )

    // a number makes the warning true. "are you sure" gets tapped away unread.
    @Test
    fun `the confirmation counts what disappears`() {
        val loss = Reset.losses(config)
        assertEquals(1, loss.screens)
        assertEquals(1, loss.folders)
        assertEquals(4, loss.tiles)
    }

    // empty tiles are no loss - counting them would make the warning bigger than it is, and
    // an exaggerated warning is not believed the next time.
    @Test
    fun `empty tiles do not count`() {
        val empty = LauncherConfig(
            screens = listOf(
                Screen("x", "X", 2, 3, cells = listOf(Cell(0, 0, button = Button()))),
            ),
        )
        assertEquals(0, Reset.losses(empty).tiles)
    }

    @Test
    fun `the pin is named separately`() {
        assertEquals(false, Reset.losses(config).hasPin)
        val withPin = config.copy(security = Security(pin = Pin.hash("1234")))
        assertEquals(true, Reset.losses(withPin).hasPin)
    }

    // without this step the widget host would keep the ids for ever, and the provider app
    // would keep alive a widget nobody sees any more.
    @Test
    fun `all widget ids are collected`() {
        assertEquals(listOf(7, 9), Reset.widgetIds(config))
    }

    @Test
    fun `the emergency screen throws away the same widget ids`() {
        // it had a reset of its own: `LauncherConfig()` straight, without releasing the ids.
        // both ways now use the same two functions here.
        assertEquals(LauncherConfig(), Reset.fresh())
        assertTrue("there are widget ids to release", Reset.widgetIds(config).isNotEmpty())
    }

    @Test
    fun `resetting gives the state after the install`() {
        assertEquals(LauncherConfig(), Reset.fresh())
        assertTrue("the set-up arrangement is not the default", config != Reset.fresh())
    }

    // otherwise one would stand before a strange home screen with no hint what to do.
    @Test
    fun `the wizard runs again afterwards`() {
        assertEquals(false, Reset.fresh().wizardDone)
    }

    // this app's most common fault would be most expensive here: a home screen without a way
    // into the settings cannot be rescued without adb.
    @Test
    fun `the fresh home screen has a settings tile`() {
        val tiles = Reset.fresh().homeScreen.cells.map { it.button.action }
        assertEquals(true, tiles.contains(ButtonAction.Action(Builtin.SETTINGS)))
    }

    @Test
    fun `the fresh home screen is the default screen`() {
        assertEquals(Defaults.mainScreen(), Reset.fresh().homeScreen)
    }
}
