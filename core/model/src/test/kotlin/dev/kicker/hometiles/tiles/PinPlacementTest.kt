package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinPlacementTest {

    private val pwa = ButtonAction.Shortcut("org.mozilla.firefox", "pwa-1", "Weather")
    private val app = Button(action = ButtonAction.App("a.b", "a.b.Main"))

    private fun home(vararg cells: Cell, cols: Int = 2, rows: Int = 2) = LauncherConfig(
        screens = listOf(Screen("home", "Home", cols = cols, rows = rows, cells = cells.toList())),
        homeScreenId = "home",
    )

    @Test
    fun `the first free spot is read like text`() {
        val config = home(Cell(0, 0, button = app))
        assertEquals(1 to 0, PinPlacement.spotFor(config))
    }

    @Test
    fun `a tile with only a label is not free`() {
        // a placeholder someone named on purpose is not ours to overwrite.
        val config = home(Cell(0, 0, button = Button(label = "later")))
        assertEquals(1 to 0, PinPlacement.spotFor(config))
    }

    @Test
    fun `an empty cell is used and keeps its size`() {
        val config = home(Cell(0, 0, w = 2, button = Button()), Cell(0, 1, button = app))
        val placed = PinPlacement.place(config, pwa)!!.homeScreen.cellAt(0, 0)!!
        assertEquals(pwa, placed.button.action)
        assertEquals(2, placed.w)
    }

    @Test
    fun `a full home screen gives no spot and no change`() {
        val config = home(
            Cell(0, 0, button = app), Cell(1, 0, button = app),
            Cell(0, 1, button = app), Cell(1, 1, button = app),
        )
        assertNull(PinPlacement.spotFor(config))
        assertNull(PinPlacement.place(config, pwa))
    }

    @Test
    fun `the shortcut lands on the home screen and nowhere else`() {
        val other = Screen("two", "Two", cols = 2, rows = 2)
        val config = home(Cell(0, 0, button = app)).let { it.copy(screens = listOf(other) + it.screens) }
        val placed = PinPlacement.place(config, pwa)!!
        assertEquals(pwa, placed.homeScreen.cellAt(1, 0)!!.button.action)
        assertEquals(other, placed.screenById("two"))
    }
}
