package dev.kicker.hometiles.toggles

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * after an sos a call button stands there - and it does not dial by itself.
 *
 * `PLAN.md` 4.8 demands both: a call button afterwards, no automatic call. a big button that
 * opens the dial pad with the number is one tap away and never triggers on its own.
 *
 * until 03.09.2026 the screen ended after sending with "close" - in the worst case together
 * with the sentence that nothing could be sent. that is the moment a person can think least,
 * and it offered nothing.
 *
 * two assurances, and the second is the more important: the button is there, and it calls
 * `Intents.dial`, **not** `Intents.call`. `dial` opens the dial pad with the number; the
 * dialling happens only through a second tap by a person. that is why `SosActivity` does not
 * stand in `OutgoingTest`'s list either.
 */
class SosCallButtonTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/toggles/SosActivity.kt")

    @Test
    fun `after sending a call button stands there`() {
        assertTrue(
            "no call button after the sos - PLAN.md 4.8 demands it",
            "R.string.sos_call_now" in source && "Intents.dial(" in source,
        )
    }

    @Test
    fun `the sos screen never dials by itself`() {
        assertFalse(
            "SosActivity calls Intents.call - the sos must never dial by itself",
            "Intents.call(" in source,
        )
        assertFalse("ACTION_CALL in the sos screen", "ACTION_CALL" in source)
    }

    @Test
    fun `the button does not stand in the preview`() {
        val place = source.indexOf("R.string.sos_call_now")
        val before = source.substring(maxOf(0, place - 300), place)
        assertTrue(
            "the button has to hang on !preview - a preview has called nobody and should " +
                "not invite it either",
            "!preview" in before,
        )
    }
}
