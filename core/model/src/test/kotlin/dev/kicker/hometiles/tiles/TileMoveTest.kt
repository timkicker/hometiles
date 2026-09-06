package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * moving tiles between screens and folders.
 *
 * the case to prevent: a tile that silently vanishes on the way because there was no room at
 * the target. better not to move at all and say so.
 */
class TileMoveTest {

    private fun tile(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    private val home = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 3,
        cells = listOf(tile(0, 0, Builtin.DIALER), tile(1, 0, Builtin.CAMERA)),
    )

    private val folder = Screen(
        id = "f1",
        name = "Bank",
        cols = 2,
        rows = 3,
        kind = ScreenKind.FOLDER,
        cells = listOf(tile(0, 0, Builtin.CLOCK)),
    )

    private val config = LauncherConfig(screens = listOf(home, folder), homeScreenId = "home")

    @Test
    fun `a tile moves into the folder`() {
        val after = TileMove.move(config, "home", 1, 0, "f1")!!
        assertEquals(1, after.screens.first { it.id == "home" }.cells.size)
        val contents = after.screens.first { it.id == "f1" }.cells
        assertEquals(2, contents.size)
        assertTrue(contents.any { (it.button.action as? ButtonAction.Action)?.builtin == Builtin.CAMERA })
    }

    @Test
    fun `it lands on the first free spot`() {
        // in the folder (0,0) is taken, so it goes to (1,0).
        val after = TileMove.move(config, "home", 1, 0, "f1")!!
        val moved = after.screens.first { it.id == "f1" }.cells
            .first { (it.button.action as? ButtonAction.Action)?.builtin == Builtin.CAMERA }
        assertEquals(1, moved.x)
        assertEquals(0, moved.y)
    }

    @Test
    fun `and out again`() {
        val after = TileMove.move(config, "f1", 0, 0, "home")!!
        assertTrue(after.screens.first { it.id == "f1" }.cells.isEmpty())
        assertEquals(3, after.screens.first { it.id == "home" }.cells.size)
    }

    @Test
    fun `a wide tile arrives one field wide`() {
        // otherwise it stood over the edge at the target or covered a filled cell.
        val wide = home.copy(cells = listOf(Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER)))))
        val c = config.copy(screens = listOf(wide, folder))
        val moved = TileMove.move(c, "home", 0, 0, "f1")!!
            .screens.first { it.id == "f1" }.cells.first { it.x == 1 && it.y == 0 }
        assertEquals(1, moved.w)
        assertEquals(1, moved.h)
    }

    @Test
    fun `without room nothing moves`() {
        val full = folder.copy(
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> tile(x, y, Builtin.CLOCK) } },
        )
        assertNull(TileMove.move(config.copy(screens = listOf(home, full)), "home", 1, 0, "f1"))
    }

    @Test
    fun `a folder does not move into a folder`() {
        val withFolder = home.copy(
            cells = home.cells + Cell(0, 1, button = Button(action = ButtonAction.Folder("f1"))),
        )
        val c = config.copy(screens = listOf(withFolder, folder))
        assertNull(TileMove.move(c, "home", 0, 1, "f1"))
        assertTrue(TileMove.targetsFor(c, "home", withFolder.cells.last()).none { it.isFolder })
    }

    @Test
    fun `moving to the same screen moves nothing`() {
        assertNull(TileMove.move(config, "home", 1, 0, "home"))
    }

    @Test
    fun `an empty cell cannot be moved`() {
        assertNull(TileMove.move(config, "home", 1, 2, "f1"))
    }

    @Test
    fun `full targets are not offered at all`() {
        val full = folder.copy(
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> tile(x, y, Builtin.CLOCK) } },
        )
        val c = config.copy(screens = listOf(home, full))
        assertTrue(TileMove.targetsFor(c, "home", home.cells.first()).isEmpty())
    }

    // --- on the same screen: PLAN.md 4.1 "Kacheln tauschen", the plan's own german heading ---

    @Test
    fun `free spots and the neighbour are on offer`() {
        val spots = TileMove.spotsFor(home, home.cells.first())
        // 2x3 minus its own cell: four free spots and the neighbour to the right.
        assertEquals(5, spots.size)
        assertEquals(1, spots.count { it.occupant != null })
        assertTrue(spots.none { it.x == 0 && it.y == 0 })
    }

    @Test
    fun `move to a free spot`() {
        val moved = TileMove.moveWithin(config, "home", 0, 0, toX = 1, toY = 2)
        assertNotNull(moved)
        val screen = moved!!.screens.first { it.id == "home" }
        assertNull(screen.cellAt(0, 0))
        assertEquals(
            ButtonAction.Action(Builtin.DIALER),
            screen.cellAt(1, 2)?.button?.action,
        )
        assertEquals(2, screen.cells.size)
    }

    /** the core of the promise: the other tile stays, it only changes place. */
    @Test
    fun `two tiles swap`() {
        val moved = TileMove.moveWithin(config, "home", 0, 0, toX = 1, toY = 0)
        assertNotNull(moved)
        val screen = moved!!.screens.first { it.id == "home" }
        assertEquals(ButtonAction.Action(Builtin.CAMERA), screen.cellAt(0, 0)?.button?.action)
        assertEquals(ButtonAction.Action(Builtin.DIALER), screen.cellAt(1, 0)?.button?.action)
        assertEquals(2, screen.cells.size)
    }

    /** tiles of unequal size left a hole or a covering behind when swapped. */
    @Test
    fun `tiles of unequal size do not swap`() {
        val wide = Screen(
            id = "s", name = "S", cols = 2, rows = 3,
            cells = listOf(
                Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
                tile(0, 1, Builtin.CAMERA),
            ),
        )
        val c = config.copy(screens = listOf(wide))
        assertTrue(TileMove.spotsFor(wide, wide.cells.first()).none { it.occupant != null })
        assertNull(TileMove.moveWithin(c, "s", 0, 0, toX = 0, toY = 1))
    }

    /** a wide tile keeps its size - and goes only where it fits whole. */
    @Test
    fun `a wide tile does not fit everywhere`() {
        val wide = Screen(
            id = "s", name = "S", cols = 2, rows = 3,
            cells = listOf(
                Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
                tile(0, 1, Builtin.CAMERA),
            ),
        )
        val spots = TileMove.spotsFor(wide, wide.cells.first())
        // only the wholly free last row; the middle one is half taken.
        assertEquals(listOf(0 to 2), spots.map { it.x to it.y })
        val c = config.copy(screens = listOf(wide))
        assertNull(TileMove.moveWithin(c, "s", 0, 0, toX = 1, toY = 1))
        val moved = TileMove.moveWithin(c, "s", 0, 0, toX = 0, toY = 2)!!
        assertEquals(2, moved.screens.first().cellAt(0, 2)!!.w)
    }

    @Test
    fun `a spot outside the grid does not work`() {
        assertNull(TileMove.moveWithin(config, "home", 0, 0, toX = 5, toY = 5))
        assertNull(TileMove.moveWithin(config, "home", 0, 0, toX = 0, toY = 0))
    }

    @Test
    fun `from an empty spot nothing happens`() {
        assertNull(TileMove.moveWithin(config, "home", 0, 2, toX = 1, toY = 2))
    }
}
