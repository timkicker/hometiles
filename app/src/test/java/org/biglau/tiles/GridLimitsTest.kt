package org.biglau.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1: the grid is free per screen, 1-6 columns by 1-8 rows.
 *
 * what was built was a fixed list of six presets ending at three columns, with the argument
 * that more turns into a postage stamp on 349 dp. that holds for this device and is still
 * the wrong answer: it makes the limit a number in the source instead of a property of the
 * screen.
 */
class GridLimitsTest {

    // Jelly 2: 349 dp wide, 2 percent margin on each side, 4 dp gutter.
    private val jellyWidth = 335f
    private val jellyHeight = 551f

    @Test
    fun `on three inches four columns are the most`() {
        assertEquals(4, GridLimits.maxColumns(jellyWidth, gutterDp = 4))
        assertEquals(8, GridLimits.maxRows(jellyHeight, gutterDp = 4))
    }

    @Test
    fun `a wider screen gets more`() {
        assertEquals(6, GridLimits.maxColumns(600f, gutterDp = 4))
    }

    // the plan's upper limit holds anyway: more than six columns gives a tile with a word on
    // it on no phone.
    @Test
    fun `above six it never goes`() {
        assertEquals(GridLimits.MAX_COLUMNS, GridLimits.maxColumns(4000f, gutterDp = 0))
        assertEquals(GridLimits.MAX_ROWS, GridLimits.maxRows(4000f, gutterDp = 0))
    }

    // even on a tiny screen one column is left - a grid with zero columns would be an empty
    // home screen.
    @Test
    fun `at least one column always stays`() {
        assertEquals(1, GridLimits.maxColumns(10f, gutterDp = 4))
        assertEquals(1, GridLimits.maxRows(10f, gutterDp = 4))
    }

    // a larger gutter costs room and possibly a column with it.
    @Test
    fun `the gutter counts`() {
        assertTrue(GridLimits.maxColumns(300f, gutterDp = 0) >= GridLimits.maxColumns(300f, gutterDp = 12))
    }

    @Test
    fun `every offered column keeps the minimum`() {
        for (n in GridLimits.columns(jellyWidth, gutterDp = 4)) {
            val cell = (jellyWidth - (n - 1) * 4) / n
            assertTrue("$n columns give $cell dp", cell >= GridLimits.MIN_CELL_WIDTH_DP)
        }
    }

    // counter-check: the next larger number really does fail.
    @Test
    fun `one column more would be too narrow`() {
        val n = GridLimits.maxColumns(jellyWidth, gutterDp = 4) + 1
        val cell = (jellyWidth - (n - 1) * 4) / n
        assertTrue("$n columns give $cell dp", cell < GridLimits.MIN_CELL_WIDTH_DP)
    }

    // the default has to stay under what the device carries - otherwise the app would stand
    // there with a grid it no longer offers itself.
    @Test
    fun `the default fits into what is offered`() {
        val default = org.biglau.data.Defaults.mainScreen()
        assertTrue(default.cols <= GridLimits.maxColumns(jellyWidth, gutterDp = 4))
        assertTrue(default.rows <= GridLimits.maxRows(jellyHeight, gutterDp = 4))
    }
}
