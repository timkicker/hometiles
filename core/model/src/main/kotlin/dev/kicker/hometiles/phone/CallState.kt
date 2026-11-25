package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.AudioRoute

enum class CallStatus { RINGING, DIALING, CONNECTING, ACTIVE, HOLDING, DISCONNECTING, DISCONNECTED, OTHER }

enum class CallAction {
    ANSWER,
    REJECT,
    HANG_UP,
    HOLD,
    UNHOLD,
    MUTE,
    UNMUTE,
    SPEAKER,

    /** without this, "speaker" only ever switched on and stayed on until hanging up. */
    SPEAKER_OFF,

    /**
     * takes the place of [SPEAKER] **once a bluetooth device is there**: with three routes a
     * toggle no longer works, and a fourth button row does not fit on three inches. without
     * bluetooth the toggle stays, because an extra step for the common case is a bad trade.
     */
    AUDIO,

    /**
     * takes the place of [HOLD]: while someone waits on the line, "hold" is the wrong
     * question. without it the held call was unreachable and no button led back.
     */
    SWITCH,

    /**
     * both calls into one conference (user mail 25.11.2026: only switching was there). it
     * takes the place of [KEYPAD] while two calls wait: a sixth row does not fit the small
     * phone, and once merged the conference is one call and the keypad is back.
     */
    MERGE,
    KEYPAD,

    /** turn the call down and say why in a short message. */
    REJECT_WITH_TEXT,
}

data class CallView(
    val status: CallStatus,
    val number: String,
    val name: String?,
    val photoUri: String? = null,
    val startedAtMillis: Long?,
    val muted: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    /** with bluetooth the toggle becomes a choice. */
    val bluetoothAvailable: Boolean = false,
    /**
     * name or number of the second call.
     *
     * this used to be a boolean that was set and never read, so a second call passed the
     * surface without trace: the screen showed only the new caller, and that someone else
     * was still on the line stood nowhere.
     */
    val otherName: String? = null,
    val otherHeld: Boolean = false,
    /** telecom says the two calls can become one; only the network knows, so it is asked. */
    val canMerge: Boolean = false,
)

/**
 * which of several calls is in front.
 *
 * the order is the order of the decision that is due: ringing (answer or not?) before the
 * running call before the held one. showing the most recently changed one meant sometimes
 * the one and sometimes the other.
 */
object CallForeground {

    /**
     * [children] marks the members of a conference, one flag per call. telecom reports a
     * conference as the conference itself **plus** every member, each as a call of its own;
     * a member on the screen would be the same conversation shown twice. an empty list means
     * no conference is running, which is the ordinary case.
     */
    fun pick(states: List<CallStatus>, children: List<Boolean> = emptyList()): Int? {
        val shown = states.indices.filterNot { children.getOrElse(it) { false } }
        if (shown.isEmpty()) return null
        return shown.firstOrNull { states[it] == CallStatus.RINGING }
            ?: shown.firstOrNull { states[it] == CallStatus.ACTIVE }
            ?: shown.first()
    }

    /**
     * the other call beside the one in front, or null while there is only one.
     *
     * conference members are skipped here too: naming one of them as the second call told
     * the user somebody else was waiting, when it was the same conversation.
     */
    fun other(children: List<Boolean>, front: Int): Int? =
        children.indices.firstOrNull { it != front && !children[it] }
}

/**
 * which buttons appear in which state.
 *
 * this is where a mistake really hurts: a "hang up" on a ringing call looks like "reject",
 * and a missing "answer" makes the phone useless. hence one table here instead of scattered
 * conditions in the surface.
 */
object CallActions {

    fun availableFor(view: CallView): List<CallAction> = when (view.status) {
        // a withheld number cannot be written to, and a button that promises a message and
        // then sends none is worse than no button. see `PhoneNumbers.isDialable`.
        CallStatus.RINGING -> listOfNotNull(
            CallAction.ANSWER,
            CallAction.REJECT,
            CallAction.REJECT_WITH_TEXT.takeIf { PhoneNumbers.isDialable(view.number) },
        )

        CallStatus.DIALING, CallStatus.CONNECTING -> listOf(
            CallAction.HANG_UP,
            audioRow(view),
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
        )

        CallStatus.ACTIVE -> listOf(
            CallAction.HANG_UP,
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
            audioRow(view),
            if (view.otherHeld) CallAction.SWITCH else CallAction.HOLD,
            if (view.otherHeld && view.canMerge) CallAction.MERGE else CallAction.KEYPAD,
        )

        CallStatus.HOLDING -> listOf(CallAction.HANG_UP, CallAction.UNHOLD)

        CallStatus.DISCONNECTING, CallStatus.DISCONNECTED, CallStatus.OTHER -> emptyList()
    }

    /**
     * with all five rows the keypad was left a strip of five dp, so entering a number during
     * a call ("press 1 for english") was impossible. two rows stay: hang up, because it
     * matters most, and the keypad itself, so the way back stays visible.
     */
    fun whileKeypad(view: CallView): List<CallAction> =
        availableFor(view).filter { it == CallAction.HANG_UP || it == CallAction.KEYPAD }

    private fun audioRow(view: CallView): CallAction = when {
        view.bluetoothAvailable -> CallAction.AUDIO
        view.audioRoute == AudioRoute.SPEAKER -> CallAction.SPEAKER_OFF
        else -> CallAction.SPEAKER
    }

    /** null until connected. */
    fun durationSeconds(view: CallView, nowMillis: Long): Long? {
        val started = view.startedAtMillis ?: return null
        if (view.status != CallStatus.ACTIVE && view.status != CallStatus.HOLDING) return null
        return ((nowMillis - started) / 1000L).coerceAtLeast(0L)
    }

    /** mm:ss, and h:mm:ss from an hour on. */
    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
        else "%d:%02d".format(minutes, secs)
    }

    /**
     * name, else number, else [unknown].
     *
     * a hard-coded "?" used to fill half the screen for a withheld number, which is one of
     * the commonest cases: that looks like a fault of the app, not like information about
     * the caller.
     */
    fun headline(view: CallView, unknown: String): String =
        view.name?.takeIf { it.isNotBlank() }
            ?: PhoneNumbers.forDisplay(view.number).takeIf { it.isNotBlank() }
            ?: unknown
}

/**
 * what stands over a conference. it has no number of its own, so without this the screen
 * said "unknown" while two people were on the line (emulator, 25.11.2026).
 */
object ConferenceName {

    /** null without members, so the ordinary fallback still applies. */
    fun of(members: List<String>): String? =
        members.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
