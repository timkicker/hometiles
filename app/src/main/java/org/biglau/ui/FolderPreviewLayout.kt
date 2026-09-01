package org.biglau.ui

/**
 * Wie gross die Vorschau-Symbole im Ordner sind und wie viele Reihen hineinpassen.
 *
 * Sie waren bisher nur aus der Zellbreite gerechnet. Quer ist eine Kachel 400 dp breit und
 * 60 hoch: die Symbole wurden so gross wie erlaubt, zwei Reihen ergaben 72 dp - und
 * schoben die Beschriftung aus der Kachel. Der Ordner hiess dann gar nichts mehr.
 */
object FolderPreviewLayout {

    const val GAP_DP = 4f
    private const val MIN_EDGE = 14f
    private const val MAX_EDGE = 34f

    /** Kantenlaenge eines Vorschau-Symbols. */
    fun edgeDp(cellWidthDp: Float, availableHeightDp: Float): Float {
        val ausBreite = cellWidthDp * 0.20f
        // Zwei Reihen mit Abstand muessen in die Hoehe passen, sonst wird das Symbol
        // kleiner statt die Beschriftung wegzudruecken.
        val ausHoehe = (availableHeightDp - GAP_DP) / 2f
        return minOf(ausBreite, ausHoehe).coerceIn(MIN_EDGE, MAX_EDGE)
    }

    /**
     * Wie viele Reihen gezeigt werden. Passt nur eine, ist eine Reihe mit zwei Symbolen
     * ehrlicher als zwei angeschnittene - und die Beschriftung bleibt stehen.
     */
    fun rows(availableHeightDp: Float, edgeDp: Float): Int =
        if (availableHeightDp >= edgeDp * 2 + GAP_DP) 2 else 1
}
