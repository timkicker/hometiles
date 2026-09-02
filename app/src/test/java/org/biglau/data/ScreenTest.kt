package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Zellgeometrie traegt spaeter Verbinden, Teilen, Strecken und Schrumpfen.
 * Hier wird festgehalten, was "belegt" ueberhaupt heisst.
 */
class ScreenTest {

    private fun cell(x: Int, y: Int, w: Int = 1, h: Int = 1) =
        Cell(x, y, w, h, Button(ButtonAction.Action(Builtin.DIALER)))

    @Test
    fun `eine 1x1-Zelle deckt genau ihren Platz ab`() {
        val c = cell(1, 2)
        assertTrue(c.covers(1, 2))
        assertTrue(!c.covers(0, 2))
        assertTrue(!c.covers(1, 3))
        assertEquals(1, c.area)
    }

    @Test
    fun `eine gespannte Zelle deckt alle Plaetze darunter ab`() {
        val c = cell(0, 0, w = 2, h = 2)
        assertEquals(4, c.area)
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1).forEach { (x, y) ->
            assertTrue("($x,$y) muesste belegt sein", c.covers(x, y))
        }
        assertTrue(!c.covers(2, 0))
        assertTrue(!c.covers(0, 2))
    }

    @Test
    fun `cellAt findet auch ueber die linke obere Ecke hinaus`() {
        val screen = Screen(id = "s", name = "Test", cols = 3, rows = 3, cells = listOf(cell(1, 1, 2, 2)))
        assertNull(screen.cellAt(0, 0))
        assertEquals(1, screen.cellAt(2, 2)?.x)
        assertEquals(1, screen.cellAt(1, 2)?.x)
    }

    @Test
    fun `freie Plaetze sind genau die nicht ueberdeckten`() {
        val screen = Screen(id = "s", name = "Test", cols = 3, rows = 2, cells = listOf(cell(0, 0, 2, 2)))
        val free = screen.freeSlots()
        assertEquals(2, free.size)
        assertTrue(free.containsAll(listOf(2 to 0, 2 to 1)))
    }

    @Test
    fun `ein leerer Screen ist vollstaendig frei`() {
        val screen = Screen(id = "s", name = "Leer", cols = 2, rows = 3)
        assertEquals(6, screen.freeSlots().size)
    }

    @Test
    fun `die Startbelegung fuellt das Standardraster ohne Luecke`() {
        val screen = Defaults.mainScreen()
        assertEquals(2, screen.cols)
        assertEquals(3, screen.rows)
        assertEquals(6, screen.cells.size)
        assertTrue("Startscreen darf keine Luecke haben", screen.freeSlots().isEmpty())
    }

    @Test
    fun `keine zwei Zellen ueberlappen sich in der Startbelegung`() {
        val screen = Defaults.mainScreen()
        val occupied = mutableSetOf<Pair<Int, Int>>()
        screen.cells.forEach { c ->
            for (x in c.x until c.x + c.w) {
                for (y in c.y until c.y + c.h) {
                    assertTrue("($x,$y) ist doppelt belegt", occupied.add(x to y))
                }
            }
        }
    }

    @Test
    fun `jedes Layout-Preset ist im erlaubten Bereich`() {
        Defaults.layouts.forEach { (cols, rows) ->
            assertTrue("$cols Spalten ausserhalb 1..6", cols in 1..6)
            assertTrue("$rows Zeilen ausserhalb 1..8", rows in 1..8)
        }
    }
}
