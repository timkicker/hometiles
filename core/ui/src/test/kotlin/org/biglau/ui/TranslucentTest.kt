package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a half-transparent colour is a new colour - and nobody has checked that one.
 *
 * `ContrastTest` and `SurfaceContrastTest` compute with the tokens. a `.copy(alpha = ...)`
 * appears in no palette: it comes into being while drawing, out of two colours. all seven
 * places were measured on 04.09.2026 and two failed - the empty pin dot's border at 0,35
 * (light 2,23, contrast 2,64, under 3,0) and the "tap to close" hint at 0,7 (light 6,92,
 * just under the 7,0 for type on the background).
 *
 * two others were measured and **stay**: the tracks under the battery bar and under the
 * signal steps reach 1,7 to 2,1 against their ground, but the statement sits in the
 * difference between filled and empty (3,4 to 9,3). a track is not a sign but the space a
 * sign can take.
 */
class TranslucentTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    private fun over(front: Long, back: Long, share: Float): Long {
        var result = 0xFFL shl 24
        listOf(16, 8, 0).forEach { shift ->
            val f = (front shr shift) and 0xFF
            val b = (back shr shift) and 0xFF
            result = result or (Math.round(share * f + (1 - share) * b).toLong() shl shift)
        }
        return result
    }

    private val themes = listOf(ThemeName.DARK, ThemeName.LIGHT, ThemeName.HIGH_CONTRAST)

    @Test
    fun `the empty dot's border can be found`() {
        themes.forEach { theme ->
            val palette = paletteFor(theme, false)
            val fill = palette.emptyTile.argb()
            val border = over(palette.onBackground.argb(), fill, DOT_BORDER)
            val ratio = contrastRatio(border, fill)
            assertTrue(
                ("%s: the empty pin dot's border reaches only %.2f:1 - and its fill stands " +
                    "quiet against the background (%.2f:1), so the border carries the dot " +
                    "alone").format(
                    theme,
                    ratio,
                    contrastRatio(fill, palette.background.argb()),
                ),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
    }
}
