package org.biglau.security

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * why the pin was wrong is said, not only shown.
 *
 * BigLau deliberately makes no sound on this screen. the row of dots is a `liveRegion` and
 * says how many digits stand there - after a failed attempt, that no digit has been entered.
 * that is the **consequence**. the **reason** merely stood there as ordinary text without a
 * `liveRegion`.
 *
 * reproduced on 04.09.2026: four digits, done, digits gone, notice visible - and for someone
 * not looking that sounded exactly like a slipped finger. whoever thinks they mistyped types
 * the same wrong pin again.
 *
 * the rule stands at the error message, not at the file: it is the only place in `PinGate`
 * telling something that does not already stand elsewhere.
 */
class SilentWrongPinTest {

    @Test
    fun `the error message is a liveRegion`() {
        val gate = Quelltext.withoutComments("org/biglau/ui/PinGate.kt")
        val notice = Quelltext.cut(
            gate,
            from = "wrong -> Text(",
            to = "\n                )",
        )
        assertTrue(
            "the wrong-pin notice is not announced: $notice\n" +
                "without a liveRegion one hears only the consequence (the row of dots is " +
                "empty), not the reason - and takes oneself for having mistyped.",
            "liveRegion" in notice,
        )
    }
}
