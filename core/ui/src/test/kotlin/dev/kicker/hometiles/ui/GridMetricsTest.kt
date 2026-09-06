package dev.kicker.hometiles.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** measured against the jelly 2's surface: 349 x 581 dp (PLAN.md 3.2). */
class GridMetricsTest {

    // cell size of the default grid, PLAN.md 3.2
    private val CELL_W = 165.6f
    private val CELL_H = 186.4f

    private val width = 349f
    private val height = 581f
    private val gutter = 4f

    @Test
    fun `the default grid gives the cell written in the plan`() {
        val m = gridMetrics(width, height, cols = 2, rows = 3, gutter = gutter, borderPercent = 2)
        assertEquals(165.6f, m.cellWidth, 0.5f)
        assertEquals(186.4f, m.cellHeight, 0.5f)
    }

    @Test
    fun `cells and gaps fill the surface exactly`() {
        listOf(1 to 1, 2 to 2, 2 to 3, 3 to 4, 3 to 5, 6 to 8).forEach { (cols, rows) ->
            val m = gridMetrics(width, height, cols, rows, gutter, borderPercent = 2)
            val usedWidth = m.cellWidth * cols + gutter * (cols - 1) + m.border * 2
            val usedHeight = m.cellHeight * rows + gutter * (rows - 1) + m.border * 2
            assertEquals("width at ${cols}x$rows", width, usedWidth, 0.01f)
            assertEquals("height at ${cols}x$rows", height, usedHeight, 0.01f)
        }
    }

    @Test
    fun `a spanned cell covers exactly the places under it`() {
        val m = gridMetrics(width, height, cols = 3, rows = 4, gutter = gutter, borderPercent = 2)
        assertEquals(m.cellWidth * 2 + gutter, m.spanWidth(2, gutter), 0.001f)
        // and its right edge meets that of the single cell beside it.
        val spannedRight = m.offsetX(0, gutter) + m.spanWidth(2, gutter)
        val singleRight = m.offsetX(1, gutter) + m.cellWidth
        assertEquals(singleRight, spannedRight, 0.001f)
    }

    @Test
    fun `the outer border is clamped to the allowed range`() {
        assertEquals(0f, gridMetrics(width, height, 2, 3, gutter, borderPercent = -5).border, 0.001f)
        val maxBorder = gridMetrics(width, height, 2, 3, gutter, borderPercent = 99).border
        assertEquals(width * 0.15f, maxBorder, 0.001f)
    }

    @Test
    fun `a grid without columns is a programming error`() {
        listOf(0 to 3, 2 to 0, -1 to 3).forEach { (cols, rows) ->
            try {
                gridMetrics(width, height, cols, rows, gutter, 2)
                throw AssertionError("${cols}x$rows should have been refused")
            } catch (expected: IllegalArgumentException) {
                // as it should be
            }
        }
    }

    @Test
    fun `the label stays between 14 and 40 sp`() {
        assertEquals(14f, labelSizeSp(165.6f, 40f, userScale = 1f), 0.01f)
        assertEquals(40f, labelSizeSp(600f, 600f, userScale = 1f), 0.01f)
        assertEquals(26.1f, labelSizeSp(165.6f, 186.4f, userScale = 1f), 0.2f)
    }

    @Test
    fun `a tall narrow cell gets no type that does not fit its width`() {
        // out of the height alone a 166 x 377 dp tile gave 40 sp and "Contacts" broke
        // mid-word.
        val tall = labelSizeSp(cellWidthDp = 165.6f, cellHeightDp = 376.8f, userScale = 1f)
        val square = labelSizeSp(cellWidthDp = 165.6f, cellHeightDp = 186.4f, userScale = 1f)
        assertTrue("a tall cell must not set larger than the width allows", tall <= 165.6f * 0.16f)
        assertTrue("but not smaller than the square one either", tall >= square)
    }

    @Test
    fun `a wide flat cell goes by the height`() {
        assertEquals(14f, labelSizeSp(cellWidthDp = 340f, cellHeightDp = 90f, userScale = 1f), 0.5f)
    }

    @Test
    fun `the user factor applies only after the clamping`() {
        // otherwise a factor could undercut the lower bound and make text unreadable.
        assertEquals(28f, labelSizeSp(165.6f, 40f, userScale = 2f), 0.01f)
        assertEquals(20f, labelSizeSp(600f, 600f, userScale = 0.5f), 0.01f)
    }

