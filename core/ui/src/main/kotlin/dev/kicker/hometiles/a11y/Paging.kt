package dev.kicker.hometiles.a11y

/**
 * paging through long lists without swiping.
 *
 * swiping needs a steady hand; two big buttons move exactly one page instead, with one row
 * of overlap so no row is skipped. a skipped row on a contact list means the name is
 * simply not there.
 */
object Paging {

    data class Row(val index: Int, val offset: Int, val size: Int)

    /**
     * only fully visible rows count. a cut-off row hangs at both edges; counting it would
     * make the jump one row too far and lose exactly that row.
     */
    fun fullyVisible(rows: List<Row>, viewportStart: Int, viewportEnd: Int): List<Row> =
        rows.filter { it.offset >= viewportStart && it.offset + it.size <= viewportEnd }

    fun firstFullyVisibleIndex(rows: List<Row>, viewportStart: Int, viewportEnd: Int, fallback: Int): Int =
        fullyVisible(rows, viewportStart, viewportEnd).firstOrNull()?.index ?: fallback

    /** one page minus one row, but at least one row. */
    fun step(visibleCount: Int): Int = maxOf(1, visibleCount - 1)

    fun down(firstVisible: Int, visibleCount: Int, total: Int): Int =
        (firstVisible + step(visibleCount)).coerceIn(0, lastStart(visibleCount, total))

    fun up(firstVisible: Int, visibleCount: Int): Int =
        maxOf(0, firstVisible - step(visibleCount))

    /** the last useful start index; beyond it the button would look pressed and do nothing. */
    fun lastStart(visibleCount: Int, total: Int): Int = maxOf(0, total - maxOf(1, visibleCount))

    fun canGoUp(firstVisible: Int): Boolean = firstVisible > 0

    fun canGoDown(firstVisible: Int, visibleCount: Int, total: Int): Boolean =
        firstVisible < lastStart(visibleCount, total)
}
