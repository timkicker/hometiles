package dev.kicker.hometiles.actions

/**
 * the text of the emergency message.
 *
 * separate from sending, because two things can go wrong here that one does not want to try
 * out on a device: a message without a location that looks as if it had one, and coordinates
 * in a notation the recipient cannot tap.
 */
object SosMessage {

    /**
     * the fallback text is passed in, not fixed here: it has to be in the phone's language.
     *
     * [ageNote] is added when the location is **old**. one without an age reads as "here he
     * is now"; if it is in truth from yesterday, help drives to the wrong place and searches
     * there. old and labelled beats none at all.
     */
    fun compose(
        text: String,
        latitude: Double?,
        longitude: Double?,
        fallback: String,
        ageNote: String? = null,
    ): String {
        val body = text.trim().ifEmpty { fallback.trim() }
        if (latitude == null || longitude == null) return body
        val link = mapsLink(latitude, longitude)
        return listOfNotNull(body, link, ageNote?.trim()?.ifEmpty { null }).joinToString("\n")
    }

    /** shorter would be noise, every fix is a few seconds old; longer would pass off a quarter of an hour as current. */
    const val AGE_THRESHOLD_MINUTES = 5L

    /** null means no location or fresh enough. minutes or hours, whichever places it more easily. */
    fun ageNote(minutes: Long?): Pair<AgeUnit, Int>? {
        if (minutes == null || minutes < AGE_THRESHOLD_MINUTES) return null
        if (minutes < 120) return AgeUnit.MINUTES to minutes.toInt()
        return AgeUnit.HOURS to (minutes / 60).toInt()
    }

    enum class AgeUnit { MINUTES, HOURS }

    /** a link every maps app opens, not an app-specific notation. */
    fun mapsLink(latitude: Double, longitude: Double): String =
        "https://maps.google.com/?q=%.5f,%.5f".format(java.util.Locale.US, latitude, longitude)

    /** past 160 characters a text is split, and every part costs; a chain to three numbers adds up. */
    fun partsNeeded(message: String): Int {
        if (message.isEmpty()) return 1
        val perPart = if (message.length <= 160) 160 else 153
        return ((message.length + perPart - 1) / perPart).coerceAtLeast(1)
    }
}
