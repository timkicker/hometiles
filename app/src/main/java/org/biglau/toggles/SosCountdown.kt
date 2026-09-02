package org.biglau.toggles

/**
 * Der Countdown vor dem Absenden.
 *
 * Er ist kein Schmuck: eine Notruf-Kachel wird auch versehentlich getroffen, und eine SMS
 * an drei Menschen laesst sich nicht zurueckholen. Gleichzeitig darf er nicht so lang sein,
 * dass er im Ernstfall im Weg steht.
 */
object SosCountdown {

    const val DEFAULT_SECONDS = 5
    const val MAX_SECONDS = 10

    fun clamp(seconds: Int): Int = seconds.coerceIn(0, MAX_SECONDS)

    /** Verbleibende Sekunden; 0 heisst: jetzt senden. */
    fun remaining(startedAtMillis: Long, nowMillis: Long, seconds: Int): Int {
        val total = clamp(seconds)
        if (total == 0) return 0
        val elapsed = ((nowMillis - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        return (total - elapsed).coerceAtLeast(0)
    }

    /** Ist der Ablauf ueberhaupt eingerichtet? Ohne Nummern gibt es nichts zu senden. */
    fun isConfigured(numbers: List<String>): Boolean = numbers.any { it.isNotBlank() }
}
