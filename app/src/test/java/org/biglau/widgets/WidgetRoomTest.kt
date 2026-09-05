package org.biglau.widgets

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the sentence for a widget that does not fit says what **is** and names the way out.
 *
 * the widget is written onto the tile before the room is checked, and that is intended: the
 * id is handed out by then, and a permission may just have been granted in the system -
 * throwing that away would be worse than a squeezed tile. so the sentence must not claim
 * nothing has happened yet.
 *
 * the way out is the resize row in the same editor. the rule checks the connection, not the
 * wording - whoever renames the row has to take the sentence along.
 */
class WidgetRoomTest {

    @Test
    fun `the sentence names the row that solves the problem`() {
        listOf("values", "values-de").forEach { language ->
            val sentence = Quelltext.textValue("widget_no_room", language)
            val row = Quelltext.textValue("editor_resize", language)
            assertTrue(
                "$language: the sentence does not name the row \"$row\" that makes the tile " +
                    "bigger: $sentence",
                row in sentence,
            )
        }
    }

    @Test
    fun `the sentence does not claim nothing has happened yet`() {
        val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")
        val spot = Quelltext.cut(editor, "fun finishWidget(", "pendingWidget = null")
        assertTrue(
            "the tile is no longer written before the room check - then the sentence may say " +
                "again that nothing has happened, and this rule can go.",
            spot.indexOf("write(next)") in 0 until spot.indexOf("widget_no_room"),
        )
        listOf("values", "values-de").forEach { language ->
            val sentence = Quelltext.textValue("widget_no_room", language)
            assertTrue(
                "$language: the sentence is only half - it has to say what is and what is " +
                    "left to do: $sentence",
                sentence.count { it == '.' } >= 2,
            )
        }
    }
}