    @Test
    fun `the label zone on the default cell stays as it was`() {
        val labelSp = labelSizeSp(165.6f, 186.4f, 1f)
        assertEquals(52.2f, labelZoneDp(186.4f, labelSp), 0.3f)
    }

    @Test
    fun `no empty zone gapes under a tall tile`() {
        val labelSp = labelSizeSp(165.6f, 376.8f, 1f)
        val zone = labelZoneDp(376.8f, labelSp)
        assertTrue("the zone would be ${zone} dp tall", zone < 376.8f * 0.28f)
        assertEquals(labelSp * 2.2f, zone, 0.01f)
    }

    @Test
    fun `a single line fits into the cell's width`() {
        // out of the height alone "4:54 PM" got a size at which the text overlapped itself.
        val short = singleLineSizeSp("9:41", CELL_W, CELL_H, userScale = 1f)
        val long = singleLineSizeSp("12:59 PM", CELL_W, CELL_H, userScale = 1f)
        assertTrue("longer text has to be set smaller", long < short)
        assertTrue("does not fit the width", long * 8 * 0.60f <= CELL_W - 16f + 0.5f)
    }

    @Test
    fun `on the default cell the width already limits at four characters`() {
        // (165.6 - 16) / (4 * 0.60) = 62.3 sp, while the height would allow 63.4.
        assertEquals(62.3f, singleLineSizeSp("9:41", CELL_W, CELL_H, 1f), 0.3f)
    }

    @Test
    fun `on a wide flat cell the height limits`() {
        assertEquals(34f, singleLineSizeSp("9:41", cellWidthDp = 600f, cellHeightDp = 100f, userScale = 1f), 0.3f)
    }

    @Test
    fun `the single line has an upper and a lower bound`() {
        assertEquals(12f, singleLineSizeSp("sehr langer text hier", 60f, 40f, 1f), 0.5f)
        assertEquals(72f, singleLineSizeSp("9", 900f, 900f, 1f), 0.5f)
    }

    @Test
    fun `empty text does not crash`() {
        assertTrue(singleLineSizeSp("", CELL_W, CELL_H, 1f) > 0f)
    }

    @Test
    fun `the icon measures 40 percent of the shorter edge`() {
        assertEquals(66.2f, iconSizeDp(cellWidthDp = 165.6f, cellHeightDp = 186.4f), 0.2f)
        assertEquals(40f, iconSizeDp(cellWidthDp = 300f, cellHeightDp = 100f), 0.01f)
        assertEquals(96f, iconSizeDp(cellWidthDp = 400f, cellHeightDp = 400f), 0.01f)
        assertEquals(24f, iconSizeDp(cellWidthDp = 30f, cellHeightDp = 30f), 0.01f)
    }

    /**
     * the two ends a person really sets, looked at on the device: the densest grid at the
     * smallest type and the widest at the largest. the rules above check shapes only.
     */
    @Test
    fun `the densest grid stays usable at the smallest type`() {
        val m = gridMetrics(width, height, cols = 3, rows = 5, gutter = gutter, borderPercent = 2)
        // 48 dp is the touch target below which people miss.
        assertTrue("the cell is ${m.cellWidth} dp wide", m.cellWidth >= 48f)
        assertTrue("the cell is ${m.cellHeight} dp tall", m.cellHeight >= 48f)
        val label = labelSizeSp(m.cellWidth, m.cellHeight, userScale = 0.75f, labelScale = 1.0f)
        assertTrue("the label is only $label sp", label >= 14f * 0.75f)
        val zone = labelZoneDp(m.cellHeight, label)
        assertTrue("only ${m.cellHeight - zone} dp are left for the icon", m.cellHeight - zone > 24f)
    }

    @Test
    fun `the widest grid carries the largest type`() {
        val m = gridMetrics(width, height, cols = 1, rows = 2, gutter = gutter, borderPercent = 2)
        val label = labelSizeSp(m.cellWidth, m.cellHeight, userScale = 2.0f, labelScale = 1.0f)
        assertTrue("the label is $label sp", label <= 40f * 2.0f)
        val zone = labelZoneDp(m.cellHeight, label)
        assertTrue("the label zone eats the cell", zone <= m.cellHeight * 0.5f)
    }
}

/**
 * PLAN.md 4.2: label size 50-150 %, icon size 20-60 % of the cell.
 *
 * both are numbers a person could break the tile with, so the bounds stand here together
 * with what happens beyond them.
 */
class TileSizingTest {

