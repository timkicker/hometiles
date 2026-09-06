package dev.kicker.hometiles.settings

import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.ScreenKind
import dev.kicker.hometiles.security.Pin

/**
 * "reset everything" from `PLAN.md` 4.9, the one step in this app that cannot be undone.
 *
 * the confirmation counts what disappears instead of asking "are you sure": a number makes a
 * warning true, and "are you sure" gets tapped away unread.
 */
object Reset {

    data class Losses(
        val screens: Int,
        val tiles: Int,
        val folders: Int,
        val hasPin: Boolean,
    )

    fun losses(config: LauncherConfig): Losses {
        val folders = config.screens.count { it.kind == ScreenKind.FOLDER }
        return Losses(
            screens = config.screens.size - folders,
            tiles = config.screens.sumOf { it.tileCount },
            folders = folders,
            hasPin = Pin.usable(config.security.pin),
        )
    }

    /**
     * every embedded widget id. without releasing these the widget host would keep them for
     * good, and the providing app would feed a widget nobody can see.
     */
    fun widgetIds(config: LauncherConfig): List<Int> = config.screens
        .flatMap { it.cells }
        .mapNotNull { (it.button.action as? ButtonAction.Widget)?.widgetId }

    /** as after installation, `wizardDone = false` included, or one would face a strange home screen. */
    fun fresh(): LauncherConfig = LauncherConfig()
}
