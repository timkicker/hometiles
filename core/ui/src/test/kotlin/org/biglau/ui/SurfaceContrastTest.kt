package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.BigSurface
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * runs over every surface pair of every theme. the same kind of fault arose here three times -
 * white text on a light surface - each time in a new place. this test knows no places, only
 * pairs, and so catches the next one too.
 */
class SurfaceContrastTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    private fun BigSurface.ratio(): Double = contrastRatio(ink.argb(), fill.argb())

    @Test
    fun `every surface carries its own type readably`() {
        themesAndSystem().forEach { (theme, systemIsDark) ->
            paletteFor(theme, systemIsDark).allSurfaces().forEach { surface ->
                val ratio = surface.ratio()
                assertTrue(
                    "%s: %s on %s reaches only %.2f:1".format(
                        theme, surface.ink.hex(), surface.fill.hex(), ratio,
                    ),
                    ratio >= Tokens.MIN_LABEL_ON_TILE,
                )
            }
        }
    }

    @Test
    fun `every meaning-carrying surface stands out from the background`() {
        // holds for fills that carry the statement themselves: tiles, accent, danger. the
        // quiet surface under a list row is exempt - there the text identifies the row, not
        // the fill.
        themesAndSystem().filter { it.first != ThemeName.HIGH_CONTRAST }.forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            val background = palette.background.argb()
            val meaningful = palette.tiles.indices.map(palette::surfaceTile) +
                palette.surfaceAccent + palette.surfaceDanger
            meaningful.forEach { surface ->
                val ratio = contrastRatio(surface.fill.argb(), background)
                assertTrue(
                    "%s: the surface %s reaches only %.2f:1 against the background".format(
                        theme, surface.fill.hex(), ratio,
                    ),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    /**
     * a border has **two** grounds: the background outside, the tile's fill inside. only the
     * outer one stood here - and inside the light border reached 2.86:1, under the threshold.
     * checked on the emulator pixel by pixel: at x=243..245 the border, from x=246 the fill,
     * with no gap.
     *
     * the same fault as with the danger red two hours before: the rule existed, it was only
     * measured against one of two grounds.
     */
    @Test
    fun `the empty tile can be found by its border`() {
        // the fill is deliberately quiet (1.09:1 in the dark theme). for an empty slot to be
        // visible anyway, the border has to reach the surface threshold.
        themesAndSystem().forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            listOf(
                "outside, against the background" to palette.background,
                "inside, against the fill" to palette.emptyTile,
            ).forEach { (where, ground) ->
                val ratio = contrastRatio(palette.emptyTileBorder.argb(), ground.argb())
                assertTrue(
                    "%s: the border %s reaches %s only %.2f:1".format(
                        theme, palette.emptyTileBorder.hex(), where, ratio,
                    ),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    /**
     * the danger type does not stand in [org.biglau.ui.theme.BigPalette.allSurfaces] - it is
     * no surface but a tone of its own. `ContrastTest` checks the tokens; here the **palette**
     * is checked, so also that every theme wired the right token. a theme accidentally using
     * `danger` would stand out nowhere else.
     */
    @Test
    fun `every theme's danger type lies above the strict threshold`() {
        themesAndSystem().forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            listOf(
                "on the background" to palette.background,
                "on the empty tile" to palette.emptyTile,
            ).forEach { (where, ground) ->
                val ratio = contrastRatio(palette.dangerText.argb(), ground.argb())
                assertTrue(
                    "%s: the danger type %s reaches %s only %.2f:1".format(
                        theme, palette.dangerText.hex(), where, ratio,
                    ),
                    ratio >= Tokens.MIN_TEXT_ON_BACKGROUND,
                )
            }
        }
    }

    @Test
    fun `every theme offers all surfaces`() {
        themesAndSystem().forEach { (theme, systemIsDark) ->
            // three named surfaces plus six tile colours
            assertTrue(paletteFor(theme, systemIsDark).allSurfaces().size == 9)
        }
    }

    private fun Color.hex() = "#%06X".format(argb() and 0xFFFFFF)
}
