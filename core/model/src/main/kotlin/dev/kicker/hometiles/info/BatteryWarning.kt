package dev.kicker.hometiles.info

/** what the battery warning does on a new reading. */
enum class BatteryStep { NOTHING, WARN, RESET }

/**
 * a warning once the battery falls to a chosen level (issue #8: a tablet for someone with
 * dementia, where a relative should hear about it before the tablet goes dark).
 *
 * once per discharge: the level passes the mark on every reading below it, and a text to the
 * relatives with every percent would be a flood. charging arms it again.
 */
object BatteryWarning {

    /** the levels one can pick. 10 was the one asked for in the issue. */
    val LEVELS = listOf(10, 15, 20, 30)
    const val DEFAULT_LEVEL = 10

    fun step(percent: Int?, charging: Boolean, level: Int, warned: Boolean): BatteryStep = when {
        charging -> if (warned) BatteryStep.RESET else BatteryStep.NOTHING
        // an unknown level is no reason to alarm anybody.
        percent == null -> BatteryStep.NOTHING
        !warned && percent <= level -> BatteryStep.WARN
        else -> BatteryStep.NOTHING
    }

    /** a stored level that is not on offer any more falls back instead of never firing. */
    fun clamp(level: Int): Int = if (level in LEVELS) level else DEFAULT_LEVEL
}
