package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever picks a widget learns beforehand that the long press then leads elsewhere.
 *
 * on every other tile a long press opens the tile editor. on a widget tile it does not: the
 * widget gets the touch first. reproduced on 04.09.2026 - an analogue clock put on a tile,
 * held down, and the alarm app opened. the usual way to the editor is shut for this one tile.
 *
 * there is another one (settings, change the tiles; in edit mode a short tap is enough).
 * only nobody knows it who does not know it already. the sentence therefore stands **before**
 * the choice, in the widget list, and not as consolation afterwards.
 */
class WidgetLongPressTest {

    @Test
    fun `the widget list warns about the long press`() {
        val list = Quelltext.cut(
            Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt"),
            from = "private fun WidgetPicker(",
            to = "\n}",
        )
        assertTrue(
            "the widget choice does not say that the long press then opens the widget " +
                "instead of the editor:\n$list",
            "widget_long_press_hint" in list,
        )
    }

    @Test
    fun `the hint names the other way`() {
        // both wordings are the interface's own; the check compares them literally.
        listOf("values-de", "values").forEach { language ->
            val text = Quelltext.textValue("widget_long_press_hint", language)
            assertTrue(
                "the hint names no way out (language \"$language\"): $text",
                "Kacheln ändern" in text || "Change the tiles" in text,
            )
        }
    }
}
