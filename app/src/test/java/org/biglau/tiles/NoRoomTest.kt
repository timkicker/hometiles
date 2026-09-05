package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * where nothing works, at least how it would work stands there.
 *
 * "change size" on a tile whose neighbours are all filled could show an empty list - a screen
 * offering nothing with nobody saying why. it does not, and that is good enough to hold fast:
 * it names the reason and **two** ways out, freeing a neighbour or giving the screen more
 * slots.
 *
 * this rule found no fault; it holds what is already right. a sentence naming a way out is
 * easily lost in the next round of shortening.
 */
class NoRoomTest {

    @Test
    fun `the notice names a way out`() {
        // the words are the interface's own; the check compares them literally, so the
        // german ones stay.
        listOf(
            "values-de" to listOf("Nachbarn", "Einstellungen"),
            "values" to listOf("neighbour", "settings"),
        ).forEach { (language, words) ->
            val text = Quelltext.textValue("resize_no_room", language).lowercase()
            words.forEach { word ->
                assertTrue(
                    "the notice does not say how it would work - \"$word\" is missing " +
                        "(language \"$language\"): $text",
                    word.lowercase() in text,
                )
            }
        }
    }

    @Test
    fun `the notice stands where nothing works any more`() {
        val source = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")
        assertTrue(
            "resize_no_room is shown nowhere - then the screen is empty on a full grid",
            "resize_no_room" in source,
        )
    }
}
