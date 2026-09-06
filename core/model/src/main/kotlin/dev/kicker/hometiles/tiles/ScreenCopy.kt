package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen

/**
 * duplicating a screen. `PLAN.md` 4.1.
 *
 * meant for trying things out: build a new arrangement without losing the old one. the
 * important part is therefore not the copying but what is **not** copied, and that one is
 * told about it.
 */
object ScreenCopy {

    sealed interface Result {
        data class Done(
            val config: LauncherConfig,
            val newId: String,
            val copied: Int,
            val skippedWidgets: Int,
            val skippedFolders: Int,
        ) : Result

        /** no free cell on the original for the tile that leads to the copy. */
        data object NoRoomForJumpTile : Result

        data object NoSuchScreen : Result
    }

    /**
     * widgets carry an id the widget host handed out exactly once; the same id twice would
     * mean deleting one tile breaks the other. folders belong to exactly one tile, and
     * deleting that tile clears both, so a second tile would point at nothing.
     */
    private fun copyable(action: ButtonAction): Boolean =
        action !is ButtonAction.Widget && action !is ButtonAction.Folder

    fun duplicate(config: LauncherConfig, id: String, name: String): Result {
        val original = config.screens.firstOrNull { it.id == id } ?: return Result.NoSuchScreen
        // the way there before the copy: a screen no tile leads to is set up and unfindable.
        val slot = original.freeSlots().firstOrNull() ?: return Result.NoRoomForJumpTile

        val newId = ScreenEdits.freeId(config)
        val kept = original.cells.filter { copyable(it.button.action) }
        val copy = Screen(
            id = newId,
            name = name,
            cols = original.cols,
            rows = original.rows,
            cells = kept,
        )
        val withCopy = config.copy(
            screens = config.screens.map { screen ->
                if (screen.id != id) {
                    screen
                } else {
                    screen.copy(
                        cells = screen.cells + Cell(
                            x = slot.first,
                            y = slot.second,
                            button = Button(action = ButtonAction.GoToScreen(newId)),
                        ),
                    )
                }
            } + copy,
        )
        return Result.Done(
            config = withCopy,
            newId = newId,
            copied = kept.count { it.button.action != ButtonAction.None },
            skippedWidgets = original.cells.count { it.button.action is ButtonAction.Widget },
            skippedFolders = original.cells.count { it.button.action is ButtonAction.Folder },
        )
    }
}
