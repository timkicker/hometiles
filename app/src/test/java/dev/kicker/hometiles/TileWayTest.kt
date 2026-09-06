package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the way out of a notice leads to the place that was meant.
 *
 * tapping a tile whose app is gone makes HomeTiles say so and open the tile editor - the way
 * instead of the directions. only the editor then has to stand on **this** tile.
 *
 * reproduced on 04.09.2026: a tile pointing at a missing package, inside a folder on field
 * (1,2). the editor opened on field (1,2) of the **home screen** - the tile that opens the
 * folder. a folder lies over the home screen without changing the screen, and
 * `currentScreenId()` knows nothing of it. whoever followed the invitation would have
 * overwritten their folder instead of the broken tile.
 *
 * the cells in the editor all come from the same corner: the composable gets the shown
 * screen handed in and runs once for the screen and once for the folder above it. whoever
 * names a tile from there has to take the shown screen's id.
 */
class TileWayTest {

    private val main = Quelltext.withoutComments("dev/kicker/hometiles/MainActivity.kt")

    @Test
    fun `no editor on the screen that is not visible`() {
        val wrong = Regex("TileEditorActivity\\.intent\\([^)]*currentScreenId\\(\\)")
            .findAll(main).map { it.value }.toList()
        assertEquals(
            "the tile editor is opened with currentScreenId(). an open folder does not " +
                "change the screen - the tile then lies in the folder while the editor " +
                "stands on the home screen.",
            emptyList<String>(),
            wrong,
        )
    }

    @Test
    fun `whoever remembers a tile remembers its screen too`() {
        val tap = Quelltext.cut(
            main,
            from = "private data class LockedTap(",
            to = ")",
        )
        assertTrue(
            "LockedTap holds x and y but not the screen. after the pin nobody knows any " +
                "more which screen the tile lay on: $tap",
            "screenId" in tap,
        )
    }

    @Test
    fun `starting is told the screen`() {
        val signature = Quelltext.cut(main, from = "private fun startAction(", to = "{")
        assertTrue(
            "startAction() takes no screenId - then it has to invent the screen itself, " +
                "and that went wrong once already: $signature",
            "screenId: String" in signature,
        )
    }
}
