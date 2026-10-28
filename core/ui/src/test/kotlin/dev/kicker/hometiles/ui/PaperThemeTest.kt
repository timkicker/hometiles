package dev.kicker.hometiles.ui

import androidx.compose.ui.graphics.Color
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.FreeTileColor
import dev.kicker.hometiles.ui.theme.ScreenBackground
import dev.kicker.hometiles.ui.theme.paletteFor
import dev.kicker.hometiles.ui.theme.tileBorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * issue #5: on an e-ink screen every theme came out as dark grey tiles. paper is white tiles
 * with black writing and a black border, the high contrast theme turned around.
 */
class PaperThemeTest {

    private val paper = paletteFor(ThemeName.PAPER, systemIsDark = true)

    @Test
    fun `white paper and black ink, whatever the phone says`() {
        assertEquals(Color.White, paper.background)
        assertEquals(Color.Black, paper.onBackground)
        assertEquals(paper, paletteFor(ThemeName.PAPER, systemIsDark = false))
    }

    @Test
    fun `every tile is white with black writing`() {
        paper.tiles.forEach { assertEquals(Color.White, it) }
        assertEquals(Color.Black, paper.onTile)
        assertEquals(Color.White, paper.emptyTile)
    }

    @Test
    fun `the tiles stand by a black border`() {
        // white on white: without the border a tile is not there at all.
        assertEquals(Color.Black, paper.tileBorder())
    }

    @Test
    fun `no colour sneaks back in`() {
        // a free tile colour or a tinted background turns into grey again on e-ink.
        assertFalse(FreeTileColor.themeUsesTileColours(paper.tiles.map { it.value.toLong() shr 32 }))
        assertFalse(ScreenBackground.offersChoices(ThemeName.PAPER))
    }
}
