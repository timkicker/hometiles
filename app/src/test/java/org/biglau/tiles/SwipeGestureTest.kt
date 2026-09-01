package org.biglau.tiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wischen zwischen Screens.
 *
 * Der Grund für die Kantenregel: auf diesem Gerät läuft Gestennavigation, und das Wischen
 * von den Rändern gehört der Zurück-Geste. Ein Wisch, der dort beginnt, kommt bei uns nie
 * an - also hört BigLau dort gar nicht erst mit, statt sich mit Android um denselben Finger
 * zu streiten.
 */
class SwipeGestureTest {

    private val breite = 349f

    private fun wisch(start: Float, strecke: Float) =
        SwipeGesture.decide(start, strecke, breite)

    @Test
    fun `nach links geht weiter`() {
        assertEquals(SwipeGesture.Direction.NEXT, wisch(200f, -100f))
    }

    @Test
    fun `nach rechts geht zurueck`() {
        assertEquals(SwipeGesture.Direction.PREVIOUS, wisch(150f, 100f))
    }

    @Test
    fun `ein verrutschter Tipp ist kein Wisch`() {
        assertEquals(SwipeGesture.Direction.NONE, wisch(180f, -20f))
        assertEquals(SwipeGesture.Direction.NONE, wisch(180f, 20f))
    }

    @Test
    fun `genau auf der Schwelle zaehlt`() {
        assertEquals(SwipeGesture.Direction.NEXT, wisch(180f, -SwipeGesture.THRESHOLD_DP))
        assertEquals(SwipeGesture.Direction.PREVIOUS, wisch(180f, SwipeGesture.THRESHOLD_DP))
    }

    @Test
    fun `am linken Rand gehoert die Geste dem System`() {
        assertEquals(SwipeGesture.Direction.NONE, wisch(5f, 200f))
    }

    @Test
    fun `am rechten Rand ebenso`() {
        assertEquals(SwipeGesture.Direction.NONE, wisch(breite - 5f, -200f))
    }

    @Test
    fun `knapp innerhalb der Kante zaehlt wieder`() {
        assertEquals(SwipeGesture.Direction.NEXT, wisch(SwipeGesture.EDGE_DP + 1f, -100f))
    }

    @Test
    fun `ohne Bewegung passiert nichts`() {
        assertEquals(SwipeGesture.Direction.NONE, wisch(180f, 0f))
    }
}
