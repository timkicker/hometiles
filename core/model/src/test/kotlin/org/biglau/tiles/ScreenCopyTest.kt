package org.biglau.tiles

import org.biglau.data.Builtin
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
 * PLAN.md 4.1, duplicating a screen.
 *
 * meant for trying things out: build a new arrangement without losing the old one. the most
 * important part is therefore not what is copied but what is not.
 */
class ScreenCopyTest {

    private fun tile(action: ButtonAction) = Button(action = action)

    private val full = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 2,
        cells = listOf(
            Cell(0, 0, button = tile(ButtonAction.App("a", "b"))),
            Cell(1, 0, button = tile(ButtonAction.Action(Builtin.SETTINGS))),
            Cell(0, 1, button = tile(ButtonAction.Widget("p/W", 7, "Uhr"))),
        ),
    )

    private val config = LauncherConfig(screens = listOf(full), homeScreenId = "home")

    @Test
    fun `the copy has the same grid and a new name`() {
        val result = ScreenCopy.duplicate(config, "home", "Start (Kopie)")
        val done = result as ScreenCopy.Result.Done
        val copy = done.config.screenById(done.newId)!!
        assertEquals("Start (Kopie)", copy.name)
        assertEquals(2, copy.cols)
        assertEquals(2, copy.rows)
    }

    /**
     * widgets are not copied along. a widget tile holds an id the AppWidgetHost handed out
     * exactly once; the same one twice would mean deleting one tile breaks the other.
     */
    @Test
    fun `widgets stay behind and are counted`() {
        val done = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        val copy = done.config.screenById(done.newId)!!
        assertEquals(false, copy.cells.any { it.button.action is ButtonAction.Widget })
        assertEquals(1, done.skippedWidgets)
        assertEquals(2, done.copied)
    }

    // exactly one tile belongs to a folder; deleting it clears both away together. two tiles
    // on the same folder would leave one pointing at nothing after the deletion.
    @Test
    fun `folder tiles stay behind`() {
        val withFolder = config.copy(
            screens = listOf(
                full.copy(cells = listOf(Cell(0, 0, button = tile(ButtonAction.Folder("f"))))),
                Screen("f", "Mehr", 2, 2, kind = ScreenKind.FOLDER),
            ),
        )
        val done = ScreenCopy.duplicate(withFolder, "home", "K") as ScreenCopy.Result.Done
        val copy = done.config.screenById(done.newId)!!
        assertEquals(false, copy.cells.any { it.button.action is ButtonAction.Folder })
        assertEquals(1, done.skippedFolders)
    }

    /**
     * the most important part: the copy is reachable. a screen no tile leads to is set up
     * and unfindable - exactly the state the screens page warns about.
     */
    @Test
    fun `a tile leads to the copy`() {
        val done = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        val targets = done.config.screenById("home")!!.cells
            .mapNotNull { (it.button.action as? ButtonAction.GoToScreen)?.screenId }
        assertTrue(targets.contains(done.newId))
        assertEquals(emptyList<Screen>(), ScreenEdits.unreachable(done.config))
    }

    // better not to duplicate at all than to leave an unfindable copy behind.
    @Test
    fun `without a free cell nothing is duplicated`() {
        val brimFull = config.copy(
            screens = listOf(
                full.copy(
                    cols = 1,
                    rows = 1,
                    cells = listOf(Cell(0, 0, button = tile(ButtonAction.App("a", "b")))),
                ),
            ),
        )
        assertEquals(
            ScreenCopy.Result.NoRoomForJumpTile,
            ScreenCopy.duplicate(brimFull, "home", "K"),
        )
    }

    @Test
    fun `an unknown screen gives nothing`() {
        assertEquals(ScreenCopy.Result.NoSuchScreen, ScreenCopy.duplicate(config, "weg", "K"))
    }

    // the id has to be new, otherwise the copy overwrites the original.
    @Test
    fun `the copy gets an id of its own`() {
        val done = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        assertEquals(2, done.config.screens.size)
        assertTrue(done.newId != "home")
    }
}
