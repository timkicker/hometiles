package org.biglau.ui

import android.content.pm.ActivityInfo
import org.biglau.data.ScreenOrientation

/** screen rotation, in one place instead of twelve times in the manifest. `PLAN.md` 4.2. */
object Orientation {

    fun requested(orientation: ScreenOrientation): Int = when (orientation) {
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // SENSOR and not UNSPECIFIED: automatic must also turn while the system rotation
        // lock is off, otherwise the choice has no effect.
        ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }
}
