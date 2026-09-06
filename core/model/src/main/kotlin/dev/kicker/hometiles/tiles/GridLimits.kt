package dev.kicker.hometiles.tiles

/**
 * how many columns and rows this screen can carry. `PLAN.md` 4.1 names 1-6 x 1-8.
 *
 * counted backwards from what a tile needs, not from a fixed list of layouts: a hard limit
 * of three columns is true for this device and wrong as a rule, because it turns a property
 * of the screen into a number in the source.
 */
object GridLimits {

    /**
     * 48 dp would be the bare touch target, but no word fits on a 48 dp tile, and a tile
     * recognised only by its colour is not worth a label.
     */
    const val MIN_CELL_WIDTH_DP = 72f

    /** height needs the label zone plus some icon above it. */
    const val MIN_CELL_HEIGHT_DP = 56f

    const val MAX_COLUMNS = 6
    const val MAX_ROWS = 8

    fun maxColumns(usableWidthDp: Float, gutterDp: Int): Int =
        fits(usableWidthDp, gutterDp, MIN_CELL_WIDTH_DP, MAX_COLUMNS)

    fun maxRows(usableHeightDp: Float, gutterDp: Int): Int =
        fits(usableHeightDp, gutterDp, MIN_CELL_HEIGHT_DP, MAX_ROWS)

    fun columns(usableWidthDp: Float, gutterDp: Int): List<Int> =
        (1..maxColumns(usableWidthDp, gutterDp)).toList()

    fun rows(usableHeightDp: Float, gutterDp: Int): List<Int> =
        (1..maxRows(usableHeightDp, gutterDp)).toList()

    private fun fits(totalDp: Float, gutterDp: Int, minCell: Float, ceiling: Int): Int {
        var best = 1
        for (n in 1..ceiling) {
            val cell = (totalDp - (n - 1) * gutterDp) / n
            if (cell >= minCell) best = n
        }
        return best
    }
}
