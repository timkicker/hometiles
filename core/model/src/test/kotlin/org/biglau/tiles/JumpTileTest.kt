package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Sprungkachel, die aus der Warnung eine Handlung macht.
 *
 * „Auf ‚Screen 2' führt keine Kachel" sagte bisher nur, was zu tun wäre. Seit dem 3.9.2026
 * bietet die Seite es an — und dahinter steht diese Funktion.
 */
class JumpTileTest {

    private fun heim(vararg belegt: Pair<Int, Int>) = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 2,
        cells = belegt.map { (x, y) ->
            Cell(x = x, y = y, button = Button(action = ButtonAction.Action(org.biglau.data.Builtin.CAMERA)))
        },
    )

    private fun config(vararg belegt: Pair<Int, Int>) = LauncherConfig(
        screens = listOf(heim(*belegt), Screen(id = "zwei", name = "Screen 2")),
        homeScreenId = "home",
    )

    @Test
    fun `die Kachel landet auf dem ersten freien Platz des Startbildschirms`() {
        val neu = ScreenEdits.withJumpTile(config(0 to 0), "zwei")
        assertTrue(neu != null)
        val kachel = neu!!.screens.first { it.id == "home" }.cells
            .first { it.button.action is ButtonAction.GoToScreen }
        assertEquals(ButtonAction.GoToScreen("zwei"), kachel.button.action)
    }

    @Test
    fun `danach ist der Bildschirm erreichbar`() {
        val vorher = config(0 to 0)
        assertEquals(listOf("zwei"), ScreenEdits.unreachable(vorher).map { it.id })
        val neu = ScreenEdits.withJumpTile(vorher, "zwei")!!
        assertTrue("nach der Kachel darf nichts mehr unerreichbar sein", ScreenEdits.unreachable(neu).isEmpty())
    }

    @Test
    fun `auf einem vollen Startbildschirm geht es nicht`() {
        val voll = config(0 to 0, 1 to 0, 0 to 1, 1 to 1)
        assertNull("kein freier Platz - dann sagt die Oberfläche das", ScreenEdits.withJumpTile(voll, "zwei"))
    }

    @Test
    fun `ein Ziel, das es nicht gibt, legt keine Kachel an`() {
        assertNull(ScreenEdits.withJumpTile(config(0 to 0), "gibtsnicht"))
    }
}
