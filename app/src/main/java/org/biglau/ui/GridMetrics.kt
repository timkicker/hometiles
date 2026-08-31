package org.biglau.ui

/** Masse eines Rasters, alle Werte in dp. */
data class GridMetrics(
    val border: Float,
    val cellWidth: Float,
    val cellHeight: Float,
) {
    fun offsetX(column: Int, gutter: Float): Float = (cellWidth + gutter) * column
    fun offsetY(row: Int, gutter: Float): Float = (cellHeight + gutter) * row

    /** Breite einer Zelle, die [span] Spalten ueberspannt. */
    fun spanWidth(span: Int, gutter: Float): Float = cellWidth * span + gutter * (span - 1)

    /** Hoehe einer Zelle, die [span] Zeilen ueberspannt. */
    fun spanHeight(span: Int, gutter: Float): Float = cellHeight * span + gutter * (span - 1)
}

/**
 * Rechnet die Zellmasse fuer eine Flaeche aus. Getrennt vom Composable, weil das die
 * Stelle ist, an der ein Fehler jedes Layout kippt - und weil sie so pruefbar bleibt.
 */
fun gridMetrics(
    availableWidth: Float,
    availableHeight: Float,
    cols: Int,
    rows: Int,
    gutter: Float,
    borderPercent: Int,
): GridMetrics {
    require(cols > 0 && rows > 0) { "Raster braucht mindestens eine Spalte und eine Zeile" }
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
 * Schriftgroesse der Beschriftung (PLAN.md 3.2).
 *
 * Beide Kanten zaehlen. Nur aus der Hoehe gerechnet bekam eine hohe schmale Kachel 40 sp,
 * und "Contacts" brach mitten im Wort - sichtbar geworden, sobald die erste Zelle zwei
 * Zeilen ueberspannte.
 *
 * Bewusst nicht aus sp abgeleitet: die Systemschriftgroesse steht auf dem Zielgeraet schon
 * auf 1,35 und wuerde sich sonst ein zweites Mal multiplizieren.
 */
fun labelSizeSp(cellWidthDp: Float, cellHeightDp: Float, userScale: Float): Float =
    minOf(cellHeightDp * 0.14f, cellWidthDp * 0.16f).coerceIn(14f, 40f) * userScale

/**
 * Hoehe der Beschriftungszone. Fest je Zellgroesse, damit die Grundlinien einer Rasterzeile
 * zusammenfallen - aber nach oben durch die Schriftgroesse begrenzt, sonst klafft unter einer
 * hohen Kachel eine leere Flaeche.
 */
fun labelZoneDp(cellHeightDp: Float, labelSp: Float): Float =
    minOf(cellHeightDp * 0.28f, labelSp * 2.2f)

/** Icongroesse: 40 % der kuerzeren Zellenkante. */
fun iconSizeDp(cellWidthDp: Float, cellHeightDp: Float): Float =
    (minOf(cellWidthDp, cellHeightDp) * 0.40f).coerceIn(24f, 96f)

/**
 * Schriftgroesse fuer eine Zeile, die vollstaendig in die Zelle passen muss - Uhrzeit,
 * Ladestand, spaeter die gewaehlte Nummer.
 *
 * Nur aus der Zellhoehe gerechnet ergab "4:54 PM" eine Groesse, bei der sich der Text
 * selbst ueberlagerte. Es zaehlt deshalb auch, wie viele Zeichen unterzubringen sind:
 * ein fetter serifenloser Buchstabe ist grob 0,60 der Schriftgroesse breit.
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
