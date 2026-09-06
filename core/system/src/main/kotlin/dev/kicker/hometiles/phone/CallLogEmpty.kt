package dev.kicker.hometiles.phone

enum class EmptyCallLog {
    NO_CALLS,

    /** there were calls, but every one is of a hidden type. */
    HIDDEN_BY_TYPE,

    /** there were calls, but the filter stands on "missed only". */
    NO_MISSED,
}

/**
 * an empty list has three different reasons and they must not share one sentence.
 *
 * "no calls yet" stood there even with seven calls in the log and every type hidden in the
 * settings. that is not information but a false statement, and nothing led out of it: with
 * an empty list the hint and the button below it disappear too.
 */
object CallLogEmpty {

    /** `null` when there is something to see. */
    fun reason(
        all: List<CallGroup>,
        missedOnly: Boolean,
        allowed: Set<CallDirection>,
    ): EmptyCallLog? {
        val visible = CallLogGrouping.visible(
            if (missedOnly) CallLogGrouping.onlyMissed(all) else all,
            allowed,
        )
        if (visible.isNotEmpty()) return null
        if (all.isEmpty()) return EmptyCallLog.NO_CALLS
        // the user's order, not the code's: the type choice sits in the settings and is
        // forgotten, the "missed only" filter is a button above the list and is visible.
        // so the hidden reason comes first.
        if (CallLogGrouping.visible(all, allowed).isEmpty()) return EmptyCallLog.HIDDEN_BY_TYPE
        return EmptyCallLog.NO_MISSED
    }
}
