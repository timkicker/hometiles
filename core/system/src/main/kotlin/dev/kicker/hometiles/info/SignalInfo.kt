package dev.kicker.hometiles.info

data class SignalReading(
    /** 0 to 4, like `SignalStrength.getLevel()`; -1 means unknown. */
    val level: Int,
    /** without `READ_PHONE_STATE` we know nothing, which is not the same as no sim. */
    val mayRead: Boolean = true,
    val hasSim: Boolean,
    val inService: Boolean,
    val roaming: Boolean,
    /** 4G, LTE, 3G ... or empty when the network reports nothing. */
    val networkType: String,
)

/**
 * what a signal tile says.
 *
 * four states, because each asks for a different move: insert a card, go somewhere else,
 * step to the window, or nothing.
 */
object SignalInfo {

    const val MAX_LEVEL = 4

    enum class State { NO_PERMISSION, NO_SIM, NO_SERVICE, WEAK, OK }

    fun stateOf(reading: SignalReading): State = when {
        !reading.mayRead -> State.NO_PERMISSION
        !reading.hasSim -> State.NO_SIM
        !reading.inService -> State.NO_SERVICE
        reading.level <= 1 -> State.WEAK
        else -> State.OK
    }

    /** bars to fill, 0 to 4. unknown counts as empty. */
    fun bars(reading: SignalReading): Int =
        if (!reading.mayRead || !reading.hasSim || !reading.inService) {
            0
        } else {
            reading.level.coerceIn(0, MAX_LEVEL)
        }

    /** the network type, prefixed with R while roaming, because roaming costs money. */
    fun caption(reading: SignalReading): String {
        val kind = reading.networkType.trim()
        return when {
            !reading.mayRead || !reading.hasSim || !reading.inService -> ""
            reading.roaming && kind.isNotEmpty() -> "R $kind"
            reading.roaming -> "R"
            else -> kind
        }
    }
}
