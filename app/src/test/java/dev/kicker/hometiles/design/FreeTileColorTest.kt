package dev.kicker.hometiles.design

import dev.kicker.hometiles.ui.theme.FreeTileColor
import dev.kicker.hometiles.ui.theme.Tokens
import dev.kicker.hometiles.ui.theme.contrastRatio
import dev.kicker.hometiles.ui.theme.relativeLuminance
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a freely chosen colour must never be unreadable.
 *
 * this is where "free" (PLAN.md 4.2) and "contrast is a function, not a taste" (3.3) meet:
 * the person picks the hue, the app computes the brightness.
 */
class FreeTileColorTest {

    private val dark = Tokens.DARK_BACKGROUND
    private val light = Tokens.LIGHT_BACKGROUND
    private val white = 0xFFFFFFFFL
    private val darkWeight = FreeTileColor.targetLuminance(Tokens.DARK_TILES)
    private val lightWeight = FreeTileColor.targetLuminance(Tokens.LIGHT_TILES)

    private fun colour(hue: Float, ground: Long) = FreeTileColor.forHue(
        hue,
        ground,
        white,
        if (ground == dark) darkWeight else lightWeight,
    )

    private fun check(ground: Long, name: String) {
        FreeTileColor.hues.forEach { hue ->
            val tile = colour(hue, ground)
            val againstGround = contrastRatio(tile, ground)
            val label = contrastRatio(white, tile)
            assertTrue(
                "$name, hue $hue: tile against background only $againstGround",
                againstGround >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
            assertTrue(
                "$name, hue $hue: label on the tile only $label",
                label >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `every hue holds both thresholds in the dark theme`() = check(dark, "dark")

    @Test
    fun `every hue holds both thresholds in the light theme`() = check(light, "light")

    @Test
    fun `there are twenty-four hues and none twice`() {
        assertEquals(24, FreeTileColor.hues.size)
        assertEquals(24, FreeTileColor.hues.toSet().size)
        assertEquals(0f, FreeTileColor.hues.first())
        assertTrue("the last stays below 360", FreeTileColor.hues.last() < 360f)
    }

    @Test
    fun `the same hue always gives the same colour`() {
        // otherwise the tile would jump on every redraw.
        FreeTileColor.hues.forEach { hue ->
            assertEquals(
                colour(hue, dark),
                colour(hue, dark),
            )
        }
    }

    @Test
    fun `the hue runs around the circle`() {
        assertEquals(
            colour(30f, dark),
            colour(390f, dark),
        )
        assertEquals(
            colour(30f, dark),
            colour(-330f, dark),
        )
    }

    @Test
    fun `the free colours lie on the palette's weight`() {
        // PLAN.md 3.3: all tile colours lie on the same level on purpose. maximising the
        // distance to both thresholds instead sends every hue towards black in the light
        // theme, since both ratios grow the darker the tile is. measured on the screenshot:
        // 14,5:1 instead of the 3,5 aimed at, and twenty-four hues that all looked alike.
        listOf(dark to darkWeight, light to lightWeight).forEach { (ground, target) ->
            FreeTileColor.hues.forEach { hue ->
                val distance = abs(relativeLuminance(colour(hue, ground)) - target)
                assertTrue(
                    "hue $hue deviates by $distance from the palette's weight",
                    distance < 0.06,
                )
            }
        }
    }

    @Test
    fun `the hues stay distinguishable in the light theme too`() {
        val colours = FreeTileColor.hues.map { colour(it, light) }
        assertTrue("at least 20 distinguishable colours", colours.toSet().size >= 20)
        // and none of them may be black in effect.
        colours.forEach {
            assertTrue("too dark: ${it.toString(16)}", relativeLuminance(it) > 0.04)
        }
    }

    @Test
    fun `different hues give different colours`() {
        // if the computation pulled everything to the same value the choice would be a sham.
        val colours = FreeTileColor.hues.map { colour(it, dark) }
        assertTrue("at least 20 distinguishable colours", colours.toSet().size >= 20)
    }

    @Test
    fun `the same theme gives every hue its own version, no grey`() {
        // the fallback returns the background colour - it must never take hold.
        FreeTileColor.hues.forEach { hue ->
            assertTrue(
                "hue $hue falls back to the background",
                colour(hue, dark) != dark,
            )
        }
    }
}
