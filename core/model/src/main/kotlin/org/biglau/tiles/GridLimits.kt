package org.biglau.tiles

/**
 * Wie viele Spalten und Zeilen dieser Bildschirm tragen kann. PLAN.md 4.1 nennt 1-6 x 1-8.
 *
 * Die feste Liste von sechs Vorlagen endete bei drei Spalten, mit dem Argument, mehr werde
 * auf 349 dp zur Briefmarke. Das stimmt fuer dieses Geraet und ist trotzdem die falsche
 * Antwort: es macht die Grenze zu einer Zahl im Quelltext statt zu einer Eigenschaft des
 * Bildschirms. Auf einem groesseren Telefon waeren fuenf Spalten gut lesbar.
 *
 * Gerechnet wird deshalb rueckwaerts: wie viele Zellen passen, ohne dass eine Zelle unter
 * das faellt, was eine Kachel braucht.
 */
object GridLimits {

    /**
     * Schmaler wird eine Kachel nicht. 48 dp waere die blosse Touchflaeche - aber auf
     * einer 48 dp breiten Kachel steht kein Wort mehr, und eine Kachel, die man nur an
     * ihrer Farbe erkennt, ist keine Beschriftung wert.
     */
    const val MIN_CELL_WIDTH_DP = 72f

    /** In der Hoehe braucht es die Beschriftungszone plus etwas Symbol darueber. */
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

    private fun fits(gesamtDp: Float, gutterDp: Int, minZelle: Float, obergrenze: Int): Int {
        var passt = 1
        for (n in 1..obergrenze) {
            val zelle = (gesamtDp - (n - 1) * gutterDp) / n
            if (zelle >= minZelle) passt = n
        }
        return passt
    }
}
