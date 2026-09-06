package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.Tokens
import dev.kicker.hometiles.ui.theme.contrastRatio
import dev.kicker.hometiles.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the contact picture without a photo stands on a list row, not on the background.
 *
 * `ContactAvatar` takes a tile colour and writes the initials on it. both pairs were checked
 * - tile against **background** and label against tile. only the avatar sits in a `BigRow`,
 * so on `surfaceDefault`, and nobody looked at that pair until 04.09.2026.
 *
 * it holds: 3.19 dark and 6.35 light against the threshold 3.0. dark is tight, and tight
 * means here: lightening the empty tile's fill by two steps makes the avatars invisible
 * without any rule saying so.
 *
 * in the contrast theme every tile place is the background; there the yellow ink carries the
 * avatar, and the area may be the same.
 */
class AvatarColourTest {

    @Test
    fun `the contact picture stands out from the row`() {
        val weak = mutableListOf<String>()
        themesAndSystem()
            .filter { it.first != ThemeName.HIGH_CONTRAST }
            .forEach { (theme, systemIsDark) ->
                val palette = paletteFor(theme, systemIsDark)
                val row = palette.surfaceDefault.fill.value.toLong() shr 32
                palette.tiles.forEachIndexed { i, colour ->
                    val value = contrastRatio(colour.value.toLong() shr 32, row)
                    if (value < Tokens.MIN_TILE_ON_BACKGROUND) {
                        weak += "$theme/Avatar$i: %.2f".format(value)
                    }
                }
            }
        assertEquals(
            "a contact picture without a photo vanishes in its row",
            emptyList<String>(),
            weak,
        )
    }

    @Test
    fun `the initials are readable on every contact picture`() {
        val weak = mutableListOf<String>()
        themesAndSystem().forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            palette.tiles.forEachIndexed { i, colour ->
                val value = contrastRatio(
                    palette.onTile.value.toLong() shr 32,
                    colour.value.toLong() shr 32,
                )
                if (value < Tokens.MIN_LABEL_ON_TILE) weak += "$theme/Avatar$i: %.2f".format(value)
            }
        }
        assertEquals(emptyList<String>(), weak)
    }
}
