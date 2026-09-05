package org.biglau.design

import org.biglau.Quelltext
import org.biglau.a11y.LongPress
import org.biglau.a11y.LongPressAction
import org.biglau.data.Accessibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever takes the long press loses the way to the editor with it - and is told.
 *
 * `LongPress.decide` gives a second action precedence over everything else, so for **this**
 * tile the long press no longer opens the editor. when a *setting* takes the long press,
 * `a11y_editor_moved` says so; when the tile itself takes it, nothing did until 04.09.2026.
 *
 * and it is not rare: whoever sets a second action wants to edit the tile - exactly what no
 * longer works the usual way afterwards.
 */
class LongPressHintTest {

    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    /** first the reason: a second action really does take the editor from the long press. */
    @Test
    fun `a second action comes before the editor`() {
        assertEquals(
            "a second action opens the editor again - then the hint is wrong and belongs " +
                "away.",
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(Accessibility(), editMode = false, hasSecondAction = true),
        )
        assertEquals(
            "without a second action the long press should still lead to the editor.",
            listOf(LongPressAction.EDIT),
            LongPress.decide(Accessibility(), editMode = false, hasSecondAction = false),
        )
    }

    @Test
    fun `the editor says so as soon as a second action is there`() {
        val from = editor.indexOf("if (button.longPress != null) {")
        assertTrue("the branch for a set second action is gone", from > 0)
        val branch = editor.substring(from, minOf(editor.length, from + 900))
        assertTrue(
            "the editor does not say that the long press no longer opens it. whoever wants " +
                "to change the tile later holds it down and triggers the second action - in " +
                "the worst case a call.",
            "R.string.editor_long_press_takes_editor" in branch,
        )
    }

    /** and it says so **only** then - otherwise it would be a warning without cause. */
    @Test
    fun `without a second action the hint does not stand there`() {
        val places = Regex("""R\.string\.editor_long_press_takes_editor""")
            .findAll(editor).map { it.range.first }.toList()
        assertEquals(
            "the hint should stand there exactly once - never would be no information, " +
                "twice would be noise.",
            1,
            places.size,
        )
        val from = editor.indexOf("if (button.longPress != null) {")
        assertTrue(
            "the hint stands outside the branch for a set second action - then it warns " +
                "even when there is nothing to warn about.",
            places.first() > from,
        )
    }
}
