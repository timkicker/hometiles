package org.biglau.tiles

import org.biglau.data.Cell
import org.biglau.data.Screen

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * Zellen vergroessern und verkleinern - im Original heisst das "stretch" und "shrink".
 *
 * Die Regeln sind absichtlich streng: eine Zelle darf nur in Flaechen wachsen, die das Raster
 * hergibt und die keine andere Zelle belegt. Eine Belegung stillschweigend zu ueberschreiben
 * waere Datenverlust an der Stelle, an der der Nutzer am wenigsten damit rechnet.
 */
object CellLayout {

    /** Das Rechteck, das die Zelle nach dem Vergroessern haette. */
    fun stretched(cell: Cell, direction: Direction): Cell = when (direction) {
        Direction.UP -> cell.copy(y = cell.y - 1, h = cell.h + 1)
        Direction.DOWN -> cell.copy(h = cell.h + 1)
        Direction.LEFT -> cell.copy(x = cell.x - 1, w = cell.w + 1)
        Direction.RIGHT -> cell.copy(w = cell.w + 1)
    }

    /** Das Rechteck nach dem Verkleinern; die Kante der angegebenen Seite weicht zurueck. */
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

    /** Gibt den Screen unveraendert zurueck, wenn der Schritt nicht erlaubt ist. */
    fun stretch(screen: Screen, cell: Cell, direction: Direction): Screen {
        if (!canStretch(screen, cell, direction)) return screen
        return replace(screen, cell, stretched(cell, direction))
    }

    fun shrink(screen: Screen, cell: Cell, direction: Direction): Screen {
        if (!canShrink(cell, direction)) return screen
        return replace(screen, cell, shrunk(cell, direction))
    }

    /** Alle Richtungen, in die diese Zelle gerade wachsen kann. */
    fun stretchable(screen: Screen, cell: Cell): List<Direction> =
        Direction.entries.filter { canStretch(screen, cell, it) }

    fun shrinkable(cell: Cell): List<Direction> =
        Direction.entries.filter { canShrink(cell, it) }

    /**
     * Versucht, eine Zelle auf die gewuenschte Spannweite zu bringen - erst nach rechts,
     * dann nach unten, dann nach links und oben. Gibt null zurueck, wenn der Platz nicht
     * reicht: dann soll die Oberflaeche das sagen, statt eine halb gewachsene Zelle
     * zurueckzulassen.
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

    /**
     * Passt einen Screen an ein geaendertes Raster an. Zellen, die ganz herausfallen,
     * verschwinden; Zellen, die nur ueberstehen, werden gestutzt.
     */
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

    /** Rasterplaetze, die nach dem Wachsen belegt waeren und es vorher nicht waren. */
    private fun newlyCovered(before: Cell, after: Cell): List<Pair<Int, Int>> =
        (after.y until after.y + after.h).flatMap { y ->
            (after.x until after.x + after.w).map { x -> x to y }
        }.filterNot { (x, y) -> before.covers(x, y) }

    private fun replace(screen: Screen, old: Cell, new: Cell): Screen =
        screen.copy(cells = screen.cells.map { if (it == old) new else it })
}
