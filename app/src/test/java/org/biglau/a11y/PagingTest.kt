package org.biglau.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagingTest {

    @Test
    fun `eine Seite blaettert um eine Seite minus eine Zeile`() {
        assertEquals(4, Paging.step(5))
        assertEquals(0 + 4, Paging.down(firstVisible = 0, visibleCount = 5, total = 100))
    }

    @Test
    fun `die Ueberlappung verhindert uebersprungene Zeilen`() {
        // Sichtbar sind 0..4. Nach dem Druck beginnt die Liste bei 4 - Zeile 4 ist zweimal
        // zu sehen. Ohne diese Überlappung waere Zeile 5 der neue Anfang und Zeile 4 nie
        // in Ruhe lesbar gewesen.
        val next = Paging.down(firstVisible = 0, visibleCount = 5, total = 100)
        assertTrue("keine Luecke", next <= 0 + 5)
    }

    @Test
    fun `am Ende bleibt der letzte Bildschirm voll`() {
        // 10 Eintraege, 4 passen: der letzte sinnvolle Start ist 6, nicht 9.
        assertEquals(6, Paging.lastStart(visibleCount = 4, total = 10))
        assertEquals(6, Paging.down(firstVisible = 5, visibleCount = 4, total = 10))
        assertEquals(6, Paging.down(firstVisible = 6, visibleCount = 4, total = 10))
    }

    @Test
    fun `am Anfang blaettert zurueck nicht ins Negative`() {
        assertEquals(0, Paging.up(firstVisible = 2, visibleCount = 8))
        assertEquals(0, Paging.up(firstVisible = 0, visibleCount = 8))
    }

    @Test
    fun `kurze Liste kennt keine Richtung`() {
        assertFalse(Paging.canGoUp(0))
        assertFalse(Paging.canGoDown(firstVisible = 0, visibleCount = 8, total = 3))
    }

    @Test
    fun `lange Liste kennt beide Richtungen in der Mitte`() {
        assertTrue(Paging.canGoUp(20))
        assertTrue(Paging.canGoDown(firstVisible = 20, visibleCount = 6, total = 338))
    }

    @Test
    fun `leere Liste laesst sich nicht blaettern`() {
        assertEquals(0, Paging.lastStart(visibleCount = 6, total = 0))
        assertFalse(Paging.canGoDown(firstVisible = 0, visibleCount = 6, total = 0))
        assertEquals(0, Paging.down(firstVisible = 0, visibleCount = 6, total = 0))
    }

    @Test
    fun `eine einzige sichtbare Zeile blaettert trotzdem weiter`() {
        // Eine sehr hohe Zeile fuellt den Bildschirm allein. step waere sonst 0 und der
        // Knopf taete nichts - der klassische tote Knopf.
        assertEquals(1, Paging.step(1))
        assertEquals(1, Paging.down(firstVisible = 0, visibleCount = 1, total = 9))
    }

    @Test
    fun `unbekannte Sichtbarkeit blockiert den Knopf nicht`() {
        // Vor der ersten Messung meldet die Liste 0 sichtbare Zeilen.
        assertEquals(1, Paging.step(0))
        assertEquals(1, Paging.down(firstVisible = 0, visibleCount = 0, total = 50))
    }
}

class PagingVisibilityTest {

    // Sichtfenster 0..100, Zeilen 40 hoch: die dritte haengt hinten heraus.
    private val rows = listOf(
        Paging.Row(index = 3, offset = -10, size = 40),
        Paging.Row(index = 4, offset = 30, size = 40),
        Paging.Row(index = 5, offset = 70, size = 40),
    )

    @Test
    fun `angeschnittene Zeilen zaehlen nicht mit`() {
        val whole = Paging.fullyVisible(rows, viewportStart = 0, viewportEnd = 100)
        assertEquals(listOf(4), whole.map { it.index })
    }

    @Test
    fun `der Sprung beginnt bei der ersten ganz sichtbaren Zeile`() {
        assertEquals(4, Paging.firstFullyVisibleIndex(rows, 0, 100, fallback = 3))
    }

    @Test
    fun `ohne eine einzige ganze Zeile bleibt der bekannte Anfang stehen`() {
        // Eine einzelne sehr hohe Zeile fuellt das Fenster und ragt oben wie unten hinaus.
        val riesig = listOf(Paging.Row(index = 7, offset = -20, size = 300))
        assertEquals(7, Paging.firstFullyVisibleIndex(riesig, 0, 100, fallback = 7))
        assertTrue(Paging.fullyVisible(riesig, 0, 100).isEmpty())
    }

    @Test
    fun `blaettern springt genau auf die letzte ganz sichtbare Zeile`() {
        // Sichtbar ganz: 4,5,6,7. Danach steht 7 oben - eine Zeile Ueberlappung.
        val ganze = (4..7).map { Paging.Row(it, (it - 4) * 25, 25) }
        val first = Paging.firstFullyVisibleIndex(ganze, 0, 100, fallback = 4)
        val count = Paging.fullyVisible(ganze, 0, 100).size
        assertEquals(7, Paging.down(first, count, total = 338))
    }
}
