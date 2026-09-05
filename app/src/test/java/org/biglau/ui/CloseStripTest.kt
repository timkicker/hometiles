package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the strip below the grid can be reached and left again by key. PLAN.md 10.3.5.
 *
 * in a folder and in the menu key's list a row below the grid closes the overlay. it is a
 * sibling of the grid frame, not its child, and the frame consumes every direction key, so
 * the focus never reached it: nine clickable surfaces in the folder, eight reached, and the
 * ninth was the strip. a visible way the keys do not hold is worse than none.
 *
 * **the trap, paid for dearly:** once the strip was reachable, a press to the right lost the
 * focus **entirely**. compose then searches itself and finds nothing, because the row spans
 * the full width - and afterwards no key helped, since without focus no key handler runs. so
 * the rules here check **both directions**: there and back, and that nothing slips through in
 * between.
 */
class CloseStripTest {

    private val frame = Quelltext.withoutComments("org/biglau/ui/HomeScreenView.kt")
    private val home = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    /** the row that closes the overlay, with its modifiers. */
    private val strip =
        Quelltext.cut(home, "stringResource(closeLabel)", "onClick = onClose")

    @Test
    fun `below the last row a key leads to the strip`() {
        val move = Quelltext.cut(frame, "fun move(", ".onPreviewKeyEvent { key ->")
        assertTrue(
            "the grid frame no longer sends the focus to the strip at the bottom edge. then " +
                "a row stands there that no key press reaches: " + move,
            "PadDirection.DOWN" in move && "below" in move,
        )
    }

    /**
     * `moveFocus(Down)` takes the next focusable node. under the overlay lie the home screen's
     * tiles, in the same places as the grid above, so the focus landed there invisibly -
     * exactly what `FocusTrapTest` is meant to prevent.
     */
    @Test
    fun `the way is a named anchor, not a blind search`() {
        assertTrue(
            "a moveFocus stands in the grid frame again. that looks for a target itself, and " +
                "under an overlay the next one is a tile nobody sees.",
            "moveFocus" !in frame,
        )
        assertTrue(
            "the strip carries no anchor any more, so the frame does not know where to go.",
            "focusRequester(belowAnchor)" in strip,
        )
    }

    @Test
    fun `from the strip a key leads back into the grid`() {
        assertTrue(
            "the strip no longer sends the focus back up into the grid. then it is a dead " +
                "end: " + strip,
            Regex("""Key\.DirectionUp[\s\S]{0,120}backAnchor\.requestFocus""")
                .containsMatchIn(strip),
        )
        assertTrue(
            "the frame no longer offers the way back. the strip's anchor then points at " +
                "nothing.",
            "gridAnchor" in frame,
        )
    }

    /**
     * the three remaining directions are consumed and do nothing. this is the case that made
     * the focus disappear: a direction key slipping through on the strip lets compose search
     * itself, and if it finds nothing the focus is gone.
     */
    @Test
    fun `the remaining directions do not slip through`() {
        listOf("DirectionDown", "DirectionLeft", "DirectionRight").forEach {
            assertTrue(
                "Key.$it is not consumed on the strip. one press there and the focus is " +
                    "gone: " + strip,
                "Key.$it" in strip,
            )
        }
        assertTrue(
            "the consumed directions no longer return true.",
            Regex("""DirectionRight -> true""").containsMatchIn(strip),
        )
    }
}
