package org.biglau.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * the "usable after the system bars" row on the diagnostics page.
 *
 * it reported **565 dp** on the Jelly 2, usable are **581**. the fault was the starting
 * number: `resources.displayMetrics` gives the window already without the gesture bar
 * (480 x 832 instead of 480 x 854), and the insets subtracted it a second time.
 *
 * the same 565 dp stood in `PLAN.md` 3.2 as measured on the device - two places, the same
 * mistake, and because they confirmed each other it looked like a checked number.
 */
class DiagnosticsUsableTest {

    /** the Jelly 2, with the bars measured on 03.09.2026. */
    @Test
    fun `on the Jelly 2 349 by 581 dp are left`() {
        assertEquals(
            349 to 581,
            Diagnostics.usableDp(480, 854, left = 0, top = 33, right = 0, bottom = 22, density = 1.375f),
        )
    }

    /**
     * the counter-check to the old fault: feed in the window height *without* the gesture
     * bar and 565 comes out again. the computation is right, the input was wrong.
     */
    @Test
    fun `with the window height instead of the screen height 565 would come out again`() {
        assertEquals(
            349 to 565,
            Diagnostics.usableDp(480, 832, left = 0, top = 33, right = 0, bottom = 22, density = 1.375f),
        )
    }

    /** in landscape the bars sit at the side - then something goes off there and little on top. */
    @Test
    fun `side bars come off the width`() {
        assertEquals(
            332 to 186,
            Diagnostics.usableDp(854, 480, left = 0, top = 15, right = 22, bottom = 0, density = 2.5f),
        )
    }

    @Test
    fun `without a density there is no number instead of a guessed one`() {
        assertNull(Diagnostics.usableDp(480, 854, 0, 33, 0, 22, density = 0f))
    }

    /** insets larger than the screen give zero area, not a negative one. */
    @Test
    fun `the area never turns negative`() {
        assertEquals(0 to 0, Diagnostics.usableDp(100, 100, 60, 60, 60, 60, density = 1f))
    }
}
