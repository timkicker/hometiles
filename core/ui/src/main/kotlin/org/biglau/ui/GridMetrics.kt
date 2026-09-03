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
fun labelSizeSp(
    cellWidthDp: Float,
    cellHeightDp: Float,
    userScale: Float,
    labelScale: Float = 1.0f,
): Float = minOf(cellHeightDp * 0.14f, cellWidthDp * 0.16f).coerceIn(14f, 40f) *
    userScale * labelScale.coerceIn(LABEL_SCALE_MIN, LABEL_SCALE_MAX)

/**
 * Die Schriftgroessen, die eine Beschriftung der Reihe nach versucht.
 *
 * **Anlass:** bei 200 % App-Schrift auf 1,35-facher Systemschrift stand auf den Kacheln
 * „Einstellun…" und „Verpasste …". Beides sind Woerter, die diese App braucht - und wer
 * 200 % einstellt, tut das nicht zum Spass, sondern weil er kleiner nichts liest. Ein
 * abgeschnittenes Wort hilft ihm nicht, ein etwas kleineres schon.
 *
 * Deshalb wird die Beschriftung in Stufen kleiner versucht, bevor sie abgeschnitten oder
 * (bei eingeschalteter Option) ausgeblendet wird. Die Untergrenze liegt bei 70 % des
 * Wunsches: darunter waere die Ersparnis gross und die Lesbarkeit dahin - dann ist
 * Abschneiden die ehrlichere Antwort.
 */
fun labelLadder(wishSp: Float): List<Float> =
    listOf(1f, 0.925f, 0.85f, 0.775f, 0.7f).map { wishSp * it }

/**
 * Die Breite, die der Beschriftung in der Kachel bleibt - in dp.
 *
 * `PLAN.md` 3.2 sagt eine Option zu: „Label ausblenden, wenn es nicht in zwei Zeilen
 * passt". Ob es passt, wird **gemessen** und nicht geschaetzt. Der erste Versuch rechnete
 * mit einer mittleren Zeichenbreite und lag daneben: „Nachrichten" haette er auf dem
 * Standardraster ausgeblendet, obwohl es dort vollstaendig steht (am Bildschirm
 * nachgesehen). Hier bleibt nur die Rechnung, wie viel Platz da ist.
 */
fun labelWidthDp(cellWidthDp: Float, cellHeightDp: Float): Float {
    val rand = (cellHeightDp * 0.06f).coerceIn(6f, 16f)
    return (cellWidthDp - 2f * rand).coerceAtLeast(1f)
}

/** PLAN.md 4.2: Label-Groesse relativ zur Kachel, 50-150 %. */
const val LABEL_SCALE_MIN = 0.5f
const val LABEL_SCALE_MAX = 1.5f
val LABEL_SCALES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

/** PLAN.md 4.2: Icongroesse 20-60 % der Zelle. */
const val ICON_PERCENT_MIN = 20
const val ICON_PERCENT_MAX = 60
val ICON_PERCENTS = listOf(20, 30, 40, 50, 60)

/**
 * Hoehe der Beschriftungszone. Fest je Zellgroesse, damit die Grundlinien einer Rasterzeile
 * zusammenfallen - aber nach oben durch die Schriftgroesse begrenzt, sonst klafft unter einer
 * hohen Kachel eine leere Flaeche.
 *
 * Und nach unten durch die Schriftgroesse begrenzt, seit die Querlage einstellbar ist: eine
 * Zelle von 60 dp Hoehe bekam ueber 28 Prozent nur 16,8 dp, und eine fette 14-sp-Zeile
 * braucht mit Unterlaengen knapp 19. Die Beschriftungen standen quer alle abgeschnitten da.
 * Ein Wort mit abgesaebeltem Unterrand liest sich schlechter als eines, fuer das das Symbol
 * ein Stueck kleiner wird - und kleiner wird es, weil [iconSizeDp] die Zone abzieht.
 */
fun labelZoneDp(cellHeightDp: Float, labelSp: Float): Float =
    minOf(cellHeightDp * 0.28f, labelSp * 2.2f)
        .coerceAtLeast(labelSp * 1.35f)
        // Aber nie mehr als die halbe Kachel: sonst bliebe fuer den Inhalt nichts uebrig.
        .coerceAtMost(cellHeightDp * 0.5f)

/**
 * Icongroesse als Anteil der kuerzeren Zellenkante, Vorgabe 40 %.
 *
 * Der Anteil ist nicht das letzte Wort: das Icon darf nie in die Beschriftungszone
 * hineinwachsen. Sonst legte sich bei 60 % auf einer flachen Kachel das Symbol ueber das
 * Wort, und beides waere schlechter zu lesen als vorher. Wer die Icons gross will, soll
 * sie so gross bekommen, wie sie hinpassen - und nicht groesser.
 */
fun iconSizeDp(
    cellWidthDp: Float,
    cellHeightDp: Float,
    percent: Int = 40,
    labelZoneDp: Float = 0f,
): Float {
    val anteil = percent.coerceIn(ICON_PERCENT_MIN, ICON_PERCENT_MAX) / 100f
    val gewuenscht = (minOf(cellWidthDp, cellHeightDp) * anteil).coerceIn(24f, 96f)
    // Ohne Beschriftung gehoert die ganze Zelle dem Symbol. Der Deckel gilt nur gegen die
    // Beschriftungszone - sonst schrumpfte er auch dort, wo gar nichts im Weg steht.
    if (labelZoneDp <= 0f) return gewuenscht
    val platz = cellHeightDp - labelZoneDp - 8f
    return minOf(gewuenscht, platz).coerceAtLeast(16f)
}

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
