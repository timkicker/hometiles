package dev.kicker.hometiles.ui.theme

import kotlin.math.abs

/**
 * a freely chosen tile colour that cannot become unreadable.
 *
 * `PLAN.md` 4.2 promises a free tile colour and 3.3 says contrast is a function here, not
 * a taste. both hold only if the person picks the *hue* and the app computes the
 * lightness, which is what readability hangs on.
 *
 * the hue is stored, not the finished argb value: a computed value belongs to the theme it
 * was computed in, and the same colour can miss the threshold in the other one.
 */
object FreeTileColor {

    /**
     * does this theme use tile colours at all?
     *
     * in the contrast theme all six palette slots carry the background colour; `PLAN.md`
     * 3.3 says tile colours are ignored there, and a free hue must be ignored likewise.
     */
    fun themeUsesTileColours(tiles: List<Long>): Boolean = tiles.distinct().size > 1

    /** hues to choose from, 15 degrees apart. */
    const val HUE_COUNT = 24

    val hues: List<Float> = (0 until HUE_COUNT).map { it * (360f / HUE_COUNT) }

    /**
     * the weight this theme's tiles sit on.
     *
     * `PLAN.md` 3.3 puts all six on the same contrast level, so no tile looks heavier than
     * another and only the hue tells them apart.
     */
    fun targetLuminance(tiles: List<Long>): Double = tiles.map(::relativeLuminance).average()

    /**
     * the shade of this hue that holds both thresholds from 3.3, tile against background
     * and label on tile, and sits as close as possible to [targetLuminance].
     *
     * searched from saturated towards pale: a saturated colour that holds is easier to tell
     * apart, and telling tiles apart is the whole point.
     */
    fun forHue(hue: Float, background: Long, onTile: Long, targetLuminance: Double): Long {
        val tone = ((hue % 360f) + 360f) % 360f
        var best: Long? = null
        var bestDistance = Double.MAX_VALUE
        for (step in 0..11) {
            val saturation = 0.72f - step * 0.05f
            if (saturation < 0.15f) break
            for (hundredth in 10..80) {
                val colour = hsl(tone, saturation, hundredth / 100f)
                val onBackground = contrastRatio(colour, background)
                val labelOnIt = contrastRatio(onTile, colour)
                if (onBackground < Tokens.MIN_TILE_ON_BACKGROUND) continue
                if (labelOnIt < Tokens.MIN_LABEL_ON_TILE) continue
                // hit the palette weight, do not maximise the thresholds: maximising drove
                // every hue towards black in the light theme, where both ratios grow the
                // darker the tile gets. measured 14.5:1 against the intended 3.5.
                val distance = abs(relativeLuminance(colour) - targetLuminance)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = colour
                }
            }
            if (best != null) return best
        }
        // does not happen for the built themes, FreeTileColorTest checks every hue in each.
        // kept anyway: a tile without a colour would be worse than one in the ground colour.
        return background
    }

    /** hsl to opaque argb. */
    private fun hsl(hue: Float, saturation: Float, lightness: Float): Long {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r, g, b) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun channel(value: Float): Long =
            ((value + m).coerceIn(0f, 1f) * 255f).toInt().toLong()
        return 0xFF000000L or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}

/** a compose colour as the opaque argb value [contrastRatio] expects. */
fun androidx.compose.ui.graphics.Color.toArgbLong(): Long =
    0xFF000000L or
        ((red * 255f).toInt().toLong() shl 16) or
        ((green * 255f).toInt().toLong() shl 8) or
        (blue * 255f).toInt().toLong()
