package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tap anywhere in the search row sets the caret.
 *
 * the drawn row is 64 dp high, the input field inside it only 48 - and once the hit count
 * stands below it, 38. measured on the Jelly 2 on 04.09.2026: a tap on the row's top
 * fourteen pixels did nothing at all (`mInputShown` stayed `false`), although a field is
 * drawn there.
 *
 * that is the silent miss: it looks like a field, one taps it, and nothing happens. the hand
 * HomeTiles is built for hits the edge regularly.
 */
class SearchRowHitTest {

    private val field = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigSearchField.kt")

    @Test
    fun `the whole row takes the tap`() {
        assertTrue(
            "the search row does not pass the tap on to the field - then a tap on its edge " +
                "does nothing.",
            "detectTapGestures { caret.requestFocus() }" in field,
        )
        assertTrue(
            "the input field does not take the caret.",
            "focusRequester(caret)" in field,
        )
    }

    /**
     * and the row stays a field, not a button. `clickable` would make it a control for the
     * screen reader - `BigRow` says in its own description why that is bad.
     */
    @Test
    fun `the row does not become a control by it`() {
        val row = Quelltext.cut(field, "Row(", "Icon(")
        assertTrue(
            "the search row has become clickable - then the screen reader says \"button\" " +
                "although an input field stands there: $row",
            ".clickable" !in row,
        )
    }
}
