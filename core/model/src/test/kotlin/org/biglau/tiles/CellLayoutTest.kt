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
    fun `eine Zelle waechst in freien Platz`() {
        val target = cell(0, 0)
        val result = CellLayout.stretch(screen(target), target, Direction.RIGHT)
        assertEquals(1, result.cells.size)
        assertEquals(2, result.cells.first().w)
    }

    @Test
    fun `nach dem Wachsen deckt die Zelle beide Plaetze ab`() {
        val target = cell(0, 0)
        val result = CellLayout.stretch(screen(target), target, Direction.RIGHT)
        val grown = result.cells.first()
        assertTrue(grown.covers(0, 0))
        assertTrue(grown.covers(1, 0))
        assertTrue(result.freeSlots().none { it == 1 to 0 })
    }

    @Test
    fun `eine Zelle waechst nicht ueber den Rand hinaus`() {
        val target = cell(1, 0)
        assertTrue(!CellLayout.canStretch(screen(target), target, Direction.RIGHT))
        assertTrue(!CellLayout.canStretch(screen(target), target, Direction.UP))
    }

    @Test
    fun `eine Zelle ueberschreibt keine belegte Nachbarin`() {
        // Genau das waere Datenverlust an der unerwartetsten Stelle.
        val target = cell(0, 0)
        val neighbour = cell(1, 0, name = Builtin.CAMERA)
        val board = screen(target, neighbour)
        assertTrue(!CellLayout.canStretch(board, target, Direction.RIGHT))
        assertEquals(board, CellLayout.stretch(board, target, Direction.RIGHT))
    }

    @Test
    fun `Wachsen nach oben verschiebt auch die Ecke`() {
        val target = cell(0, 1)
        val grown = CellLayout.stretch(screen(target), target, Direction.UP).cells.first()
        assertEquals(0, grown.y)
        assertEquals(2, grown.h)
    }

    @Test
    fun `Wachsen nach links verschiebt auch die Ecke`() {
        val target = cell(1, 0)
        val grown = CellLayout.stretch(screen(target), target, Direction.LEFT).cells.first()
        assertEquals(0, grown.x)
        assertEquals(2, grown.w)
    }

    @Test
    fun `eine breite Zelle kann nicht in eine teilweise belegte Reihe wachsen`() {
        val target = cell(0, 0, w = 2)
        val blocker = cell(1, 1, name = Builtin.CAMERA)
        assertTrue(!CellLayout.canStretch(screen(target, blocker), target, Direction.DOWN))
    }

    @Test
    fun `eine breite Zelle waechst in eine ganz freie Reihe`() {
        val target = cell(0, 0, w = 2)
        assertTrue(CellLayout.canStretch(screen(target), target, Direction.DOWN))
    }

    @Test
    fun `Verkleinern gibt Plaetze wieder frei`() {
        val target = cell(0, 0, w = 2)
        val result = CellLayout.shrink(screen(target), target, Direction.RIGHT)
        assertEquals(1, result.cells.first().w)
        assertTrue(result.freeSlots().contains(1 to 0))
    }

    @Test
    fun `Verkleinern von links schiebt die Ecke nach rechts`() {
        val target = cell(0, 0, w = 2)
        val shrunk = CellLayout.shrink(screen(target), target, Direction.LEFT).cells.first()
        assertEquals(1, shrunk.x)
        assertEquals(1, shrunk.w)
        assertTrue(CellLayout.shrink(screen(target), target, Direction.LEFT).freeSlots().contains(0 to 0))
    }

    @Test
    fun `eine einfache Zelle laesst sich nicht weiter verkleinern`() {
        val target = cell(0, 0)
        Direction.entries.forEach { assertTrue(!CellLayout.canShrink(target, it)) }
        assertEquals(screen(target), CellLayout.shrink(screen(target), target, Direction.RIGHT))
    }

    @Test
    fun `Wachsen und Verkleinern heben sich auf`() {
        val target = cell(0, 0)
        val board = screen(target)
        Direction.entries.filter { CellLayout.canStretch(board, target, it) }.forEach { direction ->
            val grown = CellLayout.stretch(board, target, direction)
            val back = CellLayout.shrink(grown, grown.cells.first(), direction)
            assertEquals("Richtung $direction", board, back)
        }
    }

    @Test
    fun `moegliche Richtungen decken sich mit der Einzelpruefung`() {
        val target = cell(0, 1)
        val board = screen(target, cell(1, 1, name = Builtin.CAMERA))
        assertEquals(listOf(Direction.UP, Direction.DOWN), CellLayout.stretchable(board, target))
        assertTrue(CellLayout.shrinkable(target).isEmpty())
    }

    @Test
    fun `nach dem Leeren kann die Nachbarin in den Platz wachsen`() {
        // Genau der Fall, der am Geraet auffiel: eine geleerte Zelle blieb als Zelle
        // ohne Aktion stehen und blockierte das Vergroessern, ohne dass man es sah.
        val neighbour = cell(0, 0)
        val board = screen(neighbour, cell(0, 1, name = Builtin.CAMERA))
        assertTrue(!CellLayout.canStretch(board, neighbour, Direction.DOWN))

        val emptied = board.copy(cells = board.cells.filter { it.y == 0 })
        assertTrue(CellLayout.canStretch(emptied, neighbour, Direction.DOWN))
    }

    @Test
    fun `eine Zelle waechst auf die gewuenschte Groesse`() {
        val target = cell(0, 0)
        val grown = CellLayout.growTo(screen(target), target, targetWidth = 2, targetHeight = 2)
        assertTrue(grown != null)
        val result = grown!!.cells.first()
        assertTrue(result.w >= 2 && result.h >= 2)
    }

    @Test
    fun `Wachsen laesst die Nachbarn unangetastet`() {
        val target = cell(0, 0)
        val neighbour = cell(1, 0, name = Builtin.CAMERA)
        val board = screen(target, neighbour)
        // Nach rechts geht nicht, nach unten schon.
        val grown = CellLayout.growTo(board, target, targetWidth = 1, targetHeight = 2)
        assertTrue(grown != null)
        assertTrue(grown!!.cells.any { it.button == neighbour.button && it.w == 1 && it.h == 1 })
    }

    @Test
    fun `ohne genug Platz wird gar nicht gewachsen`() {
        // Eine halb gewachsene Zelle waere schlimmer als eine Absage.
        val target = cell(0, 0)
        val board = screen(target, cell(1, 0, name = Builtin.CAMERA), cell(0, 1, name = Builtin.CLOCK))
        assertNull(CellLayout.growTo(board, target, targetWidth = 2, targetHeight = 2))
    }

    @Test
    fun `eine bereits passende Zelle bleibt unveraendert`() {
        val target = cell(0, 0, w = 2, h = 2)
        val board = screen(target, cols = 2, rows = 3)
        assertEquals(board, CellLayout.growTo(board, target, 2, 2))
        assertEquals(board, CellLayout.growTo(board, target, 1, 1))
    }

    @Test
    fun `Wachsen ueber den Rand hinaus wird abgelehnt`() {
        val target = cell(0, 0)
        assertNull(CellLayout.growTo(screen(target, cols = 2, rows = 3), target, 3, 1))
    }

    @Test
    fun `ein kleineres Raster wirft herausgefallene Zellen weg`() {
        val board = Screen(id = "s", name = "T", cols = 2, rows = 2, cells = listOf(cell(0, 0), cell(1, 2)))
        val fitted = CellLayout.fitToGrid(board)
        assertEquals(1, fitted.cells.size)
        assertEquals(0, fitted.cells.first().y)
    }

    @Test
    fun `ein kleineres Raster stutzt ueberstehende Zellen`() {
        val board = Screen(id = "s", name = "T", cols = 2, rows = 2, cells = listOf(cell(0, 0, w = 2, h = 3)))
        val fitted = CellLayout.fitToGrid(board)
        assertEquals(2, fitted.cells.first().h)
        assertEquals(2, fitted.cells.first().w)
    }

    @Test
    fun `ein passendes Raster bleibt unveraendert`() {
        val board = screen(cell(0, 0, w = 2), cell(0, 1))
        assertEquals(board, CellLayout.fitToGrid(board))
    }
}
