package dev.kicker.hometiles.ui

/** measurements of a grid, all values in dp. */
data class GridMetrics(
    val border: Float,
    val cellWidth: Float,
    val cellHeight: Float,
) {
    fun offsetX(column: Int, gutter: Float): Float = (cellWidth + gutter) * column
    fun offsetY(row: Int, gutter: Float): Float = (cellHeight + gutter) * row

    /** width of a cell spanning [span] columns. */
    fun spanWidth(span: Int, gutter: Float): Float = cellWidth * span + gutter * (span - 1)

    /** height of a cell spanning [span] rows. */
    fun spanHeight(span: Int, gutter: Float): Float = cellHeight * span + gutter * (span - 1)
}

/** cell measurements for an area; apart from the composable so a mistake here is testable. */
fun gridMetrics(
    availableWidth: Float,
    availableHeight: Float,
    cols: Int,
    rows: Int,
    gutter: Float,
    borderPercent: Int,
): GridMetrics {
    require(cols > 0 && rows > 0) { "a grid needs at least one column and one row" }
    val border = availableWidth * (borderPercent.coerceIn(0, 15) / 100f)
    val innerWidth = availableWidth - border * 2
    val innerHeight = availableHeight - border * 2
    return GridMetrics(
        border = border,
        cellWidth = (innerWidth - gutter * (cols - 1)) / cols,
        cellHeight = (innerHeight - gutter * (rows - 1)) / rows,
    )
}

/**
 * label font size (`PLAN.md` 3.2).
 *
 * both edges count: from the height alone a tall narrow tile got 40 sp and "Contacts"
 * broke mid-word. not derived from sp, or the 1.35 system scale would multiply twice.
 */
fun labelSizeSp(
    cellWidthDp: Float,
    cellHeightDp: Float,
    userScale: Float,
    labelScale: Float = 1.0f,
): Float = minOf(cellHeightDp * 0.14f, cellWidthDp * 0.16f).coerceIn(14f, 40f) *
    userScale * labelScale.coerceIn(LABEL_SCALE_MIN, LABEL_SCALE_MAX)

/**
 * the sizes a label tries in turn before being cut off or hidden.
 *
 * at 200 % app scale on 1.35 system scale the tiles read "Einstellun..." and
 * "Verpasste ...". the floor is 70 % of the wish: below that cutting off is honester.
 */
fun labelLadder(wishSp: Float): List<Float> =
    listOf(1f, 0.925f, 0.85f, 0.775f, 0.7f).map { wishSp * it }

/**
 * the width left to the label inside the tile, in dp.
 *
 * `PLAN.md` 3.2 offers hiding a label that does not fit two lines, and whether it fits is
 * *measured*, not estimated: an average character width would have hidden "Nachrichten" on
 * the default grid where it stands in full. only the space is computed here.
 */
fun labelWidthDp(cellWidthDp: Float, cellHeightDp: Float): Float {
    val margin = (cellHeightDp * 0.06f).coerceIn(6f, 16f)
    return (cellWidthDp - 2f * margin).coerceAtLeast(1f)
}

/** `PLAN.md` 4.2: label size relative to the tile, 50 to 150 percent. */
const val LABEL_SCALE_MIN = 0.5f
const val LABEL_SCALE_MAX = 1.5f
val LABEL_SCALES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

/** `PLAN.md` 4.2: icon size, 20 to 60 percent of the cell. */
const val ICON_PERCENT_MIN = 20
const val ICON_PERCENT_MAX = 60
val ICON_PERCENTS = listOf(20, 30, 40, 50, 60)

/**
 * height of the label zone. fixed per cell size so the baselines of a grid row line up,
 * bounded above by the font size so no gap opens under a tall tile.
 *
 * and bounded below by it: a 60 dp cell got 16.8 dp from the 28 percent, while a bold 14 sp
 * line needs close to 19 with descenders, and every landscape label stood cut off.
 */
fun labelZoneDp(cellHeightDp: Float, labelSp: Float): Float =
    minOf(cellHeightDp * 0.28f, labelSp * 2.2f)
        .coerceAtLeast(labelSp * 1.35f)
        // never more than half the tile, or nothing is left for the content.
        .coerceAtMost(cellHeightDp * 0.5f)

/**
 * icon size as a share of the shorter cell edge, 40 percent by default.
 *
 * the share is not the last word: the icon must never grow into the label zone, where at
 * 60 percent on a flat tile it would lie over the word.
 */
fun iconSizeDp(
    cellWidthDp: Float,
    cellHeightDp: Float,
    percent: Int = 40,
    labelZoneDp: Float = 0f,
): Float {
    val share = percent.coerceIn(ICON_PERCENT_MIN, ICON_PERCENT_MAX) / 100f
    val wanted = (minOf(cellWidthDp, cellHeightDp) * share).coerceIn(24f, 96f)
    // without a label the whole cell belongs to the icon; the cap only guards the zone.
    if (labelZoneDp <= 0f) return wanted
    val room = cellHeightDp - labelZoneDp - 8f
    return minOf(wanted, room).coerceAtLeast(16f)
}

/**
 * font size for a line that must fit the cell whole: clock, battery, dialled number.
 *
 * from the height alone "4:54 PM" came out overlapping itself, so the character count
 * counts too; a bold sans letter is roughly 0.60 of the font size wide.
 */
fun singleLineSizeSp(
    text: String,
    cellWidthDp: Float,
    cellHeightDp: Float,
    userScale: Float,
    paddingDp: Float = 16f,
    maxSp: Float = 72f,
): Float {
    val characters = text.length.coerceAtLeast(1)
    val usableWidth = (cellWidthDp - paddingDp).coerceAtLeast(1f)
    val byWidth = usableWidth / (characters * 0.60f)
    val byHeight = cellHeightDp * 0.34f
    return minOf(byWidth, byHeight).coerceIn(12f, maxSp) * userScale
}
