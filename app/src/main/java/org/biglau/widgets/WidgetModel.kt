package org.biglau.widgets

/** Ein Widget-Anbieter, so weit ihn die Auswahl braucht. */
data class WidgetProviderRow(
    val packageName: String,
    val className: String,
    val label: String,
    val appLabel: String,
    /** Mindestmasse in dp, wie der Anbieter sie meldet. */
    val minWidthDp: Int,
    val minHeightDp: Int,
    val resizeHorizontal: Boolean = false,
    val resizeVertical: Boolean = false,
    val needsConfiguration: Boolean = false,
) {
    val component: String get() = "$packageName/$className"
}

/**
 * Welche Widgets in welche Zelle passen.
 *
 * Ein Widget, das breiter ist als die Zelle, wird nicht abgeschnitten sondern gestaucht -
 * und sieht dann kaputt aus. Deshalb wird vorher gerechnet und dem Nutzer gesagt, wie viele
 * Felder es braucht, statt ihn ein unbrauchbares Ergebnis herstellen zu lassen.
 */
object WidgetFit {

    /** Wie viele Rasterfelder das Widget mindestens braucht. */
    fun cellsNeeded(
        minDp: Int,
        cellDp: Float,
        gutterDp: Float,
    ): Int {
        if (minDp <= 0) return 1
        if (cellDp <= 0f) return 1
        var cells = 1
        while (cells * cellDp + (cells - 1) * gutterDp < minDp && cells < MAX_SPAN) cells++
        return cells
    }

    /** Passt der Anbieter in eine Zelle dieser Spannweite? */
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

    /** Text wie "braucht 2 x 1 Felder" - der Nutzer soll es vor dem Antippen wissen. */
    fun requirement(
        row: WidgetProviderRow,
        cellWidthDp: Float,
        cellHeightDp: Float,
        gutterDp: Float,
    ): Pair<Int, Int> = Pair(
        cellsNeeded(row.minWidthDp, cellWidthDp, gutterDp),
        cellsNeeded(row.minHeightDp, cellHeightDp, gutterDp),
    )

    /** Sortiert nach App, dann nach Widgetname - so sucht man auch. */
    fun sorted(rows: List<WidgetProviderRow>): List<WidgetProviderRow> = rows
        .filter { it.label.isNotBlank() }
        .distinctBy { it.component }
        .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))

    const val MAX_SPAN = 8
}
