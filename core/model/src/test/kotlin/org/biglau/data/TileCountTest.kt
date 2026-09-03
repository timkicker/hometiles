package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Eine Kachel ist eine belegte Zelle - eine leere ist ein freier Platz.
 *
 * Der Unterschied klingt spitzfindig und war es nicht: beim Laden einer Sicherung stand
 * „3 screens with 14 tiles", beim Zurücksetzen „2 screens with 14 tiles and 1 folder" —
 * dieselbe Einrichtung, zwei Zahlen, weil drei Stellen `cells.size` zählten und eine
 * `cells.count { … != None }`. Auf einem halb belegten Raster hätte die Sicherung mehr
 * Kacheln versprochen, als sie enthält.
 *
 * Jetzt gibt es die Zahl **einmal**, und diese Regel sagt, was sie bedeutet.
 */
class TileCountTest {

    private fun zelle(x: Int, action: ButtonAction) =
        Cell(x = x, y = 0, button = Button(action = action))

    @Test
    fun `leere Zellen zaehlen nicht mit`() {
        val screen = Screen(
            id = "s",
            name = "Probe",
            cells = listOf(
                zelle(0, ButtonAction.GoToScreen("home")),
                zelle(1, ButtonAction.None),
                zelle(2, ButtonAction.App("org.example", "Main")),
            ),
        )
        assertEquals(3, screen.cells.size)
        assertEquals(2, screen.tileCount)
    }

    @Test
    fun `ein leerer Screen hat keine Kacheln`() {
        assertEquals(0, Screen(id = "leer", name = "Leer").tileCount)
    }

    /**
     * Eine leere Zelle mit eigener Beschriftung bleibt eine leere Zelle: der
     * Startbildschirm zeigt dort zwar ihren Text statt „Antippen zum Belegen", aber
     * angetippt passiert nichts. Sie zu zählen hiesse, eine Kachel zu versprechen, die
     * nichts tut.
     */
    @Test
    fun `eine leere Zelle mit Beschriftung ist trotzdem keine Kachel`() {
        val screen = Screen(
            id = "s",
            name = "Probe",
            cells = listOf(Cell(x = 0, y = 0, button = Button(label = "Später"))),
        )
        assertEquals(0, screen.tileCount)
    }
}
