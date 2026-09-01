package org.biglau.tiles

import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * „Nächster Screen" und „voriger Screen".
 *
 * Beide standen bisher im else-Zweig der Kachelbehandlung und meldeten „demnächst" - der
 * Plan sagt sie in 4.3 zu. Aufgefallen beim Abgleich der Konfigurationsmatrix gegen das
 * Gebaute; ein else-Zweig verbirgt so etwas, ein vollständiges when nicht.
 */
class ScreenOrderTest {

    private fun screen(id: String) = Screen(id = id, name = id)

    private val drei = LauncherConfig(
        screens = listOf(screen("a"), screen("b"), screen("c")),
        homeScreenId = "a",
    )

    @Test
    fun `weiter geht der Reihe nach`() {
        assertEquals("b", ScreenOrder.next(drei, "a"))
        assertEquals("c", ScreenOrder.next(drei, "b"))
    }

    @Test
    fun `die Reihe ist ein Ring`() {
        // Sonst taete die Kachel am letzten Screen nichts - ein toter Knopf.
        assertEquals("a", ScreenOrder.next(drei, "c"))
        assertEquals("c", ScreenOrder.previous(drei, "a"))
    }

    @Test
    fun `zurueck geht rueckwaerts`() {
        assertEquals("a", ScreenOrder.previous(drei, "b"))
    }

    @Test
    fun `bei einem einzigen Screen gibt es nichts zu blaettern`() {
        val einer = LauncherConfig(screens = listOf(screen("a")), homeScreenId = "a")
        assertNull(ScreenOrder.next(einer, "a"))
        assertNull(ScreenOrder.previous(einer, "a"))
    }

    @Test
    fun `Ordner liegen nicht in der Reihe`() {
        // Wer "weiter" tippt, erwartet den naechsten Bildschirm, nicht einen Ordnerinhalt.
        val mitOrdner = LauncherConfig(
            screens = listOf(screen("a"), Screen(id = "f", name = "F", kind = ScreenKind.FOLDER), screen("b")),
            homeScreenId = "a",
        )
        assertEquals(listOf("a", "b"), ScreenOrder.ordered(mitOrdner).map { it.id })
        assertEquals("b", ScreenOrder.next(mitOrdner, "a"))
    }

    @Test
    fun `eine eigene Reihenfolge wird beachtet`() {
        val eigen = drei.copy(swipeOrder = listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), ScreenOrder.ordered(eigen).map { it.id })
        assertEquals("a", ScreenOrder.next(eigen, "c"))
    }

    @Test
    fun `ein Screen ausserhalb der eigenen Reihenfolge faellt nicht weg`() {
        // Sonst waere er ueber "weiter" nie erreichbar - dieselbe Falle wie ein Screen
        // ohne Sprungkachel.
        val eigen = drei.copy(swipeOrder = listOf("c"))
        assertEquals(listOf("c", "a", "b"), ScreenOrder.ordered(eigen).map { it.id })
    }

    @Test
    fun `ein unbekannter Screen fuehrt nirgendwohin`() {
        assertNull(ScreenOrder.next(drei, "gibtsnicht"))
    }
}
