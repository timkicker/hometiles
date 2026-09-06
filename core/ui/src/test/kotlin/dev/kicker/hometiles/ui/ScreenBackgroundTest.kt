package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.Background
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.ui.theme.ScreenBackground
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.contrastRatio
import dev.kicker.hometiles.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1: a background of its own per screen.
 *
 * `Background.Solid` was painted from the start and settable nowhere; `Background.Image`
 * stood in the model and was never painted, swallowed by an `else` branch. both are promises
 * that stand in the backup file and do nothing.
 */
class ScreenBackgroundTest {

    @Test
    fun `the default is the theme's colour`() {
        assertEquals(Background.Theme, Screen("a", "A", 2, 3).background)
    }

    /**
     * the one rule everything hangs on: a tile has to stand out from its ground or one cannot
     * see where it ends. three to one is the wcag threshold for surfaces that must be told
     * apart.
     */
    @Test
    fun `every colour stands out from every tile colour`() {
        val weak = mutableListOf<String>()
        for ((theme, systemIsDark) in themesAndSystem()) {
            val palette = paletteFor(theme, systemIsDark)
            for (ground in ScreenBackground.choicesFor(theme, systemIsDark)) {
                for ((i, tile) in palette.tiles.withIndex()) {
                    val value = contrastRatio(ground, tile.value.toLong() shr 32)
                    if (value < 3.0) weak += "$theme/tile$i/${ground.toString(16)}: %.2f".format(value)
                }
            }
        }
        assertEquals(emptyList<String>(), weak)
    }

    /**
     * **more** than a tile stands on a background of its own: the empty tile's border - the
     * only sign that a free slot is there - and the warning type. both are checked against the
     * *theme* colour, and a background of its own is exactly what invalidates that check.
     *
     * both hold, narrowly: the border reaches 3.06 in the dark theme, the warning type 7.23.
     * at twelve percent tinting that is not chance but the number chosen there.
     */
    @Test
    fun `border and warning type hold on every background of its own too`() {
        val weak = mutableListOf<String>()
        for ((theme, systemIsDark) in themesAndSystem()) {
            val palette = paletteFor(theme, systemIsDark)
            for (ground in ScreenBackground.choicesFor(theme, systemIsDark)) {
                val border = contrastRatio(palette.emptyTileBorder.value.toLong() shr 32, ground)
                if (border < 3.0) {
                    weak += "$theme/border/${ground.toString(16)}: %.2f".format(border)
                }
                val warning = contrastRatio(palette.dangerText.value.toLong() shr 32, ground)
                if (warning < 7.0) {
                    weak += "$theme/warning/${ground.toString(16)}: %.2f".format(warning)
                }
            }
        }
        assertEquals(emptyList<String>(), weak)
    }

    // the ink on the background is decided anew, not taken from the theme: the theme's ink is
    // checked against the theme colour, not against this one.
    @Test
    fun `the ink reaches the large-text value everywhere`() {
        for (ground in themesAndSystem().flatMap { (theme, dark) -> ScreenBackground.choicesFor(theme, dark) }) {
            val ink = ScreenBackground.inkFor(ground)
            assertTrue(
                "${ground.toString(16)} reaches only %.2f".format(contrastRatio(ground, ink)),
                contrastRatio(ground, ink) >= 4.5,
            )
        }
    }

    @Test
    fun `light ink stands on a dark ground`() {
        assertEquals(0xFFFFFFFF, ScreenBackground.inkFor(0xFF101418))
        assertEquals(0xFF000000, ScreenBackground.inkFor(0xFFFFEB3B))
    }

    /**
     * in the high contrast theme there is no choice: background and tiles carry the same
     * colour there, and the tiles stand out by their border, not their fill. a background of
     * its own would soften the one property this theme is chosen for.
     */
    @Test
    fun `the high contrast mode is offered no colours`() {
        assertEquals(false, ScreenBackground.offersChoices(ThemeName.HIGH_CONTRAST))
        assertEquals(emptyList<Long>(), ScreenBackground.choicesFor(ThemeName.HIGH_CONTRAST, true))
        assertTrue(ScreenBackground.choicesFor(ThemeName.DARK, true).isNotEmpty())
        assertTrue(ScreenBackground.choicesFor(ThemeName.LIGHT, true).isNotEmpty())
        // "like the phone" gets colours too - in both states.
        assertTrue(ScreenBackground.choicesFor(ThemeName.SYSTEM, true).isNotEmpty())
        assertTrue(ScreenBackground.choicesFor(ThemeName.SYSTEM, false).isNotEmpty())
    }

    // five tones nobody can tell apart are not a choice but an imposition.
    @Test
    fun `the colours differ from each other`() {
        val colours = ScreenBackground.choicesFor(ThemeName.DARK, true)
        assertEquals(colours.size, colours.toSet().size)
    }
}
