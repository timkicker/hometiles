package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * two empty spots are two spots, not the same one twice.
 *
 * every empty tile invites with the same word. for the eye that is right - one sees where it
 * lies. found on 04.09.2026 with `tools/same-names.py` on the home screen: two clickable
 * areas, one name, no difference for someone who does not see them.
 *
 * and the chain stayed silent afterwards: the tile editor said the cell was empty without
 * naming it either. whoever fills one of two knew at no point which.
 *
 * the spot is now said - with the same words as the move view, so both screens describe the
 * same grid.
 */
class EmptySpotsTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/ui/HomeScreenView.kt")

    @Test
    fun `an empty tile says its spot`() {
        val empty = Quelltext.cut(
            source,
            from = "private fun EmptyTile(",
            to = "\n}",
        )
        assertTrue(
            "the empty tile does not name its spot - then all empty tiles are called the " +
                "same:\n$empty",
            "move_spot" in empty,
        )
    }

    @Test
    fun `both ways to the empty tile pass the spot along`() {
        // there are two: a grid spot without a cell, and a cell without an action. if only
        // one passed the spot, half the empty tiles would stay mute - and which half would
        // depend on how the configuration happens to look.
        val calls = Regex("EmptyTile\\(").findAll(source).count()
        val withSpot = Regex("column = ").findAll(source).count()
        assertTrue(
            "EmptyTile is called ${calls - 1} times but only $withSpot times with a spot",
            withSpot >= calls - 1,
        )
    }

    @Test
    fun `the words for the spot exist in both languages`() {
        listOf("values-de", "values").forEach { language ->
            val text = Quelltext.textValue("move_spot", language)
            assertTrue(
                "move_spot needs row and column (language \"$language\"): $text",
                "%1\$d" in text && "%2\$d" in text,
            )
        }
    }
}
