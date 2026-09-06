package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.Appearance
import dev.kicker.hometiles.data.ClockDisplay

/**
 * can the user still see the time and the battery? `PLAN.md` 4.2.
 *
 * full screen removes the system status bar; the app header carries both while it is on.
 * turning both off is allowed, but it is the kind of setting one hits by accident and then
 * blames the phone for.
 */
object StatusVisibility {

    fun showsTime(appearance: Appearance): Boolean = when {
        !appearance.fullScreen -> true
        !appearance.showHeader -> false
        else -> appearance.clock != ClockDisplay.OFF
    }

    fun showsBattery(appearance: Appearance): Boolean =
        !appearance.fullScreen || appearance.showHeader

    /** a missing one earns a hint beside the setting, not a ban. */
    fun warns(appearance: Appearance): Boolean =
        !showsTime(appearance) || !showsBattery(appearance)
}
