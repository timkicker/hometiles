package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * creating, renaming and deleting screens.
 *
 * two things are delicate and therefore solved here rather than in the surface: a new screen
 * without a way back would be a dead end, because a launcher may not swallow the back
 * gesture; and a deleted screen leaves tiles pointing at nothing.
 */
object ScreenEdits {

    /** a home tile on the last slot, or one would never get away from the new screen. */
    fun newScreen(id: String, name: String, like: Screen): Screen {
        val lastX = like.cols - 1
        val lastY = like.rows - 1
        return Screen(
            id = id,
            name = name,
            cols = like.cols,
            rows = like.rows,
            cells = listOf(
                Cell(
                    x = lastX,
                    y = lastY,
                    button = Button(action = ButtonAction.Action(Builtin.HOME_SCREEN)),
                ),
            ),
        )
    }

    /** grids that work on three inches; past three columns a tile turns into a postage stamp. */
    val GRID_PRESETS: List<Pair<Int, Int>> = listOf(
        1 to 2,
        2 to 2,
        2 to 3,
        2 to 4,
        3 to 4,
        3 to 5,
    )

    /** asked before changing the grid: losing four tiles silently would be irreversible. */
    fun dropped(screen: Screen, cols: Int, rows: Int): List<Cell> =
        screen.cells.filter { it.x >= cols || it.y >= rows }

    fun setGrid(config: LauncherConfig, id: String, cols: Int, rows: Int): LauncherConfig {
        if (cols < 1 || rows < 1) return config
        return config.copy(
            screens = config.screens.map { screen ->
                if (screen.id != id) {
                    screen
                } else {
                    // this arithmetic stood here a second time, line for line as in
                    // CellLayout.fitToGrid, and the tested copy was the one nobody called.
                    CellLayout.fitToGrid(screen.copy(cols = cols, rows = rows))
                }
            },
        )
    }

    fun freeId(config: LauncherConfig, base: String = "screen"): String {
        var index = config.screens.size + 1
        while (config.screens.any { it.id == "$base$index" }) index++
        return "$base$index"
    }

    fun add(config: LauncherConfig, screen: Screen): LauncherConfig =
        if (config.screens.any { it.id == screen.id }) config
        else config.copy(screens = config.screens + screen)

    fun rename(config: LauncherConfig, id: String, name: String): LauncherConfig {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return config
        return config.copy(
            screens = config.screens.map { if (it.id == id) it.copy(name = trimmed) else it },
        )
    }

    /**
     * what a deletion costs: the screen's filled tiles and the folders going with it.
     *
     * the number is larger than what one sees on the screen, because a folder's contents are
     * folded away. that is exactly when it has to be stated.
     */
    fun deletionLosses(config: LauncherConfig, id: String): Pair<Int, Int> {
        val screen = config.screens.firstOrNull { it.id == id } ?: return 0 to 0
        val tiles = screen.tileCount
        val folders = screen.cells
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .distinct()
            .count { folderId ->
                // only those nothing else points at.
                config.screens.flatMap { it.cells }.count { cell ->
                    (cell.button.action as? ButtonAction.Folder)?.screenId == folderId
                } == 1
            }
        return tiles to folders
    }

    /**
     * deletes a screen and tidies up after it: tiles jumping there go, and the swipe order
     * loses its entry. the home screen cannot be deleted, or the launcher would have no home.
     */
    fun delete(config: LauncherConfig, id: String): LauncherConfig {
        if (id == config.homeScreenId) return config
        if (config.screens.none { it.id == id }) return config
        if (config.screens.size <= 1) return config

        val remaining = config.screens
            .filter { it.id != id }
            .map { screen ->
                screen.copy(cells = screen.cells.filterNot { it.button.action == ButtonAction.GoToScreen(id) })
            }
        val withoutScreen = config.copy(
            screens = remaining,
            swipeOrder = config.swipeOrder.filterNot { it == id },
        )
        // a folder tile on the deleted screen used to leave its folder behind: unreachable,
        // in no list, impossible to delete. only the ones from here, though - a folder
        // already orphaned has nothing to do with this deletion.
        val foldersFromHere = config.screens.first { it.id == id }.cells
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .toSet()
        return FolderEdits.orphaned(withoutScreen)
            .filter { it.id in foldersFromHere }
            .fold(withoutScreen) { state, folder -> FolderEdits.delete(state, folder.id) }
    }

