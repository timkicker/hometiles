package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * pale does not mean invisible.
 *
 * nobody had done the arithmetic behind `PALE` until 04.09.2026: at 0.4 the light theme
 * reached 2.59, under the threshold. so this rule composites the layers itself instead of
 * believing a number; why the threshold is the one for areas stands at `PALE`.
 */
class PaleButtonTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    /** what becomes of [front] when it lies on [behind] with [share] coverage. */
    private fun over(front: Long, behind: Long, share: Float): Long {
        var result = 0xFFL shl 24
        listOf(16, 8, 0).forEach { shift ->
            val f = (front shr shift) and 0xFF
            val b = (behind shr shift) and 0xFF
            val value = Math.round(share * f + (1 - share) * b).toLong()
            result = result or (value shl shift)
        }
        return result
    }

    @Test
    fun `the pale button stays visible`() {
        listOf(
            ThemeName.DARK to false,
            ThemeName.LIGHT to false,
            ThemeName.HIGH_CONTRAST to false,
        ).forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            val ground = palette.background.argb()
            val surface = palette.surfaceDefault.fill.argb()
            val ink = palette.surfaceDefault.ink.argb()

            val paleSurface = over(surface, ground, PALE)
            val paleIcon = over(ink, paleSurface, PALE)

            listOf(
                "against the pale surface" to paleSurface,
                "against the background" to ground,
            ).forEach { (where, behind) ->
                val ratio = contrastRatio(paleIcon, behind)
                assertTrue(
                    "%s: the pale icon (#%06X) reaches %s only %.2f:1 - under %.1f"
                        .format(theme, paleIcon and 0xFFFFFF, where, ratio, Tokens.MIN_TILE_ON_BACKGROUND),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    @Test
    fun `pale is still clearly paler than awake`() {
        // otherwise the withdrawal is none: whoever cannot see that the button does nothing
        // any more keeps tapping it.
        listOf(ThemeName.DARK, ThemeName.LIGHT, ThemeName.HIGH_CONTRAST).forEach { theme ->
            val palette = paletteFor(theme, false)
            val surface = palette.surfaceDefault.fill.argb()
            val ink = palette.surfaceDefault.ink.argb()
            val paleSurface = over(surface, palette.background.argb(), PALE)
            val paleIcon = over(ink, paleSurface, PALE)
            val awake = contrastRatio(ink, surface)
            val pale = contrastRatio(paleIcon, paleSurface)
            assertTrue(
                "%s: awake %.2f, pale %.2f - the difference does not carry".format(theme, awake, pale),
                awake >= pale * 2,
            )
        }
    }
}
