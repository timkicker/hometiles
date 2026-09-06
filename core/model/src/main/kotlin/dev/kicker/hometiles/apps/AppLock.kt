package dev.kicker.hometiles.apps

import dev.kicker.hometiles.data.AppsConfig
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.security.Pin

/**
 * the app lock. `PLAN.md` 4.5: which apps start without the pin.
 *
 * an allow list, not a block list: a block list would have to name every app on the phone
 * and would be incomplete after the next installation.
 *
 * that is also what makes it dangerous, so switching it on never stands alone:
 * [initialAllowance] fills the list with whatever sits on the tiles. whoever switches it on
 * and then faces a phone where nothing opens has not set up a lock but locked themselves out.
 */
object AppLock {

    fun needsPin(config: LauncherConfig, key: String, packageName: String): Boolean {
        if (!Pin.protects(config.security.pin, config.apps.lockOthers)) return false
        return key !in config.apps.allowed && packageName !in config.apps.allowed
    }

    enum class Step {
        /** off: every app starts without a pin. */
        TURN_OFF,

        /** on: something is allowed and stays open. */
        TURN_ON,

        /**
         * show the allow list first instead of switching on.
         *
         * with no app on any tile [initialAllowance] fills nothing, and the switch would
         * land in a state where **nothing** opens without the pin.
         */
        CHOOSE_FIRST,
    }

    /** what the line under the switch says, worked out rather than claimed. */
    fun wouldAllow(config: LauncherConfig): Int =
        if (config.apps.allowed.isNotEmpty()) {
            config.apps.allowed.size
        } else {
            initialAllowance(config).size
        }

    fun toggle(config: LauncherConfig): Step = when {
        config.apps.lockOthers -> Step.TURN_OFF
        wouldAllow(config) > 0 -> Step.TURN_ON
        else -> Step.CHOOSE_FIRST
    }

    fun toggleAllowed(apps: AppsConfig, key: String): AppsConfig =
        if (key in apps.allowed) {
            apps.copy(allowed = apps.allowed - key)
        } else {
            apps.copy(allowed = apps.allowed + key)
        }

    /**
     * allowed automatically: everything on a tile. whoever set up the phone put these within
     * reach on purpose, and locking them afterwards would undo what they just did.
     */
    fun initialAllowance(config: LauncherConfig): Set<String> = config.screens
        .flatMap { it.cells }
        .mapNotNull { cell ->
            // a shortcut leads into an app and lies on a tile just as deliberately. with
            // only `App` here, a shortcut tile would be locked the moment the lock goes on.
            when (val action = cell.button.action) {
                is ButtonAction.App -> "${action.packageName}/${action.activityName}"
                is ButtonAction.Shortcut -> action.packageName
                else -> null
            }
        }
        .toSet()
}
