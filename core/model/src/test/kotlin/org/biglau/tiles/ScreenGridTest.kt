package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das Raster eines Screens umstellen.
 *
 * Der gefaehrliche Fall ist das Verkleinern: Kacheln ausserhalb des neuen Randes sind weg,
 * und zwar endgueltig. Deshalb muss vorher zu erfahren sein, wie viele es trifft.
 */
class ScreenGridTest {

    private fun screen(cols: Int, rows: Int, vararg cells: Cell) =
        Screen(id = "s", name = "S", cols = cols, rows = rows, cells = cells.toList())

    private fun config(screen: Screen) = LauncherConfig(screens = listOf(screen))

    @Test
    fun `groesseres Raster verliert nichts`() {
        val before = screen(2, 3, Cell(0, 0), Cell(1, 2))
        val after = ScreenEdits.setGrid(config(before), "s", 3, 5).screens.first()
        assertEquals(3, after.cols)
        assertEquals(5, after.rows)
        assertEquals(2, after.cells.size)
    }

    @Test
    fun `kleineres Raster nennt vorher die Verluste`() {
        val before = screen(3, 4, Cell(0, 0), Cell(2, 0), Cell(0, 3))
        val verloren = ScreenEdits.dropped(before, cols = 2, rows = 2)
        assertEquals(2, verloren.size)
        assertTrue(verloren.any { it.x == 2 && it.y == 0 })
        assertTrue(verloren.any { it.x == 0 && it.y == 3 })
    }

    @Test
    fun `kleineres Raster wirft genau die genannten weg`() {
        val before = screen(3, 4, Cell(0, 0), Cell(2, 0), Cell(0, 3))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 2).screens.first()
        assertEquals(1, after.cells.size)
        assertEquals(0, after.cells.first().x)
        assertEquals(0, after.cells.first().y)
    }

    @Test
    fun `eine breite Kachel wird beschnitten statt hinauszuragen`() {
        // Eine 2x1-Kachel bei x=1 braucht die Spalten 1 und 2. Bei zwei Spalten bleibt
        // nur noch eine - sie schrumpft, statt ueber den Rand zu stehen.
        val before = screen(3, 3, Cell(x = 1, y = 0, w = 2, h = 1))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 3).screens.first()
        assertEquals(1, after.cells.first().w)
    }

    @Test
    fun `eine hohe Kachel wird ebenso beschnitten`() {
        val before = screen(2, 4, Cell(x = 0, y = 1, w = 1, h = 3))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 2).screens.first()
        assertEquals(1, after.cells.first().h)
    }

    @Test
    fun `unsinnige Raster aendern nichts`() {
        val before = screen(2, 3, Cell(0, 0))
        assertEquals(before, ScreenEdits.setGrid(config(before), "s", 0, 3).screens.first())
        assertEquals(before, ScreenEdits.setGrid(config(before), "s", 2, -1).screens.first())
    }

    @Test
    fun `ein anderer Screen bleibt unberuehrt`() {
        val a = screen(2, 3, Cell(0, 0))
        val b = Screen(id = "b", name = "B", cols = 3, rows = 5, cells = listOf(Cell(2, 4)))
        val after = ScreenEdits.setGrid(LauncherConfig(screens = listOf(a, b)), "s", 1, 2)
        assertEquals(3, after.screens[1].cols)
        assertEquals(1, after.screens[1].cells.size)
    }

    @Test
    fun `jedes angebotene Raster passt auf drei Zoll`() {
        // Kein Preset ueber drei Spalten: darueber wird eine Kachel auf 349 dp Breite
        // schmaler als ein Daumen.
        assertTrue(ScreenEdits.GRID_PRESETS.all { (cols, _) -> cols in 1..3 })
        assertTrue(ScreenEdits.GRID_PRESETS.all { (_, rows) -> rows in 2..5 })
    }
}

/**
 * Screens, zu denen kein Weg führt.
 *
 * Das Gegenstück zur Kachel, die ins Leere zeigt. Ein Screen ohne Sprungkachel steht nur
 * noch in der Konfiguration - man hat ihn eingerichtet und kommt nie wieder hin. Dieselbe
 * Familie wie die ausgeblendete App ohne Weg zurück, nur andersherum.
 */
class ScreenReachabilityTest {

    private fun jump(x: Int, y: Int, to: String) =
        Cell(x, y, button = Button(action = ButtonAction.GoToScreen(to)))

    private fun screen(id: String, vararg cells: Cell) =
        Screen(id = id, name = id, cols = 2, rows = 3, cells = cells.toList())

    @Test
    fun `der Startbildschirm gilt nie als unerreichbar`() {
        val config = LauncherConfig(screens = listOf(screen("home")), homeScreenId = "home")
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `ein Screen ohne Sprungkachel wird gemeldet`() {
        val config = LauncherConfig(
            screens = listOf(screen("home"), screen("screen2")),
            homeScreenId = "home",
        )
        assertEquals(listOf("screen2"), ScreenEdits.unreachable(config).map { it.id })
    }

    @Test
    fun `eine Sprungkachel genuegt`() {
        val config = LauncherConfig(
            screens = listOf(screen("home", jump(0, 0, "screen2")), screen("screen2")),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `die Sprungkachel darf auf einem dritten Screen liegen`() {
        // Kette: home -> zwei -> drei. Auch drei ist erreichbar, wenn auch nur ueber zwei.
        val config = LauncherConfig(
            screens = listOf(
                screen("home", jump(0, 0, "zwei")),
                screen("zwei", jump(0, 0, "drei")),
                screen("drei"),
            ),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `ein Ring ohne Anschluss an den Start faellt trotzdem nicht auf`() {
        // Bewusst festgehalten: zwei Screens, die nur aufeinander zeigen, gelten als
        // erreichbar. Die Pruefung schaut auf Kacheln, nicht auf Wege vom Start aus -
        // eine echte Erreichbarkeitsanalyse waere mehr Versprechen, als sie halten kann,
        // denn eine Sprungkachel kann auch auf einem Screen liegen, den man erst sucht.
        val config = LauncherConfig(
            screens = listOf(screen("home"), screen("a", jump(0, 0, "b")), screen("b", jump(0, 0, "a"))),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }
}

/**
 * Ordner dürfen die Warnung vor unerreichbaren Screens nicht auslösen.
 *
 * Zu einem Ordner führt eine Ordnerkachel, keine Sprungkachel. Ohne diese Ausnahme hätte
 * jeder angelegte Ordner sofort eine rote Warnung erzeugt - und eine Warnung, die immer
 * kommt, liest nach der dritten niemand mehr.
 */
class FolderReachabilityTest {

    @Test
    fun `ein Ordner gilt nicht als unerreichbarer Screen`() {
        val ordner = Screen(id = "f1", name = "Bank", kind = ScreenKind.FOLDER)
        val heim = Screen(
            id = "home",
            name = "Start",
            cells = listOf(Cell(0, 0, button = Button(action = ButtonAction.Folder("f1")))),
        )
        val config = LauncherConfig(screens = listOf(heim, ordner), homeScreenId = "home")
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `ein gewoehnlicher Screen ohne Sprungkachel weiterhin schon`() {
        val zweiter = Screen(id = "s2", name = "Zwei")
        val heim = Screen(id = "home", name = "Start")
        val config = LauncherConfig(screens = listOf(heim, zweiter), homeScreenId = "home")
        assertEquals(listOf("s2"), ScreenEdits.unreachable(config).map { it.id })
    }
}
