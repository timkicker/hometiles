package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * changing a screen's grid.
 *
 * the dangerous case is shrinking: tiles outside the new edge are gone, and for good. so how
 * many it hits has to be knowable beforehand.
 */
class ScreenGridTest {

    private fun screen(cols: Int, rows: Int, vararg cells: Cell) =
        Screen(id = "s", name = "S", cols = cols, rows = rows, cells = cells.toList())

    private fun config(screen: Screen) = LauncherConfig(screens = listOf(screen))

    @Test
    fun `a larger grid loses nothing`() {
        val before = screen(2, 3, Cell(0, 0), Cell(1, 2))
        val after = ScreenEdits.setGrid(config(before), "s", 3, 5).screens.first()
        assertEquals(3, after.cols)
        assertEquals(5, after.rows)
        assertEquals(2, after.cells.size)
    }

    @Test
    fun `a smaller grid names the losses beforehand`() {
        val before = screen(3, 4, Cell(0, 0), Cell(2, 0), Cell(0, 3))
        val lost = ScreenEdits.dropped(before, cols = 2, rows = 2)
        assertEquals(2, lost.size)
        assertTrue(lost.any { it.x == 2 && it.y == 0 })
        assertTrue(lost.any { it.x == 0 && it.y == 3 })
    }

    @Test
    fun `a smaller grid throws away exactly the named ones`() {
        val before = screen(3, 4, Cell(0, 0), Cell(2, 0), Cell(0, 3))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 2).screens.first()
        assertEquals(1, after.cells.size)
        assertEquals(0, after.cells.first().x)
        assertEquals(0, after.cells.first().y)
    }

    @Test
    fun `a wide tile is trimmed instead of sticking out`() {
        // a 2x1 tile at x=1 needs columns 1 and 2. with two columns only one is left - it
        // shrinks rather than standing over the edge.
        val before = screen(3, 3, Cell(x = 1, y = 0, w = 2, h = 1))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 3).screens.first()
        assertEquals(1, after.cells.first().w)
    }

    @Test
    fun `a tall tile is trimmed likewise`() {
        val before = screen(2, 4, Cell(x = 0, y = 1, w = 1, h = 3))
        val after = ScreenEdits.setGrid(config(before), "s", 2, 2).screens.first()
        assertEquals(1, after.cells.first().h)
    }

    @Test
    fun `nonsensical grids change nothing`() {
        val before = screen(2, 3, Cell(0, 0))
        assertEquals(before, ScreenEdits.setGrid(config(before), "s", 0, 3).screens.first())
        assertEquals(before, ScreenEdits.setGrid(config(before), "s", 2, -1).screens.first())
    }

    @Test
    fun `another screen stays untouched`() {
        val a = screen(2, 3, Cell(0, 0))
        val b = Screen(id = "b", name = "B", cols = 3, rows = 5, cells = listOf(Cell(2, 4)))
        val after = ScreenEdits.setGrid(LauncherConfig(screens = listOf(a, b)), "s", 1, 2)
        assertEquals(3, after.screens[1].cols)
        assertEquals(1, after.screens[1].cells.size)
    }

    @Test
    fun `every offered grid fits on three inches`() {
        // no preset above three columns: beyond that a tile on 349 dp of width gets narrower
        // than a thumb.
        assertTrue(ScreenEdits.GRID_PRESETS.all { (cols, _) -> cols in 1..3 })
        assertTrue(ScreenEdits.GRID_PRESETS.all { (_, rows) -> rows in 2..5 })
    }
}

/**
 * screens no way leads to.
 *
 * the counterpart to a tile pointing at nothing. a screen without a jump tile stands only in
 * the configuration - it was set up and can never be reached again. the same family as the
 * hidden app without a way back, only the other way round.
 */
class ScreenReachabilityTest {

    private fun jump(x: Int, y: Int, to: String) =
        Cell(x, y, button = Button(action = ButtonAction.GoToScreen(to)))

    private fun screen(id: String, vararg cells: Cell) =
        Screen(id = id, name = id, cols = 2, rows = 3, cells = cells.toList())

    @Test
    fun `the home screen never counts as unreachable`() {
        val config = LauncherConfig(screens = listOf(screen("home")), homeScreenId = "home")
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `a screen without a jump tile is reported`() {
        val config = LauncherConfig(
            screens = listOf(screen("home"), screen("screen2")),
            homeScreenId = "home",
        )
        assertEquals(listOf("screen2"), ScreenEdits.unreachable(config).map { it.id })
    }

    @Test
    fun `one jump tile is enough`() {
        val config = LauncherConfig(
            screens = listOf(screen("home", jump(0, 0, "screen2")), screen("screen2")),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `the jump tile may lie on a third screen`() {
        // a chain: home -> two -> three. three is reachable as well, if only through two.
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
    fun `a ring with no link to home still does not stand out`() {
        // held on purpose: two screens pointing only at each other count as reachable. the
        // check looks at tiles, not at ways from home - a real reachability analysis would
        // promise more than it can keep, since a jump tile can sit on a screen one has to
        // find first.
        val config = LauncherConfig(
            screens = listOf(screen("home"), screen("a", jump(0, 0, "b")), screen("b", jump(0, 0, "a"))),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }
}

/**
 * folders must not trigger the warning about unreachable screens.
 *
 * a folder is reached by a folder tile, not a jump tile. without this exception every folder
 * created would have raised a red warning at once - and a warning that always comes is not
 * read after the third time.
 */
class FolderReachabilityTest {

    @Test
    fun `a folder does not count as an unreachable screen`() {
        val folder = Screen(id = "f1", name = "Bank", kind = ScreenKind.FOLDER)
        val home = Screen(
            id = "home",
            name = "Start",
            cells = listOf(Cell(0, 0, button = Button(action = ButtonAction.Folder("f1")))),
        )
        val config = LauncherConfig(screens = listOf(home, folder), homeScreenId = "home")
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }

    @Test
    fun `an ordinary screen without a jump tile still does`() {
        val second = Screen(id = "s2", name = "Zwei")
        val home = Screen(id = "home", name = "Start")
        val config = LauncherConfig(screens = listOf(home, second), homeScreenId = "home")
        assertEquals(listOf("s2"), ScreenEdits.unreachable(config).map { it.id })
    }
}
