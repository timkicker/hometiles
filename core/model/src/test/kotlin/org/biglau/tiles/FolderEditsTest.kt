package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * folders: create, look into, delete.
 *
 * deleting is the dangerous case. a folder and the tile that opens it belong together; if
 * one of the two stays behind, exactly what the settings otherwise warn about appears - a
 * screen no way leads to, or a tile pointing at nothing.
 */
class FolderEditsTest {

    private fun app(name: String) =
        Button(action = ButtonAction.App(name, "$name.Main"))

    private val folder = Screen(
        id = "f1",
        name = "Bank",
        cols = 2,
        rows = 3,
        kind = ScreenKind.FOLDER,
        cells = listOf(
            Cell(1, 1, button = app("b")),
            Cell(0, 0, button = app("a")),
            Cell(0, 2, button = app("e")),
            Cell(1, 0, button = app("c")),
            Cell(0, 1, button = app("d")),
        ),
    )

    private val home = Screen(
        id = "home",
        name = "Start",
        cells = listOf(
            Cell(0, 0, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
            Cell(1, 0, button = Button(action = ButtonAction.Folder("f1"))),
        ),
    )

    private val config = LauncherConfig(screens = listOf(home, folder), homeScreenId = "home")

    @Test
    fun `a new folder is empty and has no home tile`() {
        // unlike a new screen: the folder closes with the back gesture, it cannot become a
        // dead end. the name stays german because it is the interface's own wording.
        val fresh = FolderEdits.newFolder("f2", "Ämter", home)
        assertTrue(fresh.isFolder)
        assertTrue(fresh.cells.isEmpty())
        assertEquals(home.cols, fresh.cols)
        assertEquals(home.rows, fresh.rows)
    }

    @Test
    fun `the preview shows four tiles, read from the top left`() {
        val preview = FolderEdits.preview(folder)
        assertEquals(FolderEdits.PREVIEW_COUNT, preview.size)
        assertEquals(listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), preview.map { it.x to it.y })
    }

    @Test
    fun `an empty folder has an empty preview`() {
        assertTrue(FolderEdits.preview(FolderEdits.newFolder("f2", "Leer", home)).isEmpty())
    }

    @Test
    fun `the screen management does not show folders`() {
        assertEquals(listOf("home"), FolderEdits.plainScreens(config).map { it.id })
        assertEquals(listOf("f1"), FolderEdits.folders(config).map { it.id })
    }

    @Test
    fun `there is no folder inside a folder`() {
        assertTrue(FolderEdits.mayContainFolder(home))
        assertFalse(FolderEdits.mayContainFolder(folder))
    }

    @Test
    fun `deleting takes the folder and the tile with it`() {
        val after = FolderEdits.delete(config, "f1")
        assertNull(after.screens.firstOrNull { it.id == "f1" })
        val start = after.screens.first { it.id == "home" }
        assertEquals(1, start.cells.size)
        assertTrue(start.cells.single().button.action is ButtonAction.Action)
    }

    @Test
    fun `deleting names the number of tiles beforehand`() {
        assertEquals(5, FolderEdits.contentCount(config, "f1"))
        assertEquals(0, FolderEdits.contentCount(config, "gibtsnicht"))
    }

    @Test
    fun `it does not delete an ordinary screen`() {
        // delete is for folders only; a screen still goes through ScreenEdits with its own
        // checks.
        assertEquals(config, FolderEdits.delete(config, "home"))
    }

    @Test
    fun `a folder without a tile stands out`() {
        val withoutTile = config.copy(
            screens = listOf(home.copy(cells = home.cells.take(1)), folder),
        )
        assertEquals(listOf("f1"), FolderEdits.orphaned(withoutTile).map { it.id })
        assertTrue(FolderEdits.orphaned(config).isEmpty())
    }

    @Test
    fun `the tile finds its folder`() {
        assertEquals("f1", FolderEdits.folderFor(config, ButtonAction.Folder("f1"))?.id)
        assertNull(FolderEdits.folderFor(config, ButtonAction.Folder("gibtsnicht")))
        // an ordinary screen is no folder, even when a folder tile points at it.
        assertNull(FolderEdits.folderFor(config, ButtonAction.Folder("home")))
    }
}

/**
 * a folder has exactly one name.
 *
 * it stands on the tile and as the heading inside the opened folder. with a tile label
 * beside it the same thing would be called two different names, depending on whether one
 * stands in front of it or inside it.
 */
class FolderNameTest {

    private val folder = Screen(id = "f1", name = "Bank", kind = ScreenKind.FOLDER)
    private val home = Screen(
        id = "home",
        name = "Start",
        cells = listOf(Cell(0, 0, button = Button(action = ButtonAction.Folder("f1")))),
    )
    private val config = LauncherConfig(screens = listOf(home, folder), homeScreenId = "home")

    @Test
    fun `renaming changes the folder`() {
        // german test data: the check compares the name literally.
        val after = ScreenEdits.rename(config, "f1", "Ämter")
        assertEquals("Ämter", after.screens.first { it.id == "f1" }.name)
    }

    @Test
    fun `the tile stays untouched`() {
        // it carries no label of its own, it shows the folder's name.
        val after = ScreenEdits.rename(config, "f1", "Ämter")
        val tile = after.screens.first { it.id == "home" }.cells.single()
        assertNull(tile.button.label)
        assertEquals(ButtonAction.Folder("f1"), tile.button.action)
    }

    @Test
    fun `an empty name changes nothing`() {
        // otherwise nothing would stand on the tile any more, nor in the folder.
        assertEquals("Bank", ScreenEdits.rename(config, "f1", "   ").screens.first { it.id == "f1" }.name)
    }
}
