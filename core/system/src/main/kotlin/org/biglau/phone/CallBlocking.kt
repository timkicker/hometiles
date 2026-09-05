package org.biglau.phone

/**
 * blocked numbers (`PLAN.md` 4.6).
 *
 * compared over the last digits, not character by character: the same person arrives as
 * +43 664 1234567, 0043 664 1234567 or 0664 1234567.
 */
object CallBlocking {

    /** length of an austrian number without the area code. */
    const val SUFFIX_DIGITS = 7

    fun key(number: String): String =
        PhoneNumbers.clean(number).filter { it.isDigit() }.takeLast(SUFFIX_DIGITS)

    /**
     * emergency numbers never block (`PLAN.md` 4.6), and neither does a withheld number,
     * which carries no digits and would otherwise block every anonymous caller at once.
     */
    fun isBlocked(number: String, blocked: Collection<String>): Boolean {
        if (PhoneNumbers.looksLikeEmergency(number)) return false
        val wanted = key(number)
        if (wanted.length < SUFFIX_DIGITS) return false
        return blocked.any { key(it) == wanted }
    }

    /**
     * what [CallScreening] asks. incoming only: android asks for outgoing calls too, and a
     * yes there would silently fail a number the user dialled themselves.
     */
    fun blocksIncoming(number: String, incoming: Boolean, blocked: Collection<String>): Boolean =
        incoming && isBlocked(number, blocked)

    fun parse(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { key(it).length >= SUFFIX_DIGITS }
        .distinctBy(::key)

    /** what parsing dropped, so it does not vanish silently. */
    fun rejected(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && key(it).length < SUFFIX_DIGITS }

    fun format(numbers: List<String>): String = numbers.joinToString(", ")
}
