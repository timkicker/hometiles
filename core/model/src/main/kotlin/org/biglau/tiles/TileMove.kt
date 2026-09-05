package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * moving a tile from one screen or folder to another.
 *
 * without this a folder is a one-way street: things go in and only come out by deleting them
 * there and recreating them elsewhere, which for a contact with the right number and mode is
 * annoying enough to give up on.
 */
object TileMove {

    /**
     * a folder may not go into a folder: two levels destroy the overview large tiles are
     * meant to give (`PLAN.md` 4.9). and nowhere without room, because a tile that vanishes
     * quietly while being moved would be the worst outcome.
     */
    fun targetsFor(config: LauncherConfig, fromScreenId: String, cell: Cell): List<Screen> {
        val isFolder = cell.button.action is ButtonAction.Folder
        return config.screens
            .filterNot { it.id == fromScreenId }
            .filterNot { isFolder && it.isFolder }
            .filter { it.freeSlots().isNotEmpty() }
    }

    /** [occupant] is what already lies there: it gets swapped, not overwritten. */
    data class Spot(val x: Int, val y: Int, val occupant: Cell?)

    /**
     * where the tile may go on its own screen.
     *
     * `PLAN.md` 4.1 promises swapping tiles and explains why there is no dragging: dragging
     * assumes a steady hand. that replacement was half built, moving worked only to *another*
     * screen, so reordering one's own home screen meant deleting and recreating.
     *
     * only lossless spots are offered: a free place the tile fits into at its size, and a
     * neighbour of equal size to swap with. unequal sizes would leave a hole or an overlap.
     */
    fun spotsFor(screen: Screen, cell: Cell): List<Spot> =
        (0 until screen.rows).flatMap { y -> (0 until screen.cols).map { x -> x to y } }
            .filterNot { (x, y) -> cell.covers(x, y) }
            .mapNotNull { (x, y) ->
                when (val taken = screen.cellAt(x, y)) {
                    null -> if (fits(screen, cell, x, y)) Spot(x, y, null) else null
                    // only at the neighbour's own corner, or a 2x2 tile would appear four
                    // times in the list.
                    else -> if (taken.x == x && taken.y == y && taken.w == cell.w && taken.h == cell.h) {
                        Spot(x, y, taken)
                    } else {
                        null
                    }
                }
            }

    private fun fits(screen: Screen, cell: Cell, x: Int, y: Int): Boolean {
        if (x + cell.w > screen.cols || y + cell.h > screen.rows) return false
        return (y until y + cell.h).all { py ->
            (x until x + cell.w).all { px ->
                val there = screen.cellAt(px, py)
                there == null || (there.x == cell.x && there.y == cell.y)
            }
        }
    }

    /**
     * `null` when the spot is not in [spotsFor]: the config can have changed between showing
     * the list and tapping it.
     */
    fun moveWithin(config: LauncherConfig, screenId: String, x: Int, y: Int, toX: Int, toY: Int): LauncherConfig? {
        val screen = config.screens.firstOrNull { it.id == screenId } ?: return null
        val tile = screen.cellAt(x, y) ?: return null
        val spot = spotsFor(screen, tile).firstOrNull { it.x == toX && it.y == toY } ?: return null
        val swapped = screen.cells.map { cell ->
            when {
                cell.x == tile.x && cell.y == tile.y -> cell.copy(x = spot.x, y = spot.y)
                spot.occupant != null && cell.x == spot.occupant.x && cell.y == spot.occupant.y ->
                    cell.copy(x = tile.x, y = tile.y)
                else -> cell
            }
        }
        return config.copy(
            screens = config.screens.map { if (it.id == screenId) it.copy(cells = swapped) else it },
        )
    }

    /**
     * onto the target's first free slot, read from the top left.
     *
     * `null` when it cannot be done, so the caller does nothing and says so rather than
     * leaving half a move behind.
     */
    fun move(config: LauncherConfig, fromScreenId: String, x: Int, y: Int, toScreenId: String): LauncherConfig? {
        if (fromScreenId == toScreenId) return null
        val source = config.screens.firstOrNull { it.id == fromScreenId } ?: return null
        val target = config.screens.firstOrNull { it.id == toScreenId } ?: return null
        val tile = source.cellAt(x, y) ?: return null
        if (tile.button.action is ButtonAction.Folder && target.isFolder) return null

        val slot = target.freeSlots().firstOrNull() ?: return null

        return config.copy(
            screens = config.screens.map { screen ->
                when (screen.id) {
                    source.id -> screen.copy(cells = screen.cells.filterNot { it.x == tile.x && it.y == tile.y })
                    target.id -> screen.copy(
                        cells = screen.cells + tile.copy(
                            x = slot.first,
                            y = slot.second,
                            // a wide tile arrives as a single cell instead of hanging over
                            // the edge.
                            w = 1,
                            h = 1,
                        ),
                    )
                    else -> screen
                }
            },
        )
    }
}
