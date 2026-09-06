package dev.kicker.hometiles.ui

import dev.kicker.hometiles.ui.theme.Tokens
import dev.kicker.hometiles.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 3.3 sets the contrast thresholds as a function, not as a matter of taste. changing
 * a colour fails here instead of being noticed on the device.
 */
class ContrastTest {

    @Test
    fun `known ratios agree with the wcag formula`() {
        // black on white is 21:1 by definition.
        assertEquals(21.0, contrastRatio(0xFF000000L, 0xFFFFFFFFL), 0.01)
        // the same colour gives 1:1.
        assertEquals(1.0, contrastRatio(0xFF2763CBL, 0xFF2763CBL), 0.0001)
        // the order of the arguments must change nothing.
        assertEquals(
            contrastRatio(0xFF2763CBL, Tokens.DARK_BACKGROUND),
            contrastRatio(Tokens.DARK_BACKGROUND, 0xFF2763CBL),
            0.0001,
        )
    }

    @Test
    fun `every dark tile stands out from the background`() {
        Tokens.DARK_TILES.forEach { tile ->
            val ratio = contrastRatio(tile, Tokens.DARK_BACKGROUND)
            assertTrue(
                "the tile ${tile.toHex()} reaches only %.2f:1 against the background".format(ratio),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
    }

    @Test
    fun `a white label is readable on every dark tile`() {
        Tokens.DARK_TILES.forEach { tile ->
            val ratio = contrastRatio(0xFFFFFFFFL, tile)
            assertTrue(
                "white on ${tile.toHex()} reaches only %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `the tiles all lie at the same contrast level`() {
        // otherwise one tile looks heavier than another and telling them apart no longer runs
        // through the hue alone.
        val ratios = Tokens.DARK_TILES.map { contrastRatio(it, Tokens.DARK_BACKGROUND) }
        val spread = ratios.max() - ratios.min()
        assertTrue("the contrast spread is %.2f, allowed is 0.3".format(spread), spread <= 0.3)
    }

    @Test
    fun `text outside the tiles reaches the stricter threshold`() {
        assertTrue(
            contrastRatio(Tokens.DARK_ON_BACKGROUND, Tokens.DARK_BACKGROUND) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
        assertTrue(
            contrastRatio(Tokens.DARK_ON_BACKGROUND, Tokens.DARK_EMPTY_TILE) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
        assertTrue(
            contrastRatio(Tokens.LIGHT_ON_BACKGROUND, Tokens.LIGHT_BACKGROUND) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
    }

    /**
     * the danger colour is drawn as **type** on the background in fifteen places - wherever
     * something can go wrong. nobody had checked that: the rule above looked only at
     * `ON_BACKGROUND`, and `DANGER` appeared in not a single test against a background.
     *
     * measured, the old red reached 6.20:1 (dark), 5.39:1 (light) and 6.58:1 (contrast) -
     * three times under its own threshold of 7.0. hence a type tone of its own; the surface
     * colour stays as it was.
     */
    @Test
    fun `the danger type reaches the stricter threshold in every theme`() {
        listOf(
            Triple("dark", Tokens.DARK_DANGER_TEXT, Tokens.DARK_BACKGROUND),
            Triple("light", Tokens.LIGHT_DANGER_TEXT, Tokens.LIGHT_BACKGROUND),
            Triple("contrast", Tokens.CONTRAST_DANGER_TEXT, Tokens.CONTRAST_BACKGROUND),
            // on the empty tile as well: that is where it stands in the tile editor.
            Triple("dark, empty tile", Tokens.DARK_DANGER_TEXT, Tokens.DARK_EMPTY_TILE),
            Triple("light, empty tile", Tokens.LIGHT_DANGER_TEXT, Tokens.LIGHT_EMPTY_TILE),
        ).forEach { (name, type, ground) ->
            val ratio = contrastRatio(type, ground)
            assertTrue(
                "the danger type ($name) reaches only %.2f:1, required is %.1f"
                    .format(ratio, Tokens.MIN_TEXT_ON_BACKGROUND),
                ratio >= Tokens.MIN_TEXT_ON_BACKGROUND,
            )
        }
    }

    /**
     * the surface colour stays a surface colour: it has to stand out from the background and
     * carry its own label. the new type tone does not replace it, it stands beside it.
     */
    @Test
    fun `the danger surface stays a surface`() {
        listOf(
            Triple("dark", Tokens.DARK_DANGER, Tokens.DARK_BACKGROUND),
            Triple("light", Tokens.LIGHT_DANGER, Tokens.LIGHT_BACKGROUND),
        ).forEach { (name, surface, ground) ->
            val ratio = contrastRatio(surface, ground)
            assertTrue(
                "the danger surface ($name) stands out by only %.2f:1".format(ratio),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
        listOf(
            Triple("dark", Tokens.DARK_ON_DANGER, Tokens.DARK_DANGER),
            Triple("light", Tokens.LIGHT_ON_DANGER, Tokens.LIGHT_DANGER),
            Triple("contrast", Tokens.CONTRAST_ON_DANGER, Tokens.CONTRAST_DANGER),
        ).forEach { (name, type, surface) ->
            val ratio = contrastRatio(type, surface)
            assertTrue(
                "type on the danger surface ($name) reaches only %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `the contrast theme stays at black and yellow`() {
        val ratio = contrastRatio(Tokens.CONTRAST_INK, Tokens.CONTRAST_BACKGROUND)
        assertTrue("yellow on black reaches only %.2f:1".format(ratio), ratio >= 15.0)
    }

    @Test
    fun `light tiles carry a white label as well`() {
        Tokens.LIGHT_TILES.forEach { tile ->
            assertTrue(
                "white on ${tile.toHex()} is too weak",
                contrastRatio(0xFFFFFFFFL, tile) >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `text on the accent surface is readable`() {
        // white on the dark theme's light accent reached only 2.85:1 - hence a token of its
        // own instead of an assumption.
        listOf(
            Tokens.DARK_ON_ACCENT to Tokens.DARK_ACCENT,
            Tokens.LIGHT_ON_ACCENT to Tokens.LIGHT_ACCENT,
            Tokens.CONTRAST_ON_ACCENT to Tokens.CONTRAST_INK,
        ).forEach { (ink, accent) ->
            val ratio = contrastRatio(ink, accent)
            assertTrue(
                "${ink.toHex()} on ${accent.toHex()} reaches only %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `the accent itself stands out from the background`() {
        assertTrue(contrastRatio(Tokens.DARK_ACCENT, Tokens.DARK_BACKGROUND) >= Tokens.MIN_TILE_ON_BACKGROUND)
        assertTrue(contrastRatio(Tokens.LIGHT_ACCENT, Tokens.LIGHT_BACKGROUND) >= Tokens.MIN_TILE_ON_BACKGROUND)
    }

    private fun Long.toHex() = "#%06X".format(this and 0xFFFFFF)
}
