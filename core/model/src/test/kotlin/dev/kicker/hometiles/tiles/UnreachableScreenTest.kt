package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Behaviour
import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * screens no way leads to - and warnings that are not true.
 *
 * the case comes from the user's device, 3.9.2026: `Screen 2` was set up, swiping between
 * screens off, no tile jumped over. seven free fields, unreachable.
 *
 * the other direction matters as much and was wrong: a "next screen" tile pages through the
 * same row as swiping, **even when swiping is off**. whoever has one gets everywhere and got
 * the warning anyway - and a warning that is not true is not believed where it is true
 * either.
 */
class UnreachableScreenTest {

    private fun cell(x: Int, y: Int, action: ButtonAction) =
        Cell(x = x, y = y, button = Button(action = action))

    private fun config(tileOnHome: ButtonAction? = null, swipe: Boolean = false) =
        LauncherConfig(
            screens = listOf(
                Screen(
                    id = "home",
                    name = "Start",
                    cells = listOfNotNull(tileOnHome?.let { cell(0, 0, it) }),
                ),
                Screen(id = "two", name = "Screen 2"),
            ),
            homeScreenId = "home",
            behaviour = Behaviour(swipeBetweenScreens = swipe),
        )

    @Test
    fun `without swiping and without a jump tile the second screen is unreachable`() {
        assertEquals(listOf("two"), ScreenEdits.unreachable(config()).map { it.id })
    }

    @Test
    fun `with swiping it is reachable`() {
        assertTrue(ScreenEdits.unreachable(config(swipe = true)).isEmpty())
    }

    @Test
    fun `one jump tile is enough`() {
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.GoToScreen("two"))).isEmpty(),
        )
    }

    @Test
    fun `a paging tile is enough as well`() {
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.Action(Builtin.NEXT_SCREEN))).isEmpty(),
        )
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.Action(Builtin.PREV_SCREEN))).isEmpty(),
        )
    }

    @Test
    fun `a tile back to the home screen does not make the screen reachable`() {
        val asFound = LauncherConfig(
            screens = listOf(
                Screen(id = "home", name = "Start"),
                Screen(
                    id = "two",
                    name = "Screen 2",
                    cells = listOf(cell(1, 2, ButtonAction.Action(Builtin.HOME_SCREEN))),
                ),
            ),
            homeScreenId = "home",
            behaviour = Behaviour(swipeBetweenScreens = false),
        )
        assertEquals(listOf("two"), ScreenEdits.unreachable(asFound).map { it.id })
    }

    @Test
    fun `the home screen and folders never count`() {
        val config = LauncherConfig(
            screens = listOf(
                Screen(id = "home", name = "Start"),
                Screen(id = "folder", name = "More", kind = ScreenKind.FOLDER),
            ),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }
}
