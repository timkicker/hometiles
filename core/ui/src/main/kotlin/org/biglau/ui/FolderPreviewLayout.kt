package org.biglau.ui

/**
 * size and row count of the preview icons in a folder.
 *
 * computed from the cell width alone, a landscape tile of 400 by 60 dp grew two rows of
 * 72 dp and pushed the label out of the tile.
 */
object FolderPreviewLayout {

    const val GAP_DP = 4f
    private const val MIN_EDGE = 14f
    private const val MAX_EDGE = 34f

    fun edgeDp(cellWidthDp: Float, availableHeightDp: Float): Float {
        val fromWidth = cellWidthDp * 0.20f
        // two rows with a gap must fit the height, else the icon shrinks instead of
        // pushing the label away.
        val fromHeight = (availableHeightDp - GAP_DP) / 2f
        return minOf(fromWidth, fromHeight).coerceIn(MIN_EDGE, MAX_EDGE)
    }

    /** one row of two is honester than two cut-off ones, and the label stays. */
    fun rows(availableHeightDp: Float, edgeDp: Float): Int =
        if (availableHeightDp >= edgeDp * 2 + GAP_DP) 2 else 1
}
