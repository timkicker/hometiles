package dev.kicker.hometiles.ui

import androidx.compose.ui.graphics.Color
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.contrastRatio
import dev.kicker.hometiles.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the focus border can be seen on whatever it lies on, and against the page around it.
 *
 * the rows of the theme choice show the other themes, so in the dark theme a light grey row
 * stands in the list, and the white focus border on it was measured as no border at all
 * (`tools/focus-visible.py`, 19.10.2026). this runs every fill of every theme under every
 * theme, the previews included.
 *
 * the second half came with issue #2: the rule looked at the fill alone, and around a red
 * row in the light theme it took the page's own colour. 1.00:1 against the page, measured on
 * the emulator on 25.10.2026, and the focused row only looked a little smaller.
 *
 * 3:1 is wcag 1.4.11 for a part of an interface that has to be seen.
 */
class FocusInkTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    private fun faintRings(theme: Any, rings: FocusRings, fill: Long, page: Long): List<String> {
        val touching = rings.inner ?: rings.outer
        return listOfNotNull(
            contrastRatio(touching, fill).takeIf { it < 3.0 }?.let {
                "%s: border #%06X on the fill #%06X only %.2f:1".format(theme, touching and 0xFFFFFF, fill and 0xFFFFFF, it)
            },
            contrastRatio(rings.outer, page).takeIf { it < 3.0 }?.let {
                "%s: border #%06X on the page #%06X only %.2f:1".format(theme, rings.outer and 0xFFFFFF, page and 0xFFFFFF, it)
            },
        )
    }

    @Test
    fun `the focus border reaches 3 to 1 on every fill and on the page`() {
        val palettes = themesAndSystem().map { (theme, dark) -> theme to paletteFor(theme, dark) }
        val fills = palettes.flatMap { (_, p) -> p.allSurfaces().map { it.fill } + p.background }
            .map { it.argb() }
            .distinct()
        val faint = palettes.flatMap { (theme, p) ->
            fills.flatMap { fill ->
                val rings = focusRingsFor(p.onBackground.argb(), p.background.argb(), fill)
                faintRings(theme, rings, fill, p.background.argb())
            }
        }.distinct()
        assertEquals("a focus border that vanishes", emptyList<String>(), faint)
    }

    @Test
    fun `a focused tile stands out from its colour and from the page`() {
        val faint = themesAndSystem().flatMap { (theme, dark) ->
            val p = paletteFor(theme, dark)
            val rings = tileFocusRingsFor(p.onTile.argb(), p.onBackground.argb(), p.background.argb())
            p.tiles.flatMap { tile -> faintRings(theme, rings, tile.argb(), p.background.argb()) }
        }.distinct()
        assertEquals("a tile focus border that vanishes", emptyList<String>(), faint)
    }

    // the measured case itself, so it stays named.
    @Test
    fun `a red row in the light theme has a border the page does not swallow`() {
        val p = paletteFor(ThemeName.LIGHT, false)
        val rings = focusRingsFor(p.onBackground.argb(), p.background.argb(), p.danger.argb())
        assertTrue(contrastRatio(rings.outer, p.background.argb()) >= 3.0)
    }

    @Test
    fun `without a fill it is the theme's ink`() {
        val p = paletteFor(themesAndSystem().first().first, true)
        assertEquals(
            FocusRings(p.onBackground.argb()),
            focusRingsFor(p.onBackground.argb(), p.background.argb(), null),
        )
    }
}