    private fun hasPagingTile(config: LauncherConfig): Boolean = config.screens
        .flatMap { it.cells }
        .any {
            (it.button.action as? ButtonAction.Action)?.builtin in
                setOf(Builtin.NEXT_SCREEN, Builtin.PREV_SCREEN)
        }

    /**
     * screens no tile leads to. usually created, then the jump tile was reassigned, and since
     * then it lives only in the config. the home screen never counts; back always leads there.
     */
    fun unreachable(config: LauncherConfig): List<Screen> {
        val reached = config.screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.GoToScreen)?.screenId }
            .toSet() +
            // swiping counts when it is on, and so do the paging tiles: `NEXT_SCREEN` and
            // `PREV_SCREEN` run through the same row whether swiping is on or not. warning
            // about a screen one reaches with a flick makes the warning worthless where it
            // does apply.
            if (config.behaviour.swipeBetweenScreens || hasPagingTile(config)) {
                ScreenOrder.ordered(config).map { it.id }.toSet()
            } else {
                emptySet()
            }
        // folders are not meant here: a folder tile leads to them, and whether one is
        // missing is FolderEdits.orphaned's question.
        return config.screens.filter {
            it.id != config.homeScreenId && it.id !in reached && !it.isFolder
        }
    }

    /**
     * are the settings reachable from this screen?
     *
     * the worst dead end in the whole app: making a screen the home screen when no settings
     * tile lies on it means never getting back into the settings, and only another launcher
     * or a computer with adb helps.
     *
     * counted across jump and folder tiles, because a way round two corners is still a way.
     * the app list counts too, since it offers the settings at its end.
     */
    fun settingsReachable(config: LauncherConfig, fromScreenId: String): Boolean {
        val seen = mutableSetOf<String>()
        val open = ArrayDeque(listOf(fromScreenId))
        while (open.isNotEmpty()) {
            val id = open.removeFirst()
            if (!seen.add(id)) continue
            val screen = config.screens.firstOrNull { it.id == id } ?: continue
            screen.cells.forEach { cell ->
                when (val action = cell.button.action) {
                    // whoever removes the settings row from the app list has to remove
                    // APP_LIST here; `SettingsReachableFromDrawerTest` holds the other side.
                    is ButtonAction.Action ->
                        if (action.builtin == Builtin.SETTINGS || action.builtin == Builtin.APP_LIST) return true
                    is ButtonAction.GoToScreen -> open.addLast(action.screenId)
                    is ButtonAction.Folder -> open.addLast(action.screenId)
                    else -> Unit
                }
            }
        }
        return false
    }

    /**
     * the way out of that dead end. `null` when no slot is free; the switch must then not
     * happen, because afterwards there would be no way back.
     */
    fun withSettingsTile(config: LauncherConfig, screenId: String): LauncherConfig? {
        val screen = config.screens.firstOrNull { it.id == screenId } ?: return null
        val slot = screen.freeSlots().firstOrNull() ?: return null
        return config.copy(
            screens = config.screens.map {
                if (it.id != screenId) {
                    it
                } else {
                    it.copy(
                        cells = it.cells + Cell(
                            x = slot.first,
                            y = slot.second,
                            button = Button(action = ButtonAction.Action(Builtin.SETTINGS)),
                        ),
                    )
                }
            },
        )
    }

    /**
     * puts a jump tile to [targetId] on the **home screen**, where it is certainly reachable.
     *
     * the counterpart to the warning "no tile leads to Screen 2", which used to say what to
     * do and leave the user to it. same rule as elsewhere: **the way there instead of
     * directions.** `null` when the home screen is full.
     */
    fun withJumpTile(config: LauncherConfig, targetId: String): LauncherConfig? {
        if (config.screens.none { it.id == targetId }) return null
        val home = config.screens.firstOrNull { it.id == config.homeScreenId } ?: return null
        val slot = home.freeSlots().firstOrNull() ?: return null
        return config.copy(
            screens = config.screens.map {
                if (it.id != home.id) {
                    it
                } else {
                    it.copy(
                        cells = it.cells + Cell(
                            x = slot.first,
                            y = slot.second,
                            button = Button(action = ButtonAction.GoToScreen(targetId)),
                        ),
                    )
                }
            },
        )
    }
}
