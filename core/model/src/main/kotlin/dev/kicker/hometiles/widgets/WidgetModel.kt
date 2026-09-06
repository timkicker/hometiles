package dev.kicker.hometiles.widgets

/** a widget provider, as far as the picker needs it. */
data class WidgetProviderRow(
    val packageName: String,
    val className: String,
    val label: String,
    val appLabel: String,
    /** minimum size in dp as the provider reports it. */
    val minWidthDp: Int,
    val minHeightDp: Int,
    val resizeHorizontal: Boolean = false,
    val resizeVertical: Boolean = false,
    val needsConfiguration: Boolean = false,
) {
    val component: String get() = "$packageName/$className"
}

/**
 * which widgets fit into which cell.
 *
 * a widget wider than its cell is squeezed, not cropped, and then looks broken. so the size
 * is worked out beforehand and stated, instead of letting someone build an unusable result.
 */
object WidgetFit {

    fun cellsNeeded(minDp: Int, cellDp: Float, gutterDp: Float): Int {
        if (minDp <= 0) return 1
        if (cellDp <= 0f) return 1
        var cells = 1
        while (cells * cellDp + (cells - 1) * gutterDp < minDp && cells < MAX_SPAN) cells++
        return cells
    }

    fun fits(
        row: WidgetProviderRow,
        spanX: Int,
        spanY: Int,
        cellWidthDp: Float,
        cellHeightDp: Float,
        gutterDp: Float,
    ): Boolean =
        cellsNeeded(row.minWidthDp, cellWidthDp, gutterDp) <= spanX &&
            cellsNeeded(row.minHeightDp, cellHeightDp, gutterDp) <= spanY

    /** for a line like "needs 2 x 1 cells", said before tapping rather than after. */
    fun requirement(
        row: WidgetProviderRow,
        cellWidthDp: Float,
        cellHeightDp: Float,
        gutterDp: Float,
    ): Pair<Int, Int> = Pair(
        cellsNeeded(row.minWidthDp, cellWidthDp, gutterDp),
        cellsNeeded(row.minHeightDp, cellHeightDp, gutterDp),
    )

    /**
     * a widget that cannot stretch gets pulled into a larger tile anyway, and then it looks
     * like a fault of this app. the provider reports it, so it can be said in advance.
     */
    fun fixedSize(row: WidgetProviderRow): Boolean =
        !row.resizeHorizontal && !row.resizeVertical

    /** by app, then by widget name: that is how one looks for them. */
    fun sorted(rows: List<WidgetProviderRow>): List<WidgetProviderRow> = rows
        .filter { it.label.isNotBlank() }
        .distinctBy { it.component }
        .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))

    const val MAX_SPAN = 8
}
