package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.Defaults
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenEditsTest {

    private val home = Defaults.mainScreen()
    private val base = LauncherConfig(screens = listOf(home), homeScreenId = Defaults.MAIN_ID)

    @Test
    fun `ein neuer Screen hat immer einen Rueckweg`() {
        // Ein Launcher darf die Zurueck-Geste nicht abfangen. Ohne Heim-Kachel waere
        // der neue Screen eine Sackgasse.
        val screen = ScreenEdits.newScreen("s2", "Zweiter", home)
        val actions = screen.cells.map { it.button.action }
        assertTrue(actions.contains(ButtonAction.Action(Builtin.HOME_SCREEN)))
    }

    @Test
    fun `die Heim-Kachel sitzt auf dem letzten Platz`() {
        val screen = ScreenEdits.newScreen("s2", "Zweiter", home)
        val cell = screen.cells.first()
        assertEquals(home.cols - 1, cell.x)
        assertEquals(home.rows - 1, cell.y)
    }

    @Test
    fun `ein neuer Screen uebernimmt das Raster des alten`() {
        val wide = Screen(id = "a", name = "A", cols = 3, rows = 4)
        val screen = ScreenEdits.newScreen("s2", "Zweiter", wide)
        assertEquals(3, screen.cols)
        assertEquals(4, screen.rows)
        assertEquals(2, screen.cells.first().x)
        assertEquals(3, screen.cells.first().y)
    }

    @Test
    fun `eine neue Kennung faellt nie auf einen bestehenden Screen`() {
        val config = base.copy(screens = base.screens + Screen(id = "screen2", name = "X"))
        val id = ScreenEdits.freeId(config)
        assertTrue(config.screens.none { it.id == id })
    }

    @Test
    fun `dieselbe Kennung wird nicht zweimal hinzugefuegt`() {
        val once = ScreenEdits.add(base, Screen(id = "s2", name = "Zweiter"))
        val twice = ScreenEdits.add(once, Screen(id = "s2", name = "Anderer Name"))
        assertEquals(2, twice.screens.size)
        assertEquals("Zweiter", twice.screens.last().name)
    }

    @Test
    fun `Umbenennen trimmt und lehnt Leeres ab`() {
        val renamed = ScreenEdits.rename(base, Defaults.MAIN_ID, "  Zuhause  ")
        assertEquals("Zuhause", renamed.homeScreen.name)
        assertEquals(renamed, ScreenEdits.rename(renamed, Defaults.MAIN_ID, "   "))
    }

    @Test
    fun `der Startscreen laesst sich nicht loeschen`() {
        assertEquals(base, ScreenEdits.delete(base, Defaults.MAIN_ID))
    }

    @Test
    fun `der letzte Screen laesst sich nicht loeschen`() {
        val single = LauncherConfig(screens = listOf(Screen(id = "nur", name = "Nur")), homeScreenId = "anderer")
        assertEquals(single, ScreenEdits.delete(single, "nur"))
    }

    @Test
    fun `Loeschen entfernt Kacheln die dorthin sprangen`() {
        // Sonst bliebe eine Kachel stehen, die beim Antippen nichts tut.
        val withLink = base.copy(
            screens = listOf(
                home.copy(cells = home.cells + Cell(0, 0, button = Button(ButtonAction.GoToScreen("s2")))),
                Screen(id = "s2", name = "Zweiter"),
            ),
        )
        val after = ScreenEdits.delete(withLink, "s2")
        assertEquals(1, after.screens.size)
        assertTrue(after.screens.first().cells.none { it.button.action == ButtonAction.GoToScreen("s2") })
    }

    @Test
    fun `Loeschen laesst Kacheln zu anderen Screens in Ruhe`() {
        val config = base.copy(
            screens = listOf(
                home.copy(cells = listOf(Cell(0, 0, button = Button(ButtonAction.GoToScreen("s3"))))),
                Screen(id = "s2", name = "Zweiter"),
                Screen(id = "s3", name = "Dritter"),
            ),
        )
        val after = ScreenEdits.delete(config, "s2")
        assertTrue(after.screens.first().cells.any { it.button.action == ButtonAction.GoToScreen("s3") })
    }

    @Test
    fun `Loeschen raeumt auch die Wischreihenfolge auf`() {
        val config = base.copy(
            screens = listOf(home, Screen(id = "s2", name = "Zweiter")),
            swipeOrder = listOf(Defaults.MAIN_ID, "s2"),
        )
        assertEquals(listOf(Defaults.MAIN_ID), ScreenEdits.delete(config, "s2").swipeOrder)
    }

    @Test
    fun `nach dem Loeschen zeigt keine Kachel mehr ins Leere`() {
        val config = base.copy(
            screens = listOf(
                home.copy(cells = listOf(Cell(0, 0, button = Button(ButtonAction.GoToScreen("s2"))))),
                Screen(id = "s2", name = "Zweiter"),
            ),
        )
        assertTrue(ScreenEdits.danglingReferences(ScreenEdits.delete(config, "s2")).isEmpty())
    }

    @Test
    fun `eine Kachel ins Leere wird als solche erkannt`() {
        val broken = base.copy(
            screens = listOf(home.copy(cells = listOf(Cell(0, 0, button = Button(ButtonAction.GoToScreen("weg")))))),
        )
        assertEquals(listOf("weg"), ScreenEdits.danglingReferences(broken))
    }
}
