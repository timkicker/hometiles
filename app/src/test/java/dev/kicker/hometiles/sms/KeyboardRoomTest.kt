package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * with the keyboard open there is room for what one needs then.
 *
 * on three inches about 290 pixels are left above the keyboard. at 200 percent system font
 * heading (63), input field (92) and send row (90) together need more - measured on
 * 04.09.2026: the send button was half covered by the keyboard, the word no longer readable.
 * whoever set the large font is exactly the person who needs it.
 *
 * of the three rows the heading is the most dispensable: one has just picked whom one is
 * writing to.
 *
 * the "button above the text" setting helps too, but it is a preference and no rescue -
 * whoever does not know it sees only half a button.
 */
class KeyboardRoomTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/sms/SmsActivity.kt")

    @Test
    fun `the conversation drops the heading while typing`() {
        // Conversation is the file's last function - there is no next one as a boundary, so
        // cut to the end.
        val conversation = Quelltext.cut(source, "private fun Conversation(")
        assertTrue(
            "the conversation does not ask whether the keyboard is open - then the heading " +
                "stands there even when the room for the send button is missing.",
            "WindowInsets.ime.getBottom(" in conversation,
        )
        assertTrue(
            "the heading stands there unconditionally: $conversation",
            "if (!keyboardOpen) BigHeading(title)" in conversation,
        )
    }
}
