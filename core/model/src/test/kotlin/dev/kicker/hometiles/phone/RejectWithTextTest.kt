package dev.kicker.hometiles.phone

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * turning a call down with a short message.
 *
 * the case that shapes the rule: a withheld number. there is nowhere to write to, so the
 * button must not be there - it would promise a message and send none, and the caller would
 * be turned down without the word that was meant for them.
 */
class RejectWithTextTest {

    private fun ringing(number: String) = CallView(
        status = CallStatus.RINGING,
        number = number,
        name = null,
        startedAtMillis = null,
    )

    @Test
    fun `a ringing call may be turned down with a message`() {
        assertTrue(CallAction.REJECT_WITH_TEXT in CallActions.availableFor(ringing("+447700900123")))
    }

    @Test
    fun `a withheld number offers no message`() {
        assertTrue(CallAction.REJECT_WITH_TEXT !in CallActions.availableFor(ringing("")))
        assertTrue(CallAction.REJECT_WITH_TEXT !in CallActions.availableFor(ringing("unknown")))
    }

    /** answering and turning down stay where they are; the message is the third way, not a swap. */
    @Test
    fun `answer and reject stay`() {
        val actions = CallActions.availableFor(ringing("+447700900123"))
        assertTrue(CallAction.ANSWER in actions)
        assertTrue(CallAction.REJECT in actions)
    }

    @Test
    fun `a running call is not turned down with a message`() {
        val active = ringing("+447700900123").copy(status = CallStatus.ACTIVE)
        assertTrue(CallAction.REJECT_WITH_TEXT !in CallActions.availableFor(active))
    }

    /** the keypad row keeps only hang up and keypad, so the message button cannot hide there. */
    @Test
    fun `the keypad row stays short`() {
        val active = ringing("+447700900123").copy(status = CallStatus.ACTIVE)
        assertTrue(CallAction.REJECT_WITH_TEXT !in CallActions.whileKeypad(active))
    }
}
