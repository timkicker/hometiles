package dev.kicker.hometiles.phone

/** what the back key does in the dialler. */
enum class DialerBackStep { CLOSE_QUESTION, TO_KEYPAD, LEAVE }

/**
 * back in steps, as on the home screen: first an open question, then the list.
 *
 * the list goes back to the keypad only when the keypad is where one came from. opened from
 * the call log or missed calls tile, back on the list led to a keypad nobody had asked for
 * (issue #2, a flip phone where that tile is the way in). the speed dial page still goes back
 * to the keypad, it is only reached from there.
 */
object DialerBack {

    fun step(
        questionOpen: Boolean,
        onKeypad: Boolean,
        onLog: Boolean,
        openedOnLog: Boolean,
    ): DialerBackStep = when {
        questionOpen -> DialerBackStep.CLOSE_QUESTION
        onKeypad || (onLog && openedOnLog) -> DialerBackStep.LEAVE
        else -> DialerBackStep.TO_KEYPAD
    }
}
