package dev.kicker.hometiles.toggles

import dev.kicker.hometiles.phone.PhoneNumbers

/**
 * the emergency number list, entered as one line because that beats a list with a plus
 * button on three inches. what slips through here is a message that never arrives.
 */
object SosNumbers {

    const val MAX = 5

    /** comma, semicolon and newline all separate, so nobody has to guess which one counts. */
    fun parse(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && PhoneNumbers.isDialable(it) }
        .map { PhoneNumbers.clean(it) }
        .distinct()
        .take(MAX)

    fun format(numbers: List<String>): String = numbers.joinToString(", ")

    /** what was unusable, so the screen can say so instead of dropping it silently. */
    fun rejected(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && !PhoneNumbers.isDialable(it) }
}
