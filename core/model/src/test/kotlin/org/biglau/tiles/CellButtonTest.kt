package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Eine Kachel belegen und wieder leeren.
 *
 * Diese zwei Rechnungen standen bis zum 3.9.2026 in `ConfigStore` - dem einzigen Modul ohne
 * einen einzigen Test. Es sind die Rechnungen, die eine Kachel des Nutzers **überschreiben**
 * oder **löschen**; sie ohne Prüfung zu lassen war die schlechteste Stelle dafür.
 */
class CellButtonTest {

    private fun knopf(name: String) =
        Button(action = ButtonAction.GoToScreen(name), label = name)

    private fun bildschirm(vararg zellen: Cell) =
        Screen(id = "home", name = "Start", cols = 2, rows = 4, cells = zellen.toList())

    @Test
    fun `auf leerem Platz entsteht eine neue Zelle`() {
        val vorher = bildschirm()
        val nachher = CellLayout.withButton(vorher, 1, 2, knopf("a"))
        assertEquals(1, nachher.cells.size)
        assertEquals(1, nachher.cells[0].x)
        assertEquals(2, nachher.cells[0].y)
        assertEquals(1, nachher.cells[0].w)
        assertEquals(1, nachher.cells[0].h)
        assertEquals("a", nachher.cells[0].button.label)
    }

    @Test
    fun `eine belegte Zelle wird ueberschrieben, nicht verdoppelt`() {
        val vorher = bildschirm(Cell(x = 0, y = 0, button = knopf("alt")))
        val nachher = CellLayout.withButton(vorher, 0, 0, knopf("neu"))
        assertEquals(1, nachher.cells.size)
        assertEquals("neu", nachher.cells[0].button.label)
    }

    /**
     * Der Fall, der ohne Test durchgerutscht wäre: eine breite Zelle wird an **jeder**
     * Stelle getroffen, die sie überdeckt - nicht nur an ihrer Ecke. Sonst legte ein Tipp
     * auf die rechte Hälfte einer Doppelkachel eine zweite Zelle darüber.
     */
    @Test
    fun `eine breite Zelle wird auch an ihrem rechten Rand getroffen`() {
        val breit = Cell(x = 0, y = 0, w = 2, h = 1, button = knopf("breit"))
        val nachher = CellLayout.withButton(bildschirm(breit), 1, 0, knopf("neu"))
        assertEquals(1, nachher.cells.size)
        assertEquals(2, nachher.cells[0].w)
        assertEquals("neu", nachher.cells[0].button.label)
    }

    @Test
    fun `leeren laesst keine Zelle ohne Aktion zurueck`() {
        val vorher = bildschirm(Cell(x = 0, y = 0, button = knopf("a")))
        val nachher = CellLayout.withoutButton(vorher, 0, 0)
        assertEquals(emptyList<Cell>(), nachher.cells)
        assertNull(nachher.cellAt(0, 0))
    }

    @Test
    fun `leeren auf einem leeren Platz aendert nichts`() {
        val vorher = bildschirm(Cell(x = 0, y = 0, button = knopf("a")))
        assertEquals(vorher, CellLayout.withoutButton(vorher, 1, 3))
    }

    @Test
    fun `leeren trifft die breite Zelle auch an ihrem rechten Rand`() {
        val breit = Cell(x = 0, y = 0, w = 2, h = 1, button = knopf("breit"))
        assertEquals(emptyList<Cell>(), CellLayout.withoutButton(bildschirm(breit), 1, 0).cells)
    }
}
