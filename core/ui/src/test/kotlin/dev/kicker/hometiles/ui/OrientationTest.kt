package dev.kicker.hometiles.ui

import android.content.pm.ActivityInfo
import dev.kicker.hometiles.data.Appearance
import dev.kicker.hometiles.data.ScreenOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: "Bildschirmausrichtung: automatisch / hoch / quer", quoted in the plan's german.
 *
 * it stood twelve times as `portrait` in the manifest - a decision promised as a setting and
 * changeable nowhere.
 */
class OrientationTest {

    // the default is what used to be wired in: nobody's phone should start turning just
    // because the setting now exists.
    @Test
    fun `the default stays upright`() {
        assertEquals(ScreenOrientation.PORTRAIT, Appearance().orientation)
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            Orientation.requested(ScreenOrientation.PORTRAIT),
        )
    }

    @Test
    fun `landscape is landscape`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            Orientation.requested(ScreenOrientation.LANDSCAPE),
        )
    }

    /**
     * automatic means SENSOR, not UNSPECIFIED. UNSPECIFIED leaves the matter to the system's
     * rotation lock - and whoever has that on would have made a choice here that stays
     * without effect. then one looks for the fault in us.
     */
    @Test
    fun `automatic turns even with the system rotation locked`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            Orientation.requested(ScreenOrientation.AUTO),
        )
    }

    @Test
    fun `there are exactly three possibilities`() {
        assertEquals(3, ScreenOrientation.entries.size)
    }
}
