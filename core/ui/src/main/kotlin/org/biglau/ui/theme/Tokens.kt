package org.biglau.ui.theme

import kotlin.math.pow

/**
 * colour tokens as argb longs, free of compose types so the contrast check runs in a plain
 * jvm test: `PLAN.md` 3.3 wants the values measured, not claimed.
 */
object Tokens {

    // --- dark, the main theme ---
    const val DARK_BACKGROUND = 0xFF0A0A0AL
    const val DARK_ON_BACKGROUND = 0xFFFFFFFFL
    const val DARK_EMPTY_TILE = 0xFF161616L

    /** on an empty tile the border is the only sign of a slot, so it must reach the area threshold. */
    const val DARK_EMPTY_TILE_BORDER = 0xFF666666L
    const val DARK_ACCENT = 0xFF4C9AFFL

    /** the dark theme's accent is bright: dark text belongs on it. */
    const val DARK_ON_ACCENT = 0xFF0A0A0AL
    const val DARK_DANGER = 0xFFFF5252L

    /** the red is bright enough that white text fails on it. */
    const val DARK_ON_DANGER = 0xFF0A0A0AL

    /**
     * the same red as text on the background, and therefore a different value.
     *
     * [DARK_DANGER] is an area colour and fine with [DARK_ON_DANGER] on it; as text on the
     * background it reached 6.20:1, under [MIN_TEXT_ON_BACKGROUND] of 7.0.
     */
    const val DARK_DANGER_TEXT = 0xFFFF8080L

    /** six tile hues on one contrast level, so none weighs more than another. */
    val DARK_TILES = listOf(
        0xFF2763CBL, // blue
        0xFF15773EL, // green
        0xFF9539CBL, // violet
        0xFF99561CL, // orange
        0xFF187086L, // turquoise
        0xFFC22365L, // magenta
    )

    // --- light ---
    const val LIGHT_BACKGROUND = 0xFFFAFAFAL
    const val LIGHT_ON_BACKGROUND = 0xFF111111L
    const val LIGHT_EMPTY_TILE = 0xFFE8EAECL
    /**
     * this border has *two* grounds: the background outside and the tile fill inside.
     * `#8A8A8A` reached only 2.86:1 inside; `#7A7A7A` makes 3.56 inside and 4.11 outside.
     */
    const val LIGHT_EMPTY_TILE_BORDER = 0xFF7A7A7AL
    const val LIGHT_ACCENT = 0xFF1565C0L
    const val LIGHT_ON_ACCENT = 0xFFFFFFFFL
    const val LIGHT_DANGER = 0xFFC62828L

    /** warning text on a light ground, see [DARK_DANGER_TEXT]. */
    const val LIGHT_DANGER_TEXT = 0xFF8A1A1AL
    const val LIGHT_ON_DANGER = 0xFFFFFFFFL

    val LIGHT_TILES = listOf(
        0xFF1B4FA8L,
        0xFF106032L,
        0xFF7A2BA8L,
        0xFF7D4416L,
        0xFF12596BL,
        0xFF9E1B51L,
    )

    // --- contrast ---
    const val CONTRAST_BACKGROUND = 0xFF000000L
    const val CONTRAST_INK = 0xFFFFEB3BL
    const val CONTRAST_DANGER = 0xFFFF5252L

    /** warning text in the contrast theme, see [DARK_DANGER_TEXT]. */
    const val CONTRAST_DANGER_TEXT = 0xFFFF8080L
    const val CONTRAST_ON_ACCENT = 0xFF000000L
    const val CONTRAST_ON_DANGER = 0xFF000000L

    // --- thresholds from `PLAN.md` 3.3 ---
    /** tile against background. */
    const val MIN_TILE_ON_BACKGROUND = 3.0

    /** label on a tile, AAA for large text. */
    const val MIN_LABEL_ON_TILE = 4.5

    /** text outside a tile, AAA for normal text. */
    const val MIN_TEXT_ON_BACKGROUND = 7.0
}

/** relative luminance, WCAG 2.1. */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val raw = ((argb shr shift) and 0xFF).toDouble() / 255.0
        return if (raw <= 0.04045) raw / 12.92 else ((raw + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** contrast ratio, WCAG 2.1, always >= 1.0. */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}
