package org.biglau.toggles

/** the countdown before sending. an emergency tile gets hit by accident, and a text cannot be recalled. */
object SosCountdown {

    const val DEFAULT_SECONDS = 5
    const val MAX_SECONDS = 10

    fun clamp(seconds: Int): Int = seconds.coerceIn(0, MAX_SECONDS)

    /** seconds left; 0 means send now. */
    fun remaining(startedAtMillis: Long, nowMillis: Long, seconds: Int): Int {
        val total = clamp(seconds)
        if (total == 0) return 0
        val elapsed = ((nowMillis - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        return (total - elapsed).coerceAtLeast(0)
    }

    fun isConfigured(numbers: List<String>): Boolean = numbers.any { it.isNotBlank() }
}
