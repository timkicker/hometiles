package dev.kicker.hometiles.apps

/**
 * the app list as a grid (issue #7): how many apps stand side by side.
 *
 * by the width that is left, not fixed: three on the jelly 2's 349 dp, more on a tablet. a
 * larger text needs wider cells, or the names break into syllables, so the text scale counts
 * too. never fewer than two, or it is a list with big icons; never more than six, where the
 * icons stop being large.
 */
object AppGrid {

    /** the width a cell needs at text scale 1, in dp. three fit the jelly 2 with room to spare. */
    const val CELL_DP = 104

    fun columns(widthDp: Int, textScale: Float): Int =
        (widthDp / (CELL_DP * textScale.coerceAtLeast(1f))).toInt().coerceIn(2, 6)

    /** the apps in rows of [columns]; the last row may be shorter. */
    fun <T> rows(apps: List<T>, columns: Int): List<List<T>> = apps.chunked(columns.coerceAtLeast(1))
}
