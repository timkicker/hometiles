package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind

/**
 * creating, filling and getting rid of folders.
 *
 * a folder is a [Screen] with [ScreenKind.FOLDER]; the tile that opens it carries
 * [ButtonAction.Folder]. the two belong together and must never drift apart: a folder
 * without a tile is exactly the unreachable screen the settings warn about, and a tile
 * without a folder points at nothing.
 */
object FolderEdits {

    /** how many icons a folder tile shows before it turns illegible. */
    const val PREVIEW_COUNT = 4

    /** deliberately **without** a home tile, unlike a new screen: a folder closes with back. */
    fun newFolder(id: String, name: String, like: Screen): Screen = Screen(
        id = id,
        name = name,
        cols = like.cols,
        rows = like.rows,
        cells = emptyList(),
        kind = ScreenKind.FOLDER,
    )

    fun plainScreens(config: LauncherConfig): List<Screen> = config.screens.filterNot { it.isFolder }

    fun folders(config: LauncherConfig): List<Screen> = config.screens.filter { it.isFolder }

    fun folderFor(config: LauncherConfig, action: ButtonAction.Folder): Screen? =
        config.screens.firstOrNull { it.id == action.screenId && it.isFolder }

    fun preview(folder: Screen): List<Cell> =
        folder.cells
            .sortedWith(compareBy({ it.y }, { it.x }))
            .take(PREVIEW_COUNT)

    /** not inside a folder: two levels lose the overview large tiles are meant to give. */
    fun mayContainFolder(screen: Screen): Boolean = !screen.isFolder

    /** the folder and every tile pointing at it, together, because either one alone is broken. */
    fun delete(config: LauncherConfig, folderId: String): LauncherConfig {
        val folder = config.screens.firstOrNull { it.id == folderId && it.isFolder } ?: return config
        return config.copy(
            screens = config.screens
                .filterNot { it.id == folder.id }
                .map { screen ->
                    screen.copy(
                        cells = screen.cells.filterNot {
                            (it.button.action as? ButtonAction.Folder)?.screenId == folder.id
                        },
                    )
                },
        )
    }

    /** how many tiles a deletion costs; the confirmation says the number. */
    fun contentCount(config: LauncherConfig, folderId: String): Int =
        config.screens.firstOrNull { it.id == folderId && it.isFolder }?.cells?.size ?: 0

    /** folders no tile points at any more. nobody can reach those. */
    fun orphaned(config: LauncherConfig): List<Screen> {
        val used = config.screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .toSet()
        return folders(config).filterNot { it.id in used }
    }
}
