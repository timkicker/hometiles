package org.biglau.tiles

/**
 * Wischen zwischen Screens - standardmäßig aus, und aus gutem Grund.
 *
 * Auf diesem Gerät läuft Gestennavigation: das Wischen von den Seitenrändern gehört der
 * Zurück-Geste, und Android reserviert dafür Streifen an beiden Kanten. Ein Wisch, der dort
 * beginnt, kommt bei uns nie an. Deshalb zählt nur, was **innerhalb** der Fläche beginnt,
 * und erst ab einer Strecke, die niemand versehentlich zurücklegt.
 *
 * `PLAN.md` 3.2: „Screenwechsel über Kacheln statt Wischen; Wischen bleibt optional und
 * standardmäßig aus."
 */
object SwipeGesture {

    /** Ab hier ist es ein Wisch und kein verrutschter Tipp. */
    const val THRESHOLD_DP = 64f

    /** Wie breit die Streifen an den Kanten sind, die der Zurück-Geste gehören. */
    const val EDGE_DP = 24f

    enum class Direction { NEXT, PREVIOUS, NONE }

    /**
     * @param startXDp wo der Finger aufgesetzt hat, vom linken Rand aus
     * @param dragDp zurückgelegte Strecke; negativ heißt nach links
     * @param widthDp Breite der Fläche
     */
    fun decide(startXDp: Float, dragDp: Float, widthDp: Float): Direction {
        // An den Kanten gehört die Geste dem System. Dort gar nicht erst mitzuhören ist
        // ehrlicher, als sich mit Android um denselben Finger zu streiten.
        if (startXDp < EDGE_DP || startXDp > widthDp - EDGE_DP) return Direction.NONE
        return when {
            dragDp <= -THRESHOLD_DP -> Direction.NEXT
            dragDp >= THRESHOLD_DP -> Direction.PREVIOUS
            else -> Direction.NONE
        }
    }
}
