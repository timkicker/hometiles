package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a screen editing a tile says **which**.
 *
 * the tile editor showed only that a tile was being edited and what it held. whoever tapped
 * one of two empty tiles saw nowhere which one they had hit - and the fault where the editor
 * opened on the home screen instead of inside the folder would have been visible at a glance
 * had the screen's name stood there.
 *
 * the same in the move view: there **every** target is called row x, column y. without a row
 * above it the whole screen is a list of abstract spots.
 *
 * both sentences name screen and spot, and both count from one - "row 0" reads like a fault.
 */
class WhichTileTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/tiles/TileEditorActivity.kt")

    @Test
    fun `the editor names screen and spot`() {
        val head = Quelltext.cut(source, from = "Mode.MENU -> MenuList(", to = "\n                        )")
        assertTrue(
            "the tile editor does not say which tile it is editing:\n$head",
            "editor_where" in head,
        )
    }

    @Test
    fun `the move view names the tile`() {
        val head = Quelltext.cut(source, from = "Mode.MOVE -> MoveTargetList(", to = "\n                        )")
        assertTrue(
            "the move view does not say which tile is being moved:\n$head",
            "move_which" in head,
        )
    }

    @Test
    fun `both sentences count from one`() {
        listOf("editor_where", "move_which").forEach { name ->
            listOf("values-de", "values").forEach { language ->
                val text = Quelltext.textValue(name, language)
                assertTrue(
                    "$name needs screen, row and column (language \"$language\"): $text",
                    "%1\$s" in text && "%2\$d" in text && "%3\$d" in text,
                )
            }
        }
        // and the +1 stands everywhere in the source, not the grid index.
        listOf("R.string.editor_where", "R.string.move_which").forEach { call ->
            val place = Quelltext.cut(source, from = call, atMost = 400)
            assertTrue(
                "counting from one is missing at $call - \"row 0\" reads like a fault:\n$place",
                "y + 1" in place && "x + 1" in place,
            )
        }
    }
}
