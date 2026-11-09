package dev.kicker.hometiles.a11y

/**
 * drops the second press a worn key makes by itself.
 *
 * issue #2: three flip phones with keys that sometimes send one press twice. in a number that
 * is a wrong digit, on the select key a second action, and a tone sent during a call cannot
 * be taken back. a press of the same key that comes down within [WINDOW_MS] of the last one
 * is dropped, its release too, or the release alone would still click.
 *
 * held keys repeat with a repeat count above zero and are left alone: that is the system's
 * own repeat, not a bounce.
 */
class KeyBounce(private val windowMs: Long = WINDOW_MS) {

    private var lastKey = NONE
    private var lastDownAt = Long.MIN_VALUE
    private var swallowed = NONE

    /** true when the event should reach the screen. */
    fun accept(keyCode: Int, down: Boolean, repeatCount: Int, eventTime: Long): Boolean {
        if (!down) {
            if (keyCode == swallowed) {
                swallowed = NONE
                return false
            }
            return true
        }
        if (repeatCount > 0) return keyCode != swallowed
        val bounce = keyCode == lastKey && eventTime - lastDownAt in 0 until windowMs
        lastKey = keyCode
        lastDownAt = eventTime
        if (bounce) {
            swallowed = keyCode
            return false
        }
        return true
    }

    companion object {
        /**
         * a person pressing one key twice on purpose takes longer: "11" in a number lands
         * about a fifth of a second apart and more. a bounce comes within a few hundredths.
         */
        const val WINDOW_MS = 150L
        private const val NONE = -1
    }
}
