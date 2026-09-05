package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * folders no tile points at any more.
 *
 * `ScreenEdits.unreachable` leaves folders out on purpose and points in its comment at
 * `FolderEdits.orphaned` - which **nobody** called until now. at the same time folders do not
 * stand in the screen list, since a folder belongs to its tile. a folder without a tile was
 * therefore neither to be opened nor seen nor deleted - while its tiles still counted when
 * resetting and travelled into every backup.
 */
class OrphanedFoldersTest {

    private fun cell(x: Int, y: Int, action: ButtonAction) =
        Cell(x = x, y = y, button = Button(action = action))

    /** home screen with a folder tile, a second screen, and the folder itself. */
    private fun withFolder(folderOn: String): LauncherConfig {
        val base = LauncherConfig()
        val home = base.screens.first().copy(
            id = "home",
            cells = listOf(cell(0, 0, ButtonAction.GoToScreen("s2"))) +
                if (folderOn == "home") listOf(cell(1, 0, ButtonAction.Folder("f1"))) else emptyList(),
        )
        val second = home.copy(
            id = "s2",
            name = "Screen 2",
            cells = if (folderOn == "s2") listOf(cell(0, 0, ButtonAction.Folder("f1"))) else emptyList(),
        )
        val folder = home.copy(
            id = "f1",
            name = "Mehr",
            kind = ScreenKind.FOLDER,
            cells = listOf(cell(0, 0, ButtonAction.GoToScreen("home"))),
        )
        return base.copy(screens = listOf(home, second, folder), homeScreenId = "home")
    }

    private fun folderIn(config: LauncherConfig): Screen? =
        config.screens.firstOrNull { it.id == "f1" }

    @Test
    fun `a folder without a tile counts as orphaned`() {
        val config = withFolder("home")
        assertEquals(emptyList<Screen>(), FolderEdits.orphaned(config))

        val withoutTile = config.copy(
            screens = config.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { c -> c.button.action is ButtonAction.Folder }) else it
            },
        )
        assertEquals(listOf("f1"), FolderEdits.orphaned(withoutTile).map { it.id })
    }

    @Test
    fun `the screen check does not report folders - orphaned is there for that`() {
        // held so the division of labour does not tip unnoticed: one check is for screens,
        // the other for folders.
        val withoutTile = withFolder("home").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { x -> x.button.action is ButtonAction.Folder }) else it
            })
        }
        assertTrue("f1" !in ScreenEdits.unreachable(withoutTile).map { it.id })
        assertEquals(listOf("f1"), FolderEdits.orphaned(withoutTile).map { it.id })
    }

    @Test
    fun `deleting a screen takes its folders with it`() {
        // the folder lies on "Screen 2". with that deleted, the folder was until now trapped
        // in the configuration forever.
        val after = ScreenEdits.delete(withFolder("s2"), "s2")
        assertNull("the folder has to be gone too", folderIn(after))
        assertEquals(emptyList<Screen>(), FolderEdits.orphaned(after))
    }

    @Test
    fun `a folder on another screen stays`() {
        // "Screen 2" is deleted while the folder hangs on the home screen - it is none of
        // its business.
        val after = ScreenEdits.delete(withFolder("home"), "s2")
        assertEquals("Mehr", folderIn(after)?.name)
    }

    @Test
    fun `the question counts tiles and folders`() {
        // the folder's contents are folded away - what it costs has to stand there before
        // anyone taps. "Screen 2" carries exactly one tile, and it is a folder.
        assertEquals(1 to 1, ScreenEdits.deletionLosses(withFolder("s2"), "s2"))
    }

    @Test
    fun `a folder another tile still points at does not count`() {
        // two tiles on the same folder: deleting one screen leaves it standing, so the
        // question must not name it as a loss either.
        val config = withFolder("s2").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells + cell(1, 1, ButtonAction.Folder("f1"))) else it
            })
        }
        assertEquals(1 to 0, ScreenEdits.deletionLosses(config, "s2"))
        assertEquals("Mehr", folderIn(ScreenEdits.delete(config, "s2"))?.name)
    }

    @Test
    fun `an empty screen costs nothing`() {
        // "Screen 2" is empty in this arrangement, the folder hangs on the home screen.
        assertEquals(0 to 0, ScreenEdits.deletionLosses(withFolder("home"), "s2"))
    }

    @Test
    fun `the home screen reports tiles and its folder`() {
        // two filled tiles, one of them the folder.
        assertEquals(2 to 1, ScreenEdits.deletionLosses(withFolder("home"), "home"))
    }

    @Test
    fun `a screen that does not exist costs nothing`() {
        assertEquals(0 to 0, ScreenEdits.deletionLosses(withFolder("home"), "gibtsnicht"))
    }

    @Test
    fun `a folder already orphaned before does not vanish along the way`() {
        // otherwise deleting an unrelated screen would quietly clear away content nobody was
        // talking about.
        val before = withFolder("home").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { x -> x.button.action is ButtonAction.Folder }) else it
            })
        }
        val after = ScreenEdits.delete(before, "s2")
        assertEquals("Mehr", folderIn(after)?.name)
    }
}
