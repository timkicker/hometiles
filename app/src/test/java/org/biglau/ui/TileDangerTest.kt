package org.biglau.ui

import org.biglau.Quelltext
import java.io.File
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the danger colour does not belong on a tile.
 *
 * with zero bars "4G" stood in red on the orange signal tile; measured that is **1,78 to 1**,
 * where the app demands 4,5 for labels on tiles (`PLAN.md` 3.3). the same held for the
 * battery level: at nine percent the number turned red and so became hard to read exactly
 * when it counted - a warning that hides itself.
 *
 * red stays right on the **background**, where it stands against near black and is checked.
 * on a tile it stands against one of the six tile colours, and against those it reaches 1,8
 * in no theme.
 *
 * the warning is carried by the thing itself now: nine percent are nine percent, zero of
 * four bars are zero of four - and for the screen reader the word stands in the
 * announcement.
 */
class TileDangerTest {

    /** the files that draw *on* a tile. */
    private val onTiles = listOf(
        "org/biglau/ui/InfoTiles.kt",
        "org/biglau/ui/BigTile.kt",
        "org/biglau/ui/WidgetTile.kt",
        "org/biglau/ui/HomeScreenView.kt",
    ).map(Quelltext::file)

    @Test
    fun `the danger colour fails on every tile tone`() {
        listOf(
            Triple("dark", Tokens.DARK_DANGER, Tokens.DARK_TILES),
            Triple("light", Tokens.LIGHT_DANGER, Tokens.LIGHT_TILES),
        ).forEach { (theme, danger, tiles) ->
            tiles.forEach { tone ->
                val ratio = contrastRatio(danger, tone)
                assertTrue(
                    "in the $theme theme the danger colour on ${tone.toString(16)} would " +
                        "reach $ratio to 1 - should that ever rise above " +
                        "${Tokens.MIN_LABEL_ON_TILE}, the rule below may go",
                    ratio < Tokens.MIN_LABEL_ON_TILE,
                )
            }
        }
    }

    @Test
    fun `no tile drawing reaches for the danger colour`() {
        val hits = onTiles.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if ("palette.danger" in line) "${file.name}:${index + 1}" else null
            }
        }
        assertEquals(emptyList<String>(), hits)
    }
}
