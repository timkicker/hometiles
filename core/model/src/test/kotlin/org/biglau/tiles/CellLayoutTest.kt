package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellLayoutTest {

    private fun cell(x: Int, y: Int, w: Int = 1, h: Int = 1, name: Builtin = Builtin.DIALER) =
        Cell(x, y, w, h, Button(ButtonAction.Action(name)))

    private fun screen(vararg cells: Cell, cols: Int = 2, rows: Int = 3) =
        Screen(id = "s", name = "Test", cols = cols, rows = rows, cells = cells.toList())

    @Test
    fun `a cell grows into free space`() {
        val target = cell(0, 0)
        val result = CellLayout.stretch(screen(target), target, Direction.RIGHT)
        assertEquals(1, result.cells.size)
        assertEquals(2, result.cells.first().w)
    }

    @Test
    fun `after growing the cell covers both spots`() {
        val target = cell(0, 0)
        val result = CellLayout.stretch(screen(target), target, Direction.RIGHT)
        val grown = result.cells.first()
        assertTrue(grown.covers(0, 0))
        assertTrue(grown.covers(1, 0))
        assertTrue(result.freeSlots().none { it == 1 to 0 })
    }

    @Test
    fun `a cell does not grow over the edge`() {
        val target = cell(1, 0)
        assertTrue(!CellLayout.canStretch(screen(target), target, Direction.RIGHT))
        assertTrue(!CellLayout.canStretch(screen(target), target, Direction.UP))
    }

    @Test
    fun `a cell does not overwrite a filled neighbour`() {
        // that would be data loss at the least expected place.
        val target = cell(0, 0)
        val neighbour = cell(1, 0, name = Builtin.CAMERA)
        val board = screen(target, neighbour)
        assertTrue(!CellLayout.canStretch(board, target, Direction.RIGHT))
        assertEquals(board, CellLayout.stretch(board, target, Direction.RIGHT))
    }

    @Test
    fun `growing upwards moves the corner too`() {
        val target = cell(0, 1)
        val grown = CellLayout.stretch(screen(target), target, Direction.UP).cells.first()
        assertEquals(0, grown.y)
        assertEquals(2, grown.h)
    }

    @Test
    fun `growing leftwards moves the corner too`() {
        val target = cell(1, 0)
        val grown = CellLayout.stretch(screen(target), target, Direction.LEFT).cells.first()
        assertEquals(0, grown.x)
        assertEquals(2, grown.w)
    }

    @Test
    fun `a wide cell cannot grow into a partly filled row`() {
        val target = cell(0, 0, w = 2)
        val blocker = cell(1, 1, name = Builtin.CAMERA)
        assertTrue(!CellLayout.canStretch(screen(target, blocker), target, Direction.DOWN))
    }

    @Test
    fun `a wide cell grows into a wholly free row`() {
        val target = cell(0, 0, w = 2)
        assertTrue(CellLayout.canStretch(screen(target), target, Direction.DOWN))
    }

    @Test
    fun `shrinking frees spots again`() {
        val target = cell(0, 0, w = 2)
        val result = CellLayout.shrink(screen(target), target, Direction.RIGHT)
        assertEquals(1, result.cells.first().w)
        assertTrue(result.freeSlots().contains(1 to 0))
    }

    @Test
    fun `shrinking from the left moves the corner right`() {
        val target = cell(0, 0, w = 2)
        val shrunk = CellLayout.shrink(screen(target), target, Direction.LEFT).cells.first()
        assertEquals(1, shrunk.x)
        assertEquals(1, shrunk.w)
        assertTrue(CellLayout.shrink(screen(target), target, Direction.LEFT).freeSlots().contains(0 to 0))
    }

    @Test
    fun `a single cell cannot be shrunk further`() {
        val target = cell(0, 0)
        Direction.entries.forEach { assertTrue(!CellLayout.canShrink(target, it)) }
        assertEquals(screen(target), CellLayout.shrink(screen(target), target, Direction.RIGHT))
    }

    @Test
    fun `growing and shrinking cancel out`() {
        val target = cell(0, 0)
        val board = screen(target)
        Direction.entries.filter { CellLayout.canStretch(board, target, it) }.forEach { direction ->
            val grown = CellLayout.stretch(board, target, direction)
            val back = CellLayout.shrink(grown, grown.cells.first(), direction)
            assertEquals("direction $direction", board, back)
        }
    }

    @Test
    fun `the possible directions match the single check`() {
        val target = cell(0, 1)
        val board = screen(target, cell(1, 1, name = Builtin.CAMERA))
        assertEquals(listOf(Direction.UP, Direction.DOWN), CellLayout.stretchable(board, target))
        assertTrue(CellLayout.shrinkable(target).isEmpty())
    }

    @Test
    fun `after emptying the neighbour can grow into the spot`() {
        // an emptied cell stayed as a cell without an action and blocked the growing
        // without anyone seeing it.
        val neighbour = cell(0, 0)
        val board = screen(neighbour, cell(0, 1, name = Builtin.CAMERA))
        assertTrue(!CellLayout.canStretch(board, neighbour, Direction.DOWN))

        val emptied = board.copy(cells = board.cells.filter { it.y == 0 })
        assertTrue(CellLayout.canStretch(emptied, neighbour, Direction.DOWN))
    }

    @Test
    fun `a cell grows to the wanted size`() {
        val target = cell(0, 0)
        val grown = CellLayout.growTo(screen(target), target, targetWidth = 2, targetHeight = 2)
        assertTrue(grown != null)
        val result = grown!!.cells.first()
        assertTrue(result.w >= 2 && result.h >= 2)
    }

    @Test
    fun `growing leaves the neighbours untouched`() {
        val target = cell(0, 0)
        val neighbour = cell(1, 0, name = Builtin.CAMERA)
        val board = screen(target, neighbour)
        // to the right does not work, downwards does.
        val grown = CellLayout.growTo(board, target, targetWidth = 1, targetHeight = 2)
        assertTrue(grown != null)
        assertTrue(grown!!.cells.any { it.button == neighbour.button && it.w == 1 && it.h == 1 })
    }

    @Test
    fun `without enough room nothing grows at all`() {
        // a half grown cell would be worse than a refusal.
        val target = cell(0, 0)
        val board = screen(target, cell(1, 0, name = Builtin.CAMERA), cell(0, 1, name = Builtin.CLOCK))
        assertNull(CellLayout.growTo(board, target, targetWidth = 2, targetHeight = 2))
    }

    @Test
    fun `a cell that already fits stays unchanged`() {
        val target = cell(0, 0, w = 2, h = 2)
        val board = screen(target, cols = 2, rows = 3)
        assertEquals(board, CellLayout.growTo(board, target, 2, 2))
        assertEquals(board, CellLayout.growTo(board, target, 1, 1))
    }

    @Test
    fun `growing over the edge is refused`() {
        val target = cell(0, 0)
        assertNull(CellLayout.growTo(screen(target, cols = 2, rows = 3), target, 3, 1))
    }

    @Test
    fun `a smaller grid throws away cells that fell out`() {
        val board = Screen(id = "s", name = "T", cols = 2, rows = 2, cells = listOf(cell(0, 0), cell(1, 2)))
        val fitted = CellLayout.fitToGrid(board)
        assertEquals(1, fitted.cells.size)
        assertEquals(0, fitted.cells.first().y)
    }

    @Test
    fun `a smaller grid trims cells that stick out`() {
        val board = Screen(id = "s", name = "T", cols = 2, rows = 2, cells = listOf(cell(0, 0, w = 2, h = 3)))
        val fitted = CellLayout.fitToGrid(board)
        assertEquals(2, fitted.cells.first().h)
        assertEquals(2, fitted.cells.first().w)
    }

    @Test
    fun `a fitting grid stays unchanged`() {
        val board = screen(cell(0, 0, w = 2), cell(0, 1))
        assertEquals(board, CellLayout.fitToGrid(board))
    }
}
