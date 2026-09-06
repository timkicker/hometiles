package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.Screen

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * growing and shrinking cells.
 *
 * deliberately strict: a cell may only grow into space the grid has and no other cell holds.
 * overwriting a tile silently would be data loss exactly where nobody expects it.
 */
object CellLayout {

    /** the rectangle the cell would have after growing. */
    fun stretched(cell: Cell, direction: Direction): Cell = when (direction) {
        Direction.UP -> cell.copy(y = cell.y - 1, h = cell.h + 1)
        Direction.DOWN -> cell.copy(h = cell.h + 1)
        Direction.LEFT -> cell.copy(x = cell.x - 1, w = cell.w + 1)
        Direction.RIGHT -> cell.copy(w = cell.w + 1)
    }

    /** after shrinking; the named side's edge moves back. */
    fun shrunk(cell: Cell, direction: Direction): Cell = when (direction) {
        Direction.UP -> cell.copy(y = cell.y + 1, h = cell.h - 1)
        Direction.DOWN -> cell.copy(h = cell.h - 1)
        Direction.LEFT -> cell.copy(x = cell.x + 1, w = cell.w - 1)
        Direction.RIGHT -> cell.copy(w = cell.w - 1)
    }

    fun canStretch(screen: Screen, cell: Cell, direction: Direction): Boolean {
        val next = stretched(cell, direction)
        if (!insideGrid(screen, next)) return false
        return newlyCovered(cell, next).none { (x, y) ->
            screen.cellAt(x, y).let { it != null && it != cell }
        }
    }

    fun canShrink(cell: Cell, direction: Direction): Boolean = when (direction) {
        Direction.UP, Direction.DOWN -> cell.h > 1
        Direction.LEFT, Direction.RIGHT -> cell.w > 1
    }

    /** returns the screen unchanged when the step is not allowed. */
    fun stretch(screen: Screen, cell: Cell, direction: Direction): Screen {
        if (!canStretch(screen, cell, direction)) return screen
        return replace(screen, cell, stretched(cell, direction))
    }

    fun shrink(screen: Screen, cell: Cell, direction: Direction): Screen {
        if (!canShrink(cell, direction)) return screen
        return replace(screen, cell, shrunk(cell, direction))
    }

    /** every direction this cell can currently grow in. */
    fun stretchable(screen: Screen, cell: Cell): List<Direction> =
        Direction.entries.filter { canStretch(screen, cell, it) }

    fun shrinkable(cell: Cell): List<Direction> =
        Direction.entries.filter { canShrink(cell, it) }

    /**
     * grows a cell to the wanted span, right first, then down, then left and up. null when
     * there is not enough room, so the surface can say so instead of leaving a half-grown cell.
     */
    fun growTo(screen: Screen, cell: Cell, targetWidth: Int, targetHeight: Int): Screen? {
        var board = screen
        var current = cell
        var guard = 0

        while ((current.w < targetWidth || current.h < targetHeight) && guard < 32) {
            guard++
            val direction = when {
                current.w < targetWidth && canStretch(board, current, Direction.RIGHT) -> Direction.RIGHT
                current.h < targetHeight && canStretch(board, current, Direction.DOWN) -> Direction.DOWN
                current.w < targetWidth && canStretch(board, current, Direction.LEFT) -> Direction.LEFT
                current.h < targetHeight && canStretch(board, current, Direction.UP) -> Direction.UP
                else -> return null
            }
            board = stretch(board, current, direction)
            current = board.cells.firstOrNull { it.button == current.button && it.covers(cell.x, cell.y) }
                ?: return null
        }
        return if (current.w >= targetWidth && current.h >= targetHeight) board else null
    }

    /** fits a screen to a changed grid: cells outside vanish, cells hanging over are trimmed. */
    fun fitToGrid(screen: Screen): Screen {
        val fitted = screen.cells.mapNotNull { cell ->
            if (cell.x >= screen.cols || cell.y >= screen.rows) return@mapNotNull null
            val width = minOf(cell.w, screen.cols - cell.x)
            val height = minOf(cell.h, screen.rows - cell.y)
            if (width < 1 || height < 1) null else cell.copy(w = width, h = height)
        }
        return screen.copy(cells = fitted)
    }

    private fun insideGrid(screen: Screen, cell: Cell): Boolean =
        cell.x >= 0 && cell.y >= 0 &&
            cell.x + cell.w <= screen.cols &&
            cell.y + cell.h <= screen.rows

    /** slots covered after growing that were not covered before. */
    private fun newlyCovered(before: Cell, after: Cell): List<Pair<Int, Int>> =
        (after.y until after.y + after.h).flatMap { y ->
            (after.x until after.x + after.w).map { x -> x to y }
        }.filterNot { (x, y) -> before.covers(x, y) }

    /**
     * fills the slot at (x, y), or creates a 1x1 cell there.
     *
     * this is the arithmetic that overwrites a user's tile, so it belongs where it can be
     * tested, not in `core:data`.
     */
    fun withButton(screen: Screen, x: Int, y: Int, button: Button): Screen {
        val existing = screen.cellAt(x, y)
        val cells = screen.cells.toMutableList()
        if (existing == null) {
            cells.add(Cell(x = x, y = y, button = button))
        } else {
            cells[cells.indexOf(existing)] = existing.copy(button = button)
        }
        return screen.copy(cells = cells)
    }

    /**
     * empties a slot by making the cell **vanish**, not by leaving one without an action:
     * that would give "empty" two shapes in the model, and the second silently blocks the
     * neighbours from growing.
     */
    fun withoutButton(screen: Screen, x: Int, y: Int): Screen {
        val existing = screen.cellAt(x, y) ?: return screen
        return screen.copy(cells = screen.cells - existing)
    }

    private fun replace(screen: Screen, old: Cell, new: Cell): Screen =
        screen.copy(cells = screen.cells.map { if (it == old) new else it })
}
