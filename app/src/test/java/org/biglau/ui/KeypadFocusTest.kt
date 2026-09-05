package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the number pad leads the focus itself instead of letting it be searched for.
 *
 * PLAN.md 10.3.5. as an activity's screen it does not need to: the window gets the focus and
 * compose finds the first target. as an **overlay** over the home screen it does - there is
 * still a screen underneath, and the search for the next target runs into it.
 *
 * measured on 04.09.2026 with a locked app tapped: eleven clickable areas, **zero** reached.
 * the whole keypad was unreachable, and with it every locked app.
 *
 * a `moveFocus` in the key's direction was the first attempt and did not help: at the edge of
 * the keypad it left the overlay, and afterwards the focus was gone entirely. same rule as in
 * the grid frame and the strip below it: no blind moveFocus, but named anchors.
 *
 * measured after the rebuild: locked app 11 of 11, pin gate before the editor unchanged at
 * 11 of 11.
 */
class KeypadFocusTest {

    private val keypad = Quelltext.withoutComments("org/biglau/ui/BigKeypad.kt")
    private val gate = Quelltext.withoutComments("org/biglau/ui/PinGate.kt")

    @Test
    fun `every key has an anchor`() {
        assertTrue(
            "the keypad lays out no anchors any more. without them it cannot lead the " +
                "focus, and under an overlay the focus runs away.",
            Regex("""anchors = remember\([\s\S]{0,80}FocusRequester\(\)""").containsMatchIn(keypad),
        )
        assertTrue(
            "the anchors no longer hang on the keys.",
            "focusRequester(anchors[rowIndex][columnIndex])" in keypad,
        )
    }

    @Test
    fun `every direction key is consumed`() {
        val path = Quelltext.cut(keypad, ".onPreviewKeyEvent { event ->", "verticalArrangement")
        listOf("DirectionUp", "DirectionDown", "DirectionLeft", "DirectionRight").forEach {
            assertTrue(
                "Key.$it is not handled. a direction key that slips through lets compose " +
                    "search on its own - and under an overlay it finds a tile nobody sees.",
                "Key.$it ->" in path,
            )
        }
        assertTrue(
            "not every PadDirection returns true. at the edge the key then slips through.",
            path.split("true").size >= 5,
        )
    }

    /** the same rule as in the grid frame. it stands here a second time because it was
     * broken here a second time. */
    @Test
    fun `nothing is searched for`() {
        assertTrue(
            "a moveFocus stands in the keypad again. it looks for a target itself, and " +
                "under an overlay the next one is a tile nobody sees - measured on " +
                "04.09.2026, after ten times up the focus was gone.",
            "moveFocus" !in keypad,
        )
    }

    @Test
    fun `done stands below the last row`() {
        assertTrue(
            "the keypad knows no way out downwards. the done row below it would then be " +
                "unreachable by key.",
            "below?.let" in keypad,
        )
        assertTrue(
            "the pin entry no longer passes the anchor of its done row through.",
            "below = doneAnchor" in gate && "focusRequester(doneAnchor)" in gate,
        )
        assertTrue(
            "the pin entry no longer lets the keypad fetch the focus.",
            "takesFocus = true" in gate,
        )
    }
}
