package org.biglau.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFitTest {

    // Gemessene Zelle des Jelly 2 im Standardraster, siehe PLAN.md 3.2
    private val cellW = 165.6f
    private val cellH = 186.4f
    private val gutter = 4f

    private fun row(w: Int, h: Int, label: String = "Uhr", app: String = "Uhr-App") =
        WidgetProviderRow("com.clock", "com.clock.Widget", label, app, w, h)

    @Test
    fun `ein kleines Widget braucht ein Feld`() {
        assertEquals(1, WidgetFit.cellsNeeded(100, cellW, gutter))
        assertEquals(1, WidgetFit.cellsNeeded(165, cellW, gutter))
    }

    @Test
    fun `ein breiteres Widget braucht zwei Felder`() {
        // 166 dp passen nicht in 165,6 dp - und ein gestauchtes Widget sieht kaputt aus.
        assertEquals(2, WidgetFit.cellsNeeded(200, cellW, gutter))
        assertEquals(2, WidgetFit.cellsNeeded(335, cellW, gutter))
    }

    @Test
    fun `der Zwischenraum zaehlt mit`() {
        // Zwei Zellen tragen 165,6 + 4 + 165,6 = 335,2 dp, nicht 331,2.
        assertEquals(2, WidgetFit.cellsNeeded(335, cellW, gutter))
        assertEquals(3, WidgetFit.cellsNeeded(336, cellW, gutter))
    }

    @Test
    fun `ein Widget ohne Mindestmass braucht ein Feld`() {
        assertEquals(1, WidgetFit.cellsNeeded(0, cellW, gutter))
        assertEquals(1, WidgetFit.cellsNeeded(-50, cellW, gutter))
    }

    @Test
    fun `die Rechnung laeuft nicht endlos bei unsinnigen Werten`() {
        assertEquals(1, WidgetFit.cellsNeeded(500, 0f, gutter))
        assertEquals(WidgetFit.MAX_SPAN, WidgetFit.cellsNeeded(100_000, cellW, gutter))
    }

    @Test
    fun `ein passendes Widget passt`() {
        assertTrue(WidgetFit.fits(row(150, 150), 1, 1, cellW, cellH, gutter))
    }

    @Test
    fun `ein zu breites Widget passt nicht in ein Feld`() {
        assertTrue(!WidgetFit.fits(row(300, 150), 1, 1, cellW, cellH, gutter))
        assertTrue(WidgetFit.fits(row(300, 150), 2, 1, cellW, cellH, gutter))
    }

    @Test
    fun `ein zu hohes Widget passt nicht in ein Feld`() {
        assertTrue(!WidgetFit.fits(row(150, 300), 1, 1, cellW, cellH, gutter))
        assertTrue(WidgetFit.fits(row(150, 300), 1, 2, cellW, cellH, gutter))
    }

    @Test
    fun `die Anforderung wird als Feldzahl gemeldet`() {
        assertEquals(2 to 1, WidgetFit.requirement(row(300, 150), cellW, cellH, gutter))
        assertEquals(1 to 2, WidgetFit.requirement(row(150, 300), cellW, cellH, gutter))
    }

    @Test
    fun `die Liste ist nach App und dann nach Widgetname sortiert`() {
        val rows = listOf(
            row(1, 1, label = "Zebra", app = "Beta-App"),
            row(1, 1, label = "Anna", app = "Beta-App").copy(className = "b"),
            row(1, 1, label = "Karl", app = "Alpha-App").copy(className = "c"),
        )
        assertEquals(listOf("Karl", "Anna", "Zebra"), WidgetFit.sorted(rows).map { it.label })
    }

    @Test
    fun `namenlose Anbieter fliegen raus`() {
        val rows = listOf(row(1, 1, label = "  "), row(1, 1, label = "Uhr").copy(className = "b"))
        assertEquals(listOf("Uhr"), WidgetFit.sorted(rows).map { it.label })
    }

    @Test
    fun `derselbe Anbieter erscheint nur einmal`() {
        assertEquals(1, WidgetFit.sorted(listOf(row(1, 1), row(1, 1))).size)
    }
}
