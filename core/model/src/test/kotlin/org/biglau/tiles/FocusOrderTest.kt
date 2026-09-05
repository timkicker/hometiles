package org.biglau.tiles

import org.biglau.data.Cell
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * where the focus goes is decided by the grid, not by the order in the file.
 *
 * `PLAN.md` 10.3.2. going by the order in `config.json` would be easy and wrong: the focus
 * would jump about, differently for every user.
 *
 * the case that decides it is the **remembered column**. a tile may be several cells wide;
 * going down from a narrow tile onto a wide one and back up should return to the narrow one,
 * not to its left neighbour. without the memory the focus creeps left on every up and down.
 */
class FocusOrderTest {

    private fun cell(x: Int, y: Int, w: Int = 1, h: Int = 1) = Cell(x = x, y = y, w = w, h = h)

    private val grid = listOf(
        cell(0, 0), cell(1, 0),
        cell(0, 1), cell(1, 1),
        cell(0, 2), cell(1, 2),
    )

    @Test
    fun `right gives the right neighbour`() {
        assertEquals(cell(1, 1), FocusOrder.neighbour(grid, cell(0, 1), PadDirection.RIGHT))
    }

    @Test
    fun `left gives the left neighbour`() {
        assertEquals(cell(0, 1), FocusOrder.neighbour(grid, cell(1, 1), PadDirection.LEFT))
    }

    @Test
    fun `down gives the cell below`() {
        assertEquals(cell(1, 2), FocusOrder.neighbour(grid, cell(1, 1), PadDirection.DOWN))
    }

    @Test
    fun `up gives the cell above`() {
        assertEquals(cell(1, 0), FocusOrder.neighbour(grid, cell(1, 1), PadDirection.UP))
    }

    /** the screen change gets its own keys, or paging would slide sideways off the screen. */
    @Test
    fun `nothing happens at the edge`() {
        assertNull("right of the right edge", FocusOrder.neighbour(grid, cell(1, 1), PadDirection.RIGHT))
        assertNull("left of the left edge", FocusOrder.neighbour(grid, cell(0, 1), PadDirection.LEFT))
        assertNull("above the first row", FocusOrder.neighbour(grid, cell(0, 0), PadDirection.UP))
        assertNull("below the last row", FocusOrder.neighbour(grid, cell(0, 2), PadDirection.DOWN))
    }

    @Test
    fun `the order in the file changes nothing`() {
        val shuffled = grid.reversed()
        assertEquals(
            FocusOrder.neighbour(grid, cell(0, 1), PadDirection.RIGHT),
            FocusOrder.neighbour(shuffled, cell(0, 1), PadDirection.RIGHT),
        )
    }

    /** one wide tile at the bottom, two narrow ones above. */
    private val withWide = listOf(
        cell(0, 0), cell(1, 0),
        cell(0, 1, w = 2),
        cell(0, 2), cell(1, 2),
    )

    @Test
    fun `down from a narrow tile hits the wide one`() {
        assertEquals(cell(0, 1, w = 2), FocusOrder.neighbour(withWide, cell(1, 0), PadDirection.DOWN))
    }

    @Test
    fun `with the remembered column it goes back where it came from`() {
        val wide = cell(0, 1, w = 2)
        assertEquals(
            "without the memory the focus creeps left",
            cell(1, 0),
            FocusOrder.neighbour(withWide, wide, PadDirection.UP, rememberedColumn = 1),
        )
        assertEquals(
            "with no memory the tile's own left edge applies",
            cell(0, 0),
            FocusOrder.neighbour(withWide, wide, PadDirection.UP),
        )
    }

    @Test
    fun `the remembered column counts downwards too`() {
        val wide = cell(0, 1, w = 2)
        assertEquals(cell(1, 2), FocusOrder.neighbour(withWide, wide, PadDirection.DOWN, rememberedColumn = 1))
        assertEquals(cell(0, 2), FocusOrder.neighbour(withWide, wide, PadDirection.DOWN, rememberedColumn = 0))
    }

    @Test
    fun `a column missing below falls back to the nearest`() {
        val gap = listOf(cell(0, 0), cell(1, 0), cell(2, 0), cell(0, 1))
        assertEquals(cell(0, 1), FocusOrder.neighbour(gap, cell(2, 0), PadDirection.DOWN, rememberedColumn = 2))
    }

    @Test
    fun `an empty grid has no neighbour`() {
        assertNull(FocusOrder.neighbour(emptyList(), cell(0, 0), PadDirection.RIGHT))
    }

    /**
     * an empty slot is clickable, so it has to be reachable.
     *
     * with only `screen.cells` the focus stayed on the one filled tile in a folder while
     * seven empty slots beside it, each opening the editor, were unreachable by key. that is
     * the gap `PLAN.md` 10.3.6 names: not "big enough" but **reachable**.
     */
    @Test
    fun `empty slots belong in the order`() {
        val screen = Screen(id = "s", name = "S", cols = 2, rows = 2, cells = listOf(cell(0, 0)))
        val targets = FocusOrder.targets(screen)
        assertEquals("four slots, one filled: all four must be reachable", 4, targets.size)
        assertEquals(
            "right of the filled slot is the empty one beside it",
            cell(1, 0),
            FocusOrder.neighbour(targets, cell(0, 0), PadDirection.RIGHT),
        )
    }

    /**
     * the digits count as one reads: one is top left, then right, then the next row.
     *
     * **slots** are counted, not filled tiles. counting only the filled ones would shift
     * every number as soon as a tile appears or vanishes, and the number someone memorised
     * would be wrong.
     */
    @Test
    fun `a digit counts as one reads`() {
        assertEquals(cell(0, 0), FocusOrder.numbered(grid, 1))
        assertEquals(cell(1, 0), FocusOrder.numbered(grid, 2))
        assertEquals(cell(0, 1), FocusOrder.numbered(grid, 3))
        assertEquals(cell(1, 2), FocusOrder.numbered(grid, 6))
    }

    @Test
    fun `the order in the file does not change the number`() {
        assertEquals(FocusOrder.numbered(grid, 3), FocusOrder.numbered(grid.reversed(), 3))
    }

    @Test
    fun `a wide tile counts once`() {
        assertEquals(cell(0, 1, w = 2), FocusOrder.numbered(withWide, 3))
        assertEquals(cell(0, 2), FocusOrder.numbered(withWide, 4))
    }

    @Test
    fun `there are only nine digits`() {
        val big = (0 until 4).flatMap { y -> (0 until 3).map { x -> cell(x, y) } }
        assertEquals(cell(2, 2), FocusOrder.numbered(big, 9))
        assertNull("the tenth tile has no key", FocusOrder.numbered(big, 10))
        assertNull("zero is not a tile number", FocusOrder.numbered(big, 0))
    }

    @Test
    fun `a digit without a slot hits nothing`() {
        assertNull(FocusOrder.numbered(listOf(cell(0, 0)), 2))
    }

    @Test
    fun `the first focus sits top left`() {
        assertEquals(cell(0, 0), FocusOrder.first(grid.reversed()))
        assertNull(FocusOrder.first(emptyList()))
    }
}
