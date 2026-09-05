package org.biglau.notify

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction

/**
 * which package a tile watches.
 *
 * an app tile watches its own app; phone and messages follow whatever the system holds as
 * default, so the tile keeps blinking right when the user switches their sms app.
 */
object TileNotifications {

    fun watchedPackage(action: ButtonAction, system: SystemPackages): String? = when (action) {
        is ButtonAction.App -> action.packageName
        is ButtonAction.Action -> when (action.builtin) {
            Builtin.MESSAGES -> system.sms
            // MISSED_CALLS and DIALER count the call log, not foreign notices. see badgeFor.
            else -> null
        }
        else -> null
    }

    /** only a tile with a notifying app behind it: a clock would promise something that never comes. */
    fun canBlink(action: ButtonAction): Boolean = when (action) {
        is ButtonAction.App -> true
        is ButtonAction.Action -> action.builtin in setOf(
            Builtin.DIALER,
            Builtin.MESSAGES,
            Builtin.MISSED_CALLS,
        )
        else -> false
    }

    fun badgeFor(
        button: Button,
        counts: Map<String, Int>,
        system: SystemPackages,
        missed: Int = 0,
        unread: Int? = null,
    ): Int {
        if (!button.blink) return 0
        val action = button.action
        if (action is ButtonAction.Action) {
            // both count the call log. the notice about a missed call comes from telecom,
            // not from the dialer app, and the default dialer is BigLau itself once it
            // holds the role, which notifies about messages.
            if (action.builtin == Builtin.MISSED_CALLS) return missed
            if (action.builtin == Builtin.DIALER) return missed
            // null means we may not read: then the notices are the best answer there is.
            if (action.builtin == Builtin.MESSAGES && unread != null) return unread
        }
        val watched = watchedPackage(action, system) ?: return 0
        return counts[watched] ?: 0
    }
}
