package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the jump tile that turns the warning into an action.
 *
 * the warning that no tile leads to a screen only said what one would have to do. since
 * 3.9.2026 the page offers it, and this function stands behind that.
 */
class JumpTileTest {

    private fun home(vararg filled: Pair<Int, Int>) = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 2,
        cells = filled.map { (x, y) ->
            Cell(x = x, y = y, button = Button(action = ButtonAction.Action(org.biglau.data.Builtin.CAMERA)))
        },
    )

    private fun config(vararg filled: Pair<Int, Int>) = LauncherConfig(
        screens = listOf(home(*filled), Screen(id = "two", name = "Screen 2")),
        homeScreenId = "home",
    )

    @Test
    fun `the tile lands on the first free spot of the home screen`() {
        val withTile = ScreenEdits.withJumpTile(config(0 to 0), "two")
        assertTrue(withTile != null)
        val tile = withTile!!.screens.first { it.id == "home" }.cells
            .first { it.button.action is ButtonAction.GoToScreen }
        assertEquals(ButtonAction.GoToScreen("two"), tile.button.action)
    }

    @Test
    fun `afterwards the screen is reachable`() {
        val before = config(0 to 0)
        assertEquals(listOf("two"), ScreenEdits.unreachable(before).map { it.id })
        val withTile = ScreenEdits.withJumpTile(before, "two")!!
        assertTrue("after the tile nothing may be unreachable", ScreenEdits.unreachable(withTile).isEmpty())
    }

    @Test
    fun `on a full home screen it does not work`() {
        val full = config(0 to 0, 1 to 0, 0 to 1, 1 to 1)
        assertNull("no free spot - then the interface says so", ScreenEdits.withJumpTile(full, "two"))
    }

    @Test
    fun `a target that does not exist creates no tile`() {
        assertNull(ScreenEdits.withJumpTile(config(0 to 0), "doesnotexist"))
    }
}
