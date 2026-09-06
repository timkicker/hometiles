package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every surface laid over the home screen takes the focus. PLAN.md 10.3.5.
 *
 * the overlays sit in the **same window** as the home screen, as siblings above it. compose
 * picks a first focus target when a window starts, not when a surface arrives later.
 *
 * and gone means dead: `onPreviewKeyEvent` only runs the path from the focused node to the
 * root, so without focus no key handler runs, nothing moves, and the focus never comes back.
 *
 * this rule **counts**: every overlay in `MainActivity` has to be named here together with
 * the place where it asks for focus, so a new one makes it fall over.
 */
class OverlayFocusTest {

    private val home = Quelltext.withoutComments("dev/kicker/hometiles/MainActivity.kt")

    /** the locked app draws `PinGate`, which lies in `:core:ui`, so its mark is not in home. */
    private val lock = Quelltext.withoutComments("dev/kicker/hometiles/ui/PinGate.kt")

    private val sources get() = home + lock

    /**
     * what lies over the home screen, and what shows in the source that it takes the focus.
     * the same surfaces as in `covered`; `CoveredTest` holds that none is missing.
     */
    private val overlays = mapOf(
        "folder" to "active = onTop",
        "tileMenu" to "anchors.first().requestFocus()",
        "label" to "anchors.requestFocus()",
        // in `covered` it is called `asking`, in the source `ContactChoice`.
        "asking" to "anchors.first().requestFocus()",
        // the keypad takes the focus itself, see BigKeypad and KeypadFocusTest.
        "pending" to "takesFocus = true",
        "phoneStateAsked" to "anchors.first().requestFocus()",
    )

    /** nothing open. it stays empty only until a new overlay arrives. */
    private val unmeasured = emptyList<String>()

    @Test
    fun `every overlay is accounted for`() {
        val condition = Quelltext.cut(home, "val covered = ", "Column(")
        val named = overlays.keys + unmeasured
        val missing = named.filterNot { it in condition }
        assertTrue("the list is empty, then this rule measures nothing", named.size >= 5)
        assertEquals(
            "these names stand here but no longer in the condition `verdeckt`. either the " +
                "overlay is called something else now or it is gone - either way the rule " +
                "measures the wrong thing: $missing",
            emptyList<String>(),
            missing,
        )
        // and the other direction: nothing in `covered` that is missing here.
        val inCondition = Regex("""(\w+)(?:\.value)? != null|(\w+)\.value \|\|""")
            .findAll(condition)
            .map { it.groupValues.drop(1).first { value -> value.isNotEmpty() } }
            .toSet()
        assertEquals(
            "these overlays stand in `covered` but not in this rule. for each of them the " +
                "question whether it takes the focus belongs answered.",
            emptyList<String>(),
            inCondition.filterNot { it in named },
        )
    }

    @Test
    fun `whatever is measured takes the focus`() {
        assertTrue("no overlay named", overlays.isNotEmpty())
        overlays.forEach { (name, mark) ->
            assertTrue(
                "the overlay `$name` no longer asks for the focus ($mark is gone). then it " +
                    "stays unusable by key, and no key brings it back.",
                mark in sources,
            )
        }
    }

    /**
     * whoever takes the focus takes the way out with it.
     *
     * the contact choice was unusable by key and its only way out was the back key through
     * the home screen's `BackHandler`. as soon as it asked for the focus, the key stopped
     * arriving there - a `BackHandler` of its own did not help either, the key is spent in
     * the focus tree and never reaches the dispatcher.
     */
    @Test
    fun `the contact choice holds its own way out`() {
        val choice = Quelltext.cut(home, "private fun ContactChoice(", "private fun FolderOverlay(")
        assertTrue(
            "the contact choice no longer handles the back key itself. as soon as something " +
                "in it has the focus, the key does not reach the home screen's BackHandler " +
                "and the question stays standing.",
            Regex("""Key\.Back ->[\s\S]{0,80}onDismiss\(\)""").containsMatchIn(choice),
        )
        assertTrue(
            "it no longer asks for the focus, then it cannot be used by key.",
            "anchors.first().requestFocus()" in choice,
        )
        listOf("DirectionUp", "DirectionDown").forEach {
            assertTrue(
                "Key.$it is gone: there is no way between calling and writing then.",
                "Key.$it" in choice,
            )
        }
    }

    /**
     * the big label has exactly one action, so every key closes it and its hint says so.
     * `tap_to_close` belongs to the contact choice, which answers a key press differently
     * and must not share the sentence.
     */
    @Test
    fun `the big label says that any key closes it`() {
        val popup = Quelltext.cut(home, "private fun LabelPopup(", "private fun ContactChoice(")
        assertTrue(
            "the big label no longer names the text that mentions the key.",
            "R.string.popup_close_any_key" in popup,
        )
        assertTrue(
            "it no longer closes on any key press.",
            Regex("""KeyEventType\.KeyDown[\s\S]{0,120}onDismiss\(\)""").containsMatchIn(popup),
        )
        assertTrue(
            "the contact choice has taken over the label's sentence. there it is wrong: " +
                "pressing a key there chooses between calling and writing.",
            "R.string.tap_to_close" !in popup,
        )
    }
}
