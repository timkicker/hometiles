package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * filling a tile and emptying it again.
 *
 * these two calculations stood in `ConfigStore` until 3.9.2026 - the only module without a
 * single test. they are the ones that **overwrite** or **delete** a tile of the user's;
 * leaving them unchecked was the worst place for it.
 */
class CellButtonTest {

    private fun button(name: String) =
        Button(action = ButtonAction.GoToScreen(name), label = name)

    private fun screen(vararg cells: Cell) =
        Screen(id = "home", name = "Start", cols = 2, rows = 4, cells = cells.toList())

    @Test
    fun `on a free spot a new cell comes about`() {
        val before = screen()
        val after = CellLayout.withButton(before, 1, 2, button("a"))
        assertEquals(1, after.cells.size)
        assertEquals(1, after.cells[0].x)
        assertEquals(2, after.cells[0].y)
        assertEquals(1, after.cells[0].w)
        assertEquals(1, after.cells[0].h)
        assertEquals("a", after.cells[0].button.label)
    }

    @Test
    fun `a filled cell is overwritten, not doubled`() {
        val before = screen(Cell(x = 0, y = 0, button = button("old")))
        val after = CellLayout.withButton(before, 0, 0, button("new"))
        assertEquals(1, after.cells.size)
        assertEquals("new", after.cells[0].button.label)
    }

    /**
     * the case that would have slipped through untested: a wide cell is hit at **every** spot
     * it covers, not only at its corner. otherwise a tap on the right half of a double tile
     * laid a second cell over it.
     */
    @Test
    fun `a wide cell is hit at its right edge too`() {
        val wide = Cell(x = 0, y = 0, w = 2, h = 1, button = button("wide"))
        val after = CellLayout.withButton(screen(wide), 1, 0, button("new"))
        assertEquals(1, after.cells.size)
        assertEquals(2, after.cells[0].w)
        assertEquals("new", after.cells[0].button.label)
    }

    @Test
    fun `emptying leaves no cell without an action behind`() {
        val before = screen(Cell(x = 0, y = 0, button = button("a")))
        val after = CellLayout.withoutButton(before, 0, 0)
        assertEquals(emptyList<Cell>(), after.cells)
        assertNull(after.cellAt(0, 0))
    }

    @Test
    fun `emptying a free spot changes nothing`() {
        val before = screen(Cell(x = 0, y = 0, button = button("a")))
        assertEquals(before, CellLayout.withoutButton(before, 1, 3))
    }

    @Test
    fun `emptying hits the wide cell at its right edge too`() {
        val wide = Cell(x = 0, y = 0, w = 2, h = 1, button = button("wide"))
        assertEquals(emptyList<Cell>(), CellLayout.withoutButton(screen(wide), 1, 0).cells)
    }
}
