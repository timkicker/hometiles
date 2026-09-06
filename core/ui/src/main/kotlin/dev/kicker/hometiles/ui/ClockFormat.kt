package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.ClockDisplay

/** what the clock shows. `PLAN.md` 4.2: off / time / time+date / time+date+weekday. */
object ClockFormat {

    /** ask `is24HourFormat`; a hard-coded "HH:mm" shows 14:30 next to a header saying 2:30 PM. */
    fun timePattern(twentyFourHour: Boolean): String = if (twentyFourHour) "HH:mm" else "h:mm a"

    /** a clock tile with no time would look broken, so "off" only applies to the header. */
    fun showsTime(display: ClockDisplay, onTile: Boolean): Boolean =
        onTile || display != ClockDisplay.OFF

    fun dateSkeleton(display: ClockDisplay, onTile: Boolean): String? =
        dateSkeletons(display, onTile).firstOrNull()

    /**
     * date *parts*, longest first, not an arrangement: the order comes from the locale via
     * `DateFormat.getBestDateTimePattern`. a ready-made german pattern gave "Wednesday,
     * 2. September" on an english phone.
     *
     * several steps because a 1x1 tile wraps the long name and leaves the bare day number
     * on the second line. the tile measures and falls back; the header starts short.
     */
    fun dateSkeletons(display: ClockDisplay, onTile: Boolean): List<String> = when (display) {
        ClockDisplay.OFF -> emptyList()
        ClockDisplay.TIME -> emptyList()
        ClockDisplay.TIME_DATE -> if (onTile) listOf("dMMMM", "dMMM") else listOf("dMMM")
        ClockDisplay.TIME_DATE_WEEKDAY ->
            if (onTile) listOf("EEEEdMMMM", "EEEdMMM", "dMMM") else listOf("EEEdMMM")
    }

    /**
     * the longest date this pattern yields over a year.
     *
     * twelve months times seven consecutive days covers every weekday and month name with a
     * two-digit day. measuring today's date instead would fit on wednesday and wrap on
     * thursday. character count is an approximation of width, and that is enough here.
     */
    fun longestDate(format: java.text.DateFormat): String {
        val calendar = java.util.Calendar.getInstance()
        var longest = ""
        for (month in 0..11) {
            for (day in 22..28) {
                calendar.set(2024, month, day)
                val text = format.format(calendar.time)
                if (text.length > longest.length) longest = text
            }
        }
        return longest
    }

    val SCALES = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    const val SCALE_MIN = 0.75f
    const val SCALE_MAX = 2.0f

    fun scale(value: Float): Float = value.coerceIn(SCALE_MIN, SCALE_MAX)

    /** grows with **both** factors; counting only the clock scale cut the date line in half. */
    fun headerHeightDp(hasDate: Boolean, textScale: Float, clockScale: Float): Float =
        (if (hasDate) 78f else 54f) * textScale * scale(clockScale)

    /** rough width of a bold sans-serif character, relative to the font size. */
    private const val CHARACTER_WIDTH = 0.62f

    /** "100 %" plus bolt plus gap. it grows with the text scale, and at 200 % it eats the row. */
    fun batteryWidthDp(textScale: Float): Float =
        "100 %".length * CHARACTER_WIDTH * 20f * textScale + 20f * textScale + 8f

    /**
     * the clock may end up smaller than asked for: with `softWrap = false` it drew straight
     * over the battery reading at 200 %. a clock you can read beats one at the wanted size.
     */
    fun clockSizeSp(text: String, availableDp: Float, textScale: Float, clockScale: Float): Float {
        val wanted = 26f * textScale * scale(clockScale)
        val fitting = availableDp / (text.length.coerceAtLeast(1) * CHARACTER_WIDTH)
        return minOf(wanted, fitting).coerceAtLeast(14f)
    }
}
