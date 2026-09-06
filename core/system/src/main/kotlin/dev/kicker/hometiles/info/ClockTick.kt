package dev.kicker.hometiles.info

/**
 * when the clock must be redrawn next.
 *
 * waking every second costs noticeable power on a 2000 mAh battery, and the tile shows no
 * seconds anyway. counted to the next full minute rather than plainly 60 seconds, or the
 * display slowly drifts out of step.
 */
object ClockTick {

    private const val MINUTE = 60_000L

    fun millisUntilNextMinute(epochMillis: Long): Long {
        val intoMinute = ((epochMillis % MINUTE) + MINUTE) % MINUTE
        val remaining = MINUTE - intoMinute
        return if (remaining <= 0L) MINUTE else remaining
    }
}
