package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a tile is a filled cell - an empty one is a free spot.
 *
 * loading a backup said "3 screens with 14 tiles", resetting said "2 screens with 14 tiles
 * and 1 folder" - the same setup, two numbers, because three places counted `cells.size` and
 * one `cells.count { ... != None }`. on a half filled grid the backup would have promised
 * more tiles than it holds.
 */
class TileCountTest {

    private fun cell(x: Int, action: ButtonAction) =
        Cell(x = x, y = 0, button = Button(action = action))

    @Test
    fun `empty cells do not count`() {
        val screen = Screen(
            id = "s",
            name = "Test",
            cells = listOf(
                cell(0, ButtonAction.GoToScreen("home")),
                cell(1, ButtonAction.None),
                cell(2, ButtonAction.App("org.example", "Main")),
            ),
        )
        assertEquals(3, screen.cells.size)
        assertEquals(2, screen.tileCount)
    }

    @Test
    fun `an empty screen has no tiles`() {
        assertEquals(0, Screen(id = "empty", name = "Empty").tileCount)
    }

    /**
     * an empty cell with its own label stays an empty cell: the home screen does show its
     * text there instead of the invitation to fill it, but tapping it does nothing. counting
     * it would promise a tile that does nothing.
     */
    @Test
    fun `an empty cell with a label is still no tile`() {
        val screen = Screen(
            id = "s",
            name = "Test",
            cells = listOf(Cell(x = 0, y = 0, button = Button(label = "Later"))),
        )
        assertEquals(0, screen.tileCount)
    }
}
