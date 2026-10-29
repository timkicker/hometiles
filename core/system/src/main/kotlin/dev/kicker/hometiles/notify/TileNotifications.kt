package dev.kicker.hometiles.notify

import dev.kicker.hometiles.apps.AppKey
import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction

/**
 * which package a tile watches.
 *
 * an app tile watches its own app; phone and messages follow whatever the system holds as
 * default, so the tile keeps blinking right when the user switches their sms app.
 */
object TileNotifications {

    fun watchedPackage(action: ButtonAction, system: SystemPackages): String? = when (action) {
        is ButtonAction.App -> AppKey.notices(action.packageName, action.profile)
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
            Builtin.CALL_LOG,
        )
        else -> false
    }

    fun badgeFor(
        button: Button,
        counts: Map<String, Int>,
        system: SystemPackages,
        missed: Int = 0,
        unread: Int? = null,
        /** waiting voicemail, see NotificationCounts.voicemail. */
        voicemail: Int = 0,
    ): Int {
        if (!button.blink) return 0
        val action = button.action
        if (action is ButtonAction.Action) {
            // both count the call log. the notice about a missed call comes from telecom,
            // not from the dialer app, and the default dialer is HomeTiles itself once it
            // holds the role, which notifies about messages.
            if (action.builtin == Builtin.MISSED_CALLS) return missed
            // the log shows the same calls, and on a flip phone it is often the only way in
            // (issue #2).
            if (action.builtin == Builtin.CALL_LOG) return missed
            // the phone tile is where something waits on the phone: missed calls and, since
            // issue #2, voicemail.
            if (action.builtin == Builtin.DIALER) return missed + voicemail
            // null means we may not read: then the notices are the best answer there is.
            if (action.builtin == Builtin.MESSAGES && unread != null) return unread
        }
        val watched = watchedPackage(action, system) ?: return 0
        return counts[watched] ?: 0
    }
}
