package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** what "occupied" means - the cell geometry later carries joining, splitting and stretching. */
class ScreenTest {

    private fun cell(x: Int, y: Int, w: Int = 1, h: Int = 1) =
        Cell(x, y, w, h, Button(ButtonAction.Action(Builtin.DIALER)))

    @Test
    fun `a 1x1 cell covers exactly its own spot`() {
        val c = cell(1, 2)
        assertTrue(c.covers(1, 2))
        assertTrue(!c.covers(0, 2))
        assertTrue(!c.covers(1, 3))
    }

    @Test
    fun `a spanned cell covers every spot under it`() {
        val c = cell(0, 0, w = 2, h = 2)
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1).forEach { (x, y) ->
            assertTrue("($x,$y) should be occupied", c.covers(x, y))
        }
        assertTrue(!c.covers(2, 0))
        assertTrue(!c.covers(0, 2))
    }

    @Test
    fun `cellAt finds beyond the top left corner too`() {
        val screen = Screen(id = "s", name = "Test", cols = 3, rows = 3, cells = listOf(cell(1, 1, 2, 2)))
        assertNull(screen.cellAt(0, 0))
        assertEquals(1, screen.cellAt(2, 2)?.x)
        assertEquals(1, screen.cellAt(1, 2)?.x)
    }

    @Test
    fun `free spots are exactly the uncovered ones`() {
        val screen = Screen(id = "s", name = "Test", cols = 3, rows = 2, cells = listOf(cell(0, 0, 2, 2)))
        val free = screen.freeSlots()
        assertEquals(2, free.size)
        assertTrue(free.containsAll(listOf(2 to 0, 2 to 1)))
    }

    @Test
    fun `an empty screen is completely free`() {
        val screen = Screen(id = "s", name = "Empty", cols = 2, rows = 3)
        assertEquals(6, screen.freeSlots().size)
    }

    @Test
    fun `the starting layout fills the default grid without a gap`() {
        val screen = Defaults.mainScreen()
        assertEquals(2, screen.cols)
        assertEquals(3, screen.rows)
        assertEquals(6, screen.cells.size)
        assertTrue("the home screen must have no gap", screen.freeSlots().isEmpty())
    }

    @Test
    fun `no two cells overlap in the starting layout`() {
        val screen = Defaults.mainScreen()
        val occupied = mutableSetOf<Pair<Int, Int>>()
        screen.cells.forEach { c ->
            for (x in c.x until c.x + c.w) {
                for (y in c.y until c.y + c.h) {
                    assertTrue("($x,$y) is occupied twice", occupied.add(x to y))
                }
            }
        }
    }

    @Test
    fun `every layout preset is within the allowed range`() {
        Defaults.layouts.forEach { (cols, rows) ->
            assertTrue("$cols columns outside 1..6", cols in 1..6)
            assertTrue("$rows rows outside 1..8", rows in 1..8)
        }
    }
}
