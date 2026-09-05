package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the focus border has to be thicker than the blink border ever gets.
 *
 * the focus already moves with the d-pad today, because compose makes a focus target out of
 * every `combinedClickable` - it is only invisible. and the one border that *was* visible
 * meant something else: messages carried a white border because 17 unread waited there. so
 * whoever works with keys sees one highlighted tile and starts another one with the select
 * key.
 *
 * width and not colour, because a state that lives only in the colour reaches nobody with a
 * colour weakness. the number in the corner is drawn independently of the border, so a
 * focused tile with something new still shows its count.
 */
class FocusBorderTest {

    private val source = Quelltext.withoutComments("org/biglau/ui/BigTile.kt")

    private fun number(name: String): Float {
        val hit = Regex("""$name\s*=\s*([0-9.]+)f""").find(source)
            ?: throw AssertionError(
                "the constant $name is gone from BigTile.kt. without it this rule measures " +
                    "nothing and would stay green.",
            )
        return hit.groupValues[1].toFloat()
    }

    @Test
    fun `the focus border is thicker than the blink border`() {
        val blink = number("BADGE_BORDER_DP")
        val focus = number("FOCUS_BORDER_DP")
        assertTrue(
            "the focus border ($focus dp) is not thicker than the blink border at its " +
                "thickest point ($blink dp). then two borders that look alike point at two " +
                "different targets on the home screen.",
            focus > blink,
        )
    }

    @Test
    fun `the difference shows from a metre away too`() {
        // one dp of difference is nothing on three inches; half again is the threshold at
        // which two borders look different.
        val blink = number("BADGE_BORDER_DP")
        val focus = number("FOCUS_BORDER_DP")
        assertTrue(
            "the focus border is only $focus dp against $blink dp. too close to tell apart " +
                "in passing.",
            focus >= blink * 1.5f,
        )
    }

    @Test
    fun `the focus sets the border, not only the colour`() {
        assertTrue(
            "the border width does not know about the focus. a state living only in the " +
                "colour reaches nobody with a colour weakness.",
            Regex("""borderWidth[\s\S]{0,400}focused""").containsMatchIn(source),
        )
    }
}

/**
 * the focus stays in the screen that draws it. PLAN.md 10.3.5.
 *
 * `absorbTouches` swallows every tap beside a tile of the overlay so it does not reach the
 * home screen underneath. that modifier is pure `pointerInput` and has no counterpart for
 * keys, so two things in the grid frame carry the trap instead:
 *
 * 1. the targets come from the cells of **this** screen, so an anchor for a tile below does
 *    not exist and there is nothing to request.
 * 2. each of the four direction keys is consumed even when nothing moves, or compose looks
 *    for a target itself - at worst in the screen below or in the header.
 */
class FocusTrapTest {

    private val source = Quelltext.withoutComments("org/biglau/ui/HomeScreenView.kt")

    @Test
    fun `the targets come from this screen only`() {
        assertTrue(
            "the grid frame no longer works with this screen's targets. then an anchor from " +
                "another screen can be meant.",
            "FocusOrder.neighbour(targets," in source,
        )
        assertTrue(
            "the anchors are not built from the targets.",
            Regex("""anchors\s*=\s*remember\(targets\)""").containsMatchIn(source),
        )
    }

    @Test
    fun `every direction key is consumed`() {
        val place = Quelltext.cut(source, ".onPreviewKeyEvent { key ->", "else -> false")
        listOf("DirectionLeft", "DirectionRight", "DirectionUp", "DirectionDown").forEach {
            assertTrue(
                "Key.$it is not handled. a direction key slipping through lets compose look " +
                    "for a target itself.",
                "Key.$it -> move(" in place,
            )
        }
        // the return value of move decides whether the key is consumed: it stands once at the
        // end and is always true, even when no neighbour was found.
        val move = Quelltext.cut(source, "fun move(", ".onPreviewKeyEvent { key ->")
        assertTrue(
            "move does not always return true. at the edge the key then slips through and " +
                "compose looks for a target itself: " + move,
            "return false" !in move && "return true" in move,
        )
    }
}
