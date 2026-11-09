package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * keys that belong to the screen arrive even when nothing has the focus.
 *
 * compose hands a key only to the focused node. after a tap nothing is focused (touch mode),
 * and a digit pressed on the dialler went nowhere: reported from a qin f21 on 19.10.2026.
 * the keys are therefore taken at the activity, which gets whatever compose left, by
 * [ScreenKeys].
 */
class ScreenKeysTest {

    private val base = Quelltext.withoutComments("dev/kicker/hometiles/ui/HomeTilesActivity.kt")
    private val keypad = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigKeypad.kt")
    private val search = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigSearchField.kt")
    private val dialer = Quelltext.withoutComments("dev/kicker/hometiles/phone/DialerActivity.kt")
    private val home = Quelltext.withoutComments("dev/kicker/hometiles/ui/HomeScreenView.kt")

    @Test
    fun `the activity asks the screen keys for what compose left`() {
        listOf("onKeyDown", "onKeyUp").forEach { name ->
            val handler = Quelltext.cut(base, "override fun $name", "super.$name")
            assertTrue(
                "$name does not consult the screen keys before handing on. then a digit " +
                    "without a focused node is lost again.",
                "screenKey(event)" in handler,
            )
        }
    }

    @Test
    fun `every screen drops key bounces when asked`() {
        // issue #2: a bounced select on the contact list opened the contact and acted again.
        // in the base class, or one screen forgotten is the one where it happens.
        val onCreate = Quelltext.cut(base, "override fun onCreate", "override fun onResume")
        assertTrue(
            "the base activity no longer puts KeyBounce in front of its window.",
            "window.dropKeyBounces" in onCreate && "behaviour.keyBounce" in onCreate,
        )
    }

    @Test
    fun `dispatchKeyEvent stays untouched`() {
        // androidx restricts that override to its own library group; lint failed the ci on
        // it on 19.10.2026, while every local test was green.
        assertFalse(
            "dispatchKeyEvent is overridden again. lint rejects it (RestrictedApi).",
            "override fun dispatchKeyEvent" in base,
        )
    }

    @Test
    fun `the keypad types with the phone's own keys`() {
        val keys = Quelltext.cut(keypad, "ScreenKeys {", "val rows")
        assertTrue("the keypad does not read digit keys through dialChar.", "dialChar(event)" in keys)
        assertTrue("a digit key does not reach onDigit.", "onDigit(" in keys)
        assertTrue("the delete key does not delete.", "KEYCODE_DEL" in keys && "onBackspace()" in keys)
        assertTrue(
            "a held digit key repeats on its own and would type the digit twenty times.",
            "repeatCount == 0" in keys,
        )
    }

    @Test
    fun `the green key never dials`() {
        // `PLAN.md` 10.3.3: a key that dials without a second press is not built here.
        val green = Quelltext.cut(dialer, "KEYCODE_CALL", "Row(", atMost = 600)
        assertTrue("the green key no longer leads to the call row.", "requestFocus" in green)
        assertFalse(
            "the green key dials. it may only move the focus to the call row; dialling takes " +
                "the select key there, the same second press a finger needs.",
            "dial(" in green || "onCall" in green,
        )
    }

    @Test
    fun `a keyboard types into the search from the list`() {
        val keys = Quelltext.cut(search, "ScreenKeys(", "Row(")
        assertTrue("the search does not read letters through searchCharFor.", "searchCharFor(" in keys)
        assertTrue("the first letter does not move the focus into the field.", "caret.requestFocus()" in keys)
        assertTrue(
            "the screen keys of the search stay on while the field has the focus. then " +
                "every letter would arrive twice.",
            "enabled = !inField" in keys,
        )
    }

    @Test
    fun `the home screen picks tiles with the same digits`() {
        assertTrue(
            "the home screen reads its digits past dialCharFor. then the numeric pad and " +
                "the alt digits of a keyboard pick nothing.",
            "dialCharFor(" in home,
        )
        assertFalse("a list of Key.One to Key.Nine is back beside the shared mapping.", "Key.One" in home)
    }
}
