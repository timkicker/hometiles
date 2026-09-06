package dev.kicker.hometiles.phone

/**
 * handling of phone numbers, emergency ones above all.
 *
 * some devices refuse an emergency call from a third-party app, others reroute it silently.
 * so a rule instead of a setting: a number that looks *anything* like an emergency number is
 * never dialled by hometiles but handed to the system dialler. one keypress more beats a
 * swallowed emergency call.
 *
 * the list supplements the platform check, it does not replace it.
 */
object PhoneNumbers {

    /** deliberately generous; these should hold without network and without a sim. */
    val WELL_KNOWN_EMERGENCY = setOf(
        "112", "911", "999", "000", "110", "118", "119", "115", "122", "133", "144", "08",
    )

    fun clean(number: String): String =
        number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    /** a false positive costs a keypress in the system dialler, a false negative can cost a call. */
    fun looksLikeEmergency(number: String, platformSaysYes: Boolean = false): Boolean {
        if (platformSaysYes) return true
        val digits = clean(number).removePrefix("+")
        if (digits.isEmpty()) return false
        return digits in WELL_KNOWN_EMERGENCY
    }

    fun isDialable(number: String): Boolean = clean(number).any { it.isDigit() }

    /** the region a number without a country code belongs to (iso, e.g. "at"); null means unknown. */
    @Volatile
    var region: String? = null

    /**
     * how the system writes the number, or null when it cannot.
     *
     * injected from outside, which is why this file can live in the pure kotlin module:
     * android carries the dialling-code tables, but `PhoneNumberUtils` is framework.
     * `SystemNumbers.install` in `core:system` hooks up the real thing.
     *
     * the default is **null**, the fall back to plain grouping. that is the honest default:
     * without the system parts one gets readable blocks, not a guessed country code.
     */
    @Volatile
    var systemFormat: (String, String?) -> String? = { _, _ -> null }

    /**
     * the number as it appears on screen.
     *
     * **the system is asked first.** without its tables the grouping goes wrong: an austrian
     * mobile number showed as "+436 804 ..." on the home screen, and "+436" is not a country.
     * whoever copies or reads that out gets it wrong.
     *
     * not every sender is a number, though. banks and parcel services arrive as letter ids
     * ("ADAC"), and [clean] leaves nothing of those, which produced an **empty line** in the
     * message list. so if nothing survives cleaning, the raw text stands.
     */
    fun forDisplay(number: String): String {
        val cleaned = clean(number)
        if (cleaned.isEmpty()) return number.trim()
        if (cleaned.length <= 6) return cleaned
        systemFormat(cleaned, region)?.takeIf { it.isNotBlank() }?.let { return it }
        return grouped(cleaned)
    }

    /**
     * the same number for **reading aloud**: one character at a time.
     *
     * a phone number is not a number. read as ordinary text a screen reader turns "123" into
     * "one hundred and twenty-three", and nobody can check that against a card. spaces
     * between digits is the form every reader treats separately. grouping gaps fall away;
     * they are there for the eye, and the ear gets a pause after each digit anyway.
     *
     * letter ids stay as they are: they are a word and get read as one.
     */
    fun forSpeech(number: String): String {
        val cleaned = clean(number)
        if (cleaned.isEmpty()) return number.trim()
        return cleaned.map { it.toString() }.joinToString(" ")
    }

    /** the fallback: blocks of three, the plus stays in front. */
    private fun grouped(cleaned: String): String {
        val plus = cleaned.startsWith("+")
        val body = if (plus) cleaned.drop(1) else cleaned
        val blocks = body.chunked(3).joinToString(" ")
        return if (plus) "+$blocks" else blocks
    }
}
