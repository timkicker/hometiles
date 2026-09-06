package dev.kicker.hometiles.ui.theme

import dev.kicker.hometiles.data.ThemeName

/**
 * a screen's background. `PLAN.md` 4.1 offers theme colour, solid colour, image or none.
 *
 * no image, on purpose: behind a photo no contrast can be planned, and every tile colour
 * check is made against a known ground. a solid colour says at a glance which screen this
 * is, faster than the name at the top is read.
 */
object ScreenBackground {

    /**
     * how strongly the colour tints the theme background.
     *
     * twelve percent: the theme background has only 3.49:1 of air against the tile colours,
     * and three to one is where a tile edge stops being visible. at 22 percent it was 2.64.
     */
    private const val STRENGTH = 0.12f

    /** tints of the theme background, not finished colours: a fixed dark list broke the light theme. */
    private val HUES: List<Long> = listOf(
        0xFF3B6FE0, // blue
        0xFF7A4FD0, // violet
        0xFF1E8A5A, // green
        0xFFB03A46, // red
        0xFFB08A20, // ochre
    )

    /**
     * no choice in the high contrast theme: there background and tiles carry the same
     * colour and the tiles stand by their border, which is why anyone picks that theme.
     */
    fun offersChoices(theme: ThemeName): Boolean = theme != ThemeName.HIGH_CONTRAST

    fun choicesFor(theme: ThemeName, systemIsDark: Boolean): List<Long> {
        if (!offersChoices(theme)) return emptyList()
        val ground = paletteFor(theme, systemIsDark).background.value.toLong() shr 32
        return HUES.map { blend(ground, it, STRENGTH) }
    }

    /**
     * black or white on an arbitrary colour, whichever stands better.
     *
     * used by the colour picker, where the swatches carry their own name. the home screen
     * does not use it: at twelve percent tint ([STRENGTH]) the theme ink still reaches 15.0
     * to 18.6 to one on every offered ground.
     */
    fun inkFor(background: Long): Long =
        if (contrastRatio(background, 0xFFFFFFFF) >= contrastRatio(background, 0xFF000000)) {
            0xFFFFFFFF
        } else {
            0xFF000000
        }

    private fun blend(base: Long, hue: Long, amount: Float): Long {
        fun channel(shift: Int): Long {
            val a = (base shr shift) and 0xFF
            val b = (hue shr shift) and 0xFF
            return (a + (b - a) * amount).toLong().coerceIn(0, 255)
        }
        return (0xFFL shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
