package org.biglau.tiles

import org.biglau.data.Cell
import org.biglau.data.Screen

/**
 * the four d-pad directions. `PLAN.md` 10.3.2.
 *
 * named `Richtung` and not `Direction`, which would collide with compose's own and with
 * [SwipeGesture.Direction] in this same package.
 */
enum class PadDirection { LEFT, RIGHT, UP, DOWN }

/**
 * where the focus goes, worked out from the rectangles.
 *
 * cells sit in `config.json` in the order they were created. following that would be easy
 * and wrong: the focus would jump about the screen, differently for every user, depending on
 * the order in which they filled their tiles. so geometry decides.
 *
 * pure arithmetic without android, so it can be checked without a device; actually setting
 * the focus is the surface's job.
 */
object FocusOrder {

    /**
     * the neighbour in this direction, or `null` at the edge, where nothing then happens.
     *
     * [rememberedColumn] is the column the focus originally came from. it counts only for
     * [PadDirection.UP] and [PadDirection.DOWN] and solves the case that otherwise only
     * shows up on the device: going down from a narrow tile onto a wide one and back up
     * should return to the narrow one. without the memory the wide tile's left edge wins and
     * the focus creeps left with every up and down until it sticks in the first column.
     */
    fun neighbour(
        cells: List<Cell>,
        from: Cell,
        direction: PadDirection,
        rememberedColumn: Int? = null,
    ): Cell? = when (direction) {
        // nearest edge first, upper row on a tie. sorted rather than computed: a formula
        // like x * 100 + y carries a silent assumption about how big a grid may get.
        PadDirection.RIGHT -> horizontal(cells, from) { it.x >= from.x + from.w }
            .minWithOrNull(compareBy({ it.x }, { it.y }))
        PadDirection.LEFT -> horizontal(cells, from) { it.x + it.w <= from.x }
            .minWithOrNull(compareBy({ -(it.x + it.w) }, { it.y }))
        PadDirection.DOWN -> vertical(cells, from, rememberedColumn, { it.y >= from.y + from.h }) { it.y }
        PadDirection.UP -> vertical(cells, from, rememberedColumn, { it.y + it.h <= from.y }) { -(it.y + it.h) }
    }

    /**
     * everything on this screen that may take focus: the tiles **and the empty slots**.
     *
     * an empty slot carries a "tap" and opens the editor. a finger reaches it at once, which
     * is exactly why it goes unnoticed when keys cannot. `PLAN.md` 10.3.6: not "big enough"
     * but **reachable**.
     */
    fun targets(screen: Screen): List<Cell> =
        screen.cells + screen.freeSlots().map { (x, y) -> Cell(x = x, y = y) }

    /**
     * the slot with this number, counted as one reads. `PLAN.md` 10.3.3.
     *
     * **slots** are counted, not filled tiles: whoever remembers the pharmacy is number four
     * keeps four when a tile appears or vanishes beside it. one to nine only, because a
     * keypad has no more digits and a two-digit entry with a timeout is a trap for slow hands.
     */
    fun numbered(cells: List<Cell>, digit: Int): Cell? {
        if (digit !in 1..9) return null
        return cells.sortedWith(compareBy({ it.y }, { it.x })).getOrNull(digit - 1)
    }

    /** top left, not whichever happens to be first in the list. */
    fun first(cells: List<Cell>): Cell? = cells.minWithOrNull(compareBy({ it.y }, { it.x }))

    /** rows overlap: for moving left and right. */
    private fun horizontal(cells: List<Cell>, from: Cell, beyond: (Cell) -> Boolean): List<Cell> =
        cells.filter { it != from && beyond(it) && it.y < from.y + from.h && from.y < it.y + it.h }

    /**
     * the next row in this direction, and in it the cell under the remembered column.
     *
     * if that column is not there, the nearest one wins. running into nothing would be the
     * worst outcome: a screen without focus is a frozen one on a key phone.
     */
    private fun vertical(
        cells: List<Cell>,
        from: Cell,
        rememberedColumn: Int?,
        beyond: (Cell) -> Boolean,
        nearness: (Cell) -> Int,
    ): Cell? {
        val candidates = cells.filter { it != from && beyond(it) }
        if (candidates.isEmpty()) return null
        val nearest = candidates.minOf(nearness)
        val row = candidates.filter { nearness(it) == nearest }
        val column = rememberedColumn ?: from.x
        return row.firstOrNull { column >= it.x && column < it.x + it.w }
            ?: row.minByOrNull { distance(column, it) }
    }

    /** how far the column is from this cell, zero when it lies inside. */
    private fun distance(column: Int, cell: Cell): Int = when {
        column < cell.x -> cell.x - column
        column >= cell.x + cell.w -> column - (cell.x + cell.w - 1)
        else -> 0
    }
}
