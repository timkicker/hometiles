package org.biglau.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFitTest {

    // the measured cell of the Jelly 2 in the standard grid, see PLAN.md 3.2
    private val cellW = 165.6f
    private val cellH = 186.4f
    private val gutter = 4f

    private fun row(w: Int, h: Int, label: String = "Clock", app: String = "Clock app") =
        WidgetProviderRow("com.clock", "com.clock.Widget", label, app, w, h)

    @Test
    fun `a small widget needs one field`() {
        assertEquals(1, WidgetFit.cellsNeeded(100, cellW, gutter))
        assertEquals(1, WidgetFit.cellsNeeded(165, cellW, gutter))
    }

    @Test
    fun `a wider widget needs two fields`() {
        // 166 dp do not fit into 165.6 dp - and a squeezed widget looks broken.
        assertEquals(2, WidgetFit.cellsNeeded(200, cellW, gutter))
        assertEquals(2, WidgetFit.cellsNeeded(335, cellW, gutter))
    }

    @Test
    fun `the gutter counts too`() {
        // two cells carry 165.6 + 4 + 165.6 = 335.2 dp, not 331.2.
        assertEquals(2, WidgetFit.cellsNeeded(335, cellW, gutter))
        assertEquals(3, WidgetFit.cellsNeeded(336, cellW, gutter))
    }

    @Test
    fun `a widget without a minimum size needs one field`() {
        assertEquals(1, WidgetFit.cellsNeeded(0, cellW, gutter))
        assertEquals(1, WidgetFit.cellsNeeded(-50, cellW, gutter))
    }

    @Test
    fun `the arithmetic does not run forever on nonsensical values`() {
        assertEquals(1, WidgetFit.cellsNeeded(500, 0f, gutter))
        assertEquals(WidgetFit.MAX_SPAN, WidgetFit.cellsNeeded(100_000, cellW, gutter))
    }

    @Test
    fun `a fitting widget fits`() {
        assertTrue(WidgetFit.fits(row(150, 150), 1, 1, cellW, cellH, gutter))
    }

    @Test
    fun `a widget too wide does not fit into one field`() {
        assertTrue(!WidgetFit.fits(row(300, 150), 1, 1, cellW, cellH, gutter))
        assertTrue(WidgetFit.fits(row(300, 150), 2, 1, cellW, cellH, gutter))
    }

    @Test
    fun `a widget too tall does not fit into one field`() {
        assertTrue(!WidgetFit.fits(row(150, 300), 1, 1, cellW, cellH, gutter))
        assertTrue(WidgetFit.fits(row(150, 300), 1, 2, cellW, cellH, gutter))
    }

    @Test
    fun `the requirement is reported as a number of fields`() {
        assertEquals(2 to 1, WidgetFit.requirement(row(300, 150), cellW, cellH, gutter))
        assertEquals(1 to 2, WidgetFit.requirement(row(150, 300), cellW, cellH, gutter))
    }

    @Test
    fun `the list sorts by app and then by widget name`() {
        val rows = listOf(
            row(1, 1, label = "Zebra", app = "Beta app"),
            row(1, 1, label = "Anna", app = "Beta app").copy(className = "b"),
            row(1, 1, label = "Karl", app = "Alpha app").copy(className = "c"),
        )
        assertEquals(listOf("Karl", "Anna", "Zebra"), WidgetFit.sorted(rows).map { it.label })
    }

    @Test
    fun `nameless providers fly out`() {
        val rows = listOf(row(1, 1, label = "  "), row(1, 1, label = "Clock").copy(className = "b"))
        assertEquals(listOf("Clock"), WidgetFit.sorted(rows).map { it.label })
    }

    @Test
    fun `the same provider appears only once`() {
        assertEquals(1, WidgetFit.sorted(listOf(row(1, 1), row(1, 1))).size)
    }

    // --- fixed size (02.09.2026) ---

    /**
     * the provider reports whether it can be stretched; that lay unread in the model until
     * here (found with `DeadFieldTest`). whoever fills a large tile with a widget that can
     * only do one field gets it pulled in anyway - and takes the result for a fault of the app.
     */
    @Test
    fun `without stretchability the size is fixed`() {
        assertTrue(WidgetFit.fixedSize(row(100, 100)))
    }

    @Test
    fun `whatever can be stretched has no fixed size`() {
        val horizontal = WidgetProviderRow(
            "com.clock", "com.clock.Widget", "Clock", "Clock app", 100, 100,
            resizeHorizontal = true,
        )
        val vertical = WidgetProviderRow(
            "com.clock", "com.clock.Widget", "Clock", "Clock app", 100, 100,
            resizeVertical = true,
        )
        assertTrue(!WidgetFit.fixedSize(horizontal))
        assertTrue(!WidgetFit.fixedSize(vertical))
    }
}
