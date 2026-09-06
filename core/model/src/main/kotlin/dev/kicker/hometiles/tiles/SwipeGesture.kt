package dev.kicker.hometiles.tiles

/**
 * swiping between screens, off by default (`PLAN.md` 3.2).
 *
 * gesture navigation owns the strips along both edges for its back gesture, so a swipe that
 * starts there never reaches us. only what starts *inside* counts, and only past a distance
 * nobody covers by accident.
 */
object SwipeGesture {

    /** past this it is a swipe and not a slipped tap. */
    const val THRESHOLD_DP = 64f

    /** width of the edge strips that belong to the back gesture. */
    const val EDGE_DP = 24f

    enum class Direction { NEXT, PREVIOUS, NONE }

    /**
     * @param startXDp where the finger went down, from the left edge
     * @param dragDp distance covered; negative means leftwards
     * @param widthDp width of the area
     */
    fun decide(startXDp: Float, dragDp: Float, widthDp: Float): Direction {
        // not listening at the edges at all is honester than fighting android over the
        // same finger.
        if (startXDp < EDGE_DP || startXDp > widthDp - EDGE_DP) return Direction.NONE
        return when {
            dragDp <= -THRESHOLD_DP -> Direction.NEXT
            dragDp >= THRESHOLD_DP -> Direction.PREVIOUS
            else -> Direction.NONE
        }
    }
}
