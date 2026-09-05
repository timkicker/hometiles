package org.biglau.ui

/** the three numbers that measure the grid, with their bounds. `PLAN.md` 3.2 and 4.1. */
object GridLooks {

    val GUTTERS = listOf(0, 2, 4, 8, 12)

    /** outer margin in percent of the screen width, for rounded corners. */
    val BORDERS = listOf(0, 2, 5, 10, 15)

    /** one radius for every surface; `PLAN.md` 3.7 forbids a second one beside it. */
    val RADII = listOf(0, 6, 12, 18, 24)

    fun gutter(dp: Int): Int = dp.coerceIn(0, 12)

    fun border(percent: Int): Int = percent.coerceIn(0, 15)

    fun radius(dp: Int): Int = dp.coerceIn(0, 24)
}