    private val wide = 165.6f
    private val tall = 186.4f

    @Test
    fun `the label size multiplies onto the global text size`() {
        val normal = labelSizeSp(wide, tall, userScale = 1f, labelScale = 1f)
        val large = labelSizeSp(wide, tall, userScale = 1f, labelScale = 1.5f)
        assertEquals(normal * 1.5f, large, 0.01f)
    }

    // without a bound an imported file could set the label to nothing, and a tile without a
    // readable word is one to be guessed at.
    @Test
    fun `the label size is clamped`() {
        val tooSmall = labelSizeSp(wide, tall, userScale = 1f, labelScale = 0.1f)
        val smallest = labelSizeSp(wide, tall, userScale = 1f, labelScale = LABEL_SCALE_MIN)
        assertEquals(smallest, tooSmall, 0.01f)

        val tooLarge = labelSizeSp(wide, tall, userScale = 1f, labelScale = 9f)
        val largest = labelSizeSp(wide, tall, userScale = 1f, labelScale = LABEL_SCALE_MAX)
        assertEquals(largest, tooLarge, 0.01f)
    }

    @Test
    fun `the icon size follows the percentage`() {
        val twenty = iconSizeDp(wide, tall, percent = 20)
        val forty = iconSizeDp(wide, tall, percent = 40)
        assertEquals(true, forty > twenty)
    }

    /**
     * 60 % on a flat tile plus the label zone is more than the tile is tall: the icon lay
     * over the word and both read worse than before.
     */
    @Test
    fun `the icon never grows into the label`() {
        val flat = 90f
        val labelSp = labelSizeSp(340f, flat, userScale = 1f)
        val zone = labelZoneDp(flat, labelSp)
        val icon = iconSizeDp(340f, flat, percent = 60, labelZoneDp = zone)
        assertEquals(true, icon + zone <= flat)
    }

    // the counter-check to that cap: without a label the whole cell belongs to the icon.
    @Test
    fun `without a label the cap does not apply`() {
        assertEquals(24f, iconSizeDp(30f, 30f, percent = 40, labelZoneDp = 0f), 0.01f)
    }

    @Test
    fun `an impossible percentage is clamped`() {
        assertEquals(iconSizeDp(wide, tall, ICON_PERCENT_MAX), iconSizeDp(wide, tall, 200), 0.01f)
        assertEquals(iconSizeDp(wide, tall, ICON_PERCENT_MIN), iconSizeDp(wide, tall, 0), 0.01f)
    }

    // the defaults are what the app painted before: nobody's home screen may change just
    // because the setting now exists.
    @Test
    fun `the defaults change nothing`() {
        assertEquals(1.0f, dev.kicker.hometiles.data.Appearance().labelScale, 0.001f)
        assertEquals(40, dev.kicker.hometiles.data.Appearance().iconPercent)
        assertEquals(
            labelSizeSp(wide, tall, userScale = 1f),
            labelSizeSp(wide, tall, userScale = 1f, labelScale = 1.0f),
            0.001f,
        )
    }
}

/**
 * the label zone needs a floor.
 *
 * a cell 60 dp tall got 16.8 dp out of the 28-percent rule, and a bold 14 sp line needs
 * nearly 19 with its descenders. it is not only landscape: every flat tile in a dense grid
 * has the same problem.
 */
class LabelZoneFloorTest {

    @Test
    fun `a flat cell still gets one full line`() {
        val labelSp = labelSizeSp(cellWidthDp = 420f, cellHeightDp = 60f, userScale = 1f)
        val zone = labelZoneDp(60f, labelSp)
        assertTrue("zone $zone is not enough for $labelSp sp", zone >= labelSp * 1.3f)
    }

    // the floor must not eat the tile: with nothing left for the content the tile is a line
    // of text on a coloured surface.
    @Test
    fun `the floor never takes more than half the tile`() {
        for (height in listOf(30f, 40f, 60f, 90f, 190f)) {
            val labelSp = labelSizeSp(300f, height, userScale = 2f)
            assertTrue(labelZoneDp(height, labelSp) <= height * 0.5f + 0.01f)
        }
    }

    // counter-check: on tall tiles nothing changes, the old upper bound still holds.
    @Test
    fun `tall tiles stay as they were`() {
        val labelSp = labelSizeSp(165.6f, 376.8f, userScale = 1f)
        assertEquals(labelSp * 2.2f, labelZoneDp(376.8f, labelSp), 0.01f)
    }
}
