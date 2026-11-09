package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig

/**
 * where a shortcut pinned from another app lands: the first free spot of the home screen,
 * read like text. never over a tile that holds anything, not even a label or a colour, and
 * never on another screen, because a tile nobody finds is a tile that was not added.
 */
object PinPlacement {

    /** the spot, or null when the home screen is full. */
    fun spotFor(config: LauncherConfig): Pair<Int, Int>? {
        val screen = config.homeScreen
        for (y in 0 until screen.rows) {
            for (x in 0 until screen.cols) {
                val cell = screen.cellAt(x, y) ?: return x to y
                // a wider empty cell counts by its corner, or it would be offered twice.
                if (!TileEdits.clearable(cell.button) && cell.x == x && cell.y == y) return x to y
            }
        }
        return null
    }

    /** the config with the shortcut on its spot, or null when there is none. */
    fun place(config: LauncherConfig, action: ButtonAction.Shortcut): LauncherConfig? {
        val (x, y) = spotFor(config) ?: return null
        val home = config.homeScreen
        val tile = Button(action = action)
        val existing = home.cellAt(x, y)
        val cells = if (existing == null) {
            home.cells + Cell(x, y, button = tile)
        } else {
            // an empty cell keeps its size.
            home.cells.map { if (it == existing) it.copy(button = tile) else it }
        }
        return config.copy(
            screens = config.screens.map { if (it.id == home.id) home.copy(cells = cells) else it },
        )
    }
}
