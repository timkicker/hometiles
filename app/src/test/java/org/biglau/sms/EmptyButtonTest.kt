package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a button that can do nothing does not look like one either.
 *
 * `send` refuses an empty message - `if (body.isBlank()) return`. that is right; what was
 * wrong is that one could not see it: the send button stood there in full accent colour, one
 * tapped, and **silently nothing** happened. seen on 04.09.2026 with the keyboard open and
 * the field empty.
 *
 * `LaunchFailureTest` holds the same case for the tiles, and `BigRow` says it in its own
 * description: a row with an empty action looks like a button, swallows the tap and is
 * announced as a control.
 */
class EmptyButtonTest {

    private val source = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `sending still refuses empty text`() {
        val send = Quelltext.cut(source, "private fun send(", "\n    }")
        assertTrue(
            "send() accepts an empty message again - then the button may always be " +
                "tappable again, and this rule can go.",
            "if (body.isBlank()) return" in send,
        )
    }

    @Test
    fun `without text the send button is no button`() {
        val button = Quelltext.cut(source, "val sendButton = @Composable {", "\n        }")
        assertTrue(
            "the send button stays tappable although sending does nothing on empty text: " +
                button,
            "onClick = if (draft.isBlank()) {" in button,
        )
        assertTrue(
            "without text the button still looks like one - full accent colour, no effect.",
            "draft.isBlank() -> palette.surfaceDefault" in button,
        )
    }

    /**
     * and the same row on the dial pad. `dial` refuses anything that is not a number, and
     * the call button stood in full accent colour all the same - on the screen one relies on
     * most.
     */
    @Test
    fun `without a number the call button is no button`() {
        val dialer = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")
        val dial = Quelltext.cut(dialer, "private fun dial(", "\n    }")
        assertTrue(
            "dial() accepts everything again - then the button may always be tappable " +
                "again, and this rule can go.",
            "if (!PhoneNumbers.isDialable(number)) return" in dial,
        )
        val keypad = Quelltext.cut(dialer, "private fun Keypad(", "\nprivate fun ")
        assertTrue(
            "the call button stays tappable although dialling does nothing without a number.",
            "onClick = if (dialable) onCall else null" in keypad,
        )
        assertTrue(
            "without a number it still looks like a button.",
            "if (dialable) palette.surfaceAccent else palette.surfaceDefault" in keypad,
        )
    }
}
