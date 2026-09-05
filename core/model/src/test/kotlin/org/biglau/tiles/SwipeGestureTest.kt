package org.biglau.tiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * swiping between screens.
 *
 * the reason for the edge rule: the phone runs gesture navigation, and swiping from the edges
 * belongs to the back gesture. a swipe starting there never reaches us - so BigLau does not
 * even listen there instead of fighting android over the same finger.
 */
class SwipeGestureTest {

    private val width = 349f

    private fun swipe(start: Float, distance: Float) =
        SwipeGesture.decide(start, distance, width)

    @Test
    fun `to the left goes forward`() {
        assertEquals(SwipeGesture.Direction.NEXT, swipe(200f, -100f))
    }

    @Test
    fun `to the right goes back`() {
        assertEquals(SwipeGesture.Direction.PREVIOUS, swipe(150f, 100f))
    }

    @Test
    fun `a slipped tap is no swipe`() {
        assertEquals(SwipeGesture.Direction.NONE, swipe(180f, -20f))
        assertEquals(SwipeGesture.Direction.NONE, swipe(180f, 20f))
    }

    @Test
    fun `exactly on the threshold counts`() {
        assertEquals(SwipeGesture.Direction.NEXT, swipe(180f, -SwipeGesture.THRESHOLD_DP))
        assertEquals(SwipeGesture.Direction.PREVIOUS, swipe(180f, SwipeGesture.THRESHOLD_DP))
    }

    @Test
    fun `at the left edge the gesture belongs to the system`() {
        assertEquals(SwipeGesture.Direction.NONE, swipe(5f, 200f))
    }

    @Test
    fun `at the right edge likewise`() {
        assertEquals(SwipeGesture.Direction.NONE, swipe(width - 5f, -200f))
    }

    @Test
    fun `just inside the edge it counts again`() {
        assertEquals(SwipeGesture.Direction.NEXT, swipe(SwipeGesture.EDGE_DP + 1f, -100f))
    }

    @Test
    fun `without movement nothing happens`() {
        assertEquals(SwipeGesture.Direction.NONE, swipe(180f, 0f))
    }
}
