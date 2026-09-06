package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * which call is in front while a second one is on the line.
 *
 * `CallForeground.pick` decides what the call screen shows, and nothing checked it. the case
 * that hurts: a second call comes in during a running one. showing the wrong one means either
 * answering blind or hanging up on the wrong person.
 */
class SecondCallTest {

    private fun view(
        status: CallStatus,
        otherName: String? = null,
        otherHeld: Boolean = false,
    ) = CallView(
        status = status,
        number = "+447700900123",
        name = null,
        startedAtMillis = null,
        otherName = otherName,
        otherHeld = otherHeld,
    )

    @Test
    fun `without a call there is nothing in front`() {
        assertNull(CallForeground.pick(emptyList()))
    }

    @Test
    fun `a ringing call comes before a running one`() {
        assertEquals(1, CallForeground.pick(listOf(CallStatus.ACTIVE, CallStatus.RINGING)))
    }

    @Test
    fun `with three calls the ringing one still wins`() {
        assertEquals(
            2,
            CallForeground.pick(listOf(CallStatus.HOLDING, CallStatus.ACTIVE, CallStatus.RINGING)),
        )
    }

    @Test
    fun `a running call comes before a held one`() {
        assertEquals(1, CallForeground.pick(listOf(CallStatus.HOLDING, CallStatus.ACTIVE)))
    }

    @Test
    fun `without ringing and without running the first one stands there`() {
        assertEquals(0, CallForeground.pick(listOf(CallStatus.HOLDING, CallStatus.HOLDING)))
    }

    /** the way back to the waiting one: without it the held call was unreachable. */
    @Test
    fun `with someone on hold the switch takes the place of hold`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE, otherName = "Alex", otherHeld = true))
        assertTrue(CallAction.SWITCH in actions)
        assertTrue(CallAction.HOLD !in actions)
    }

    @Test
    fun `without a second call hold stays hold`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.HOLD in actions)
        assertTrue(CallAction.SWITCH !in actions)
    }

    /** a held call offers no keypad and no mute: they would act on the other line. */
    @Test
    fun `a held call offers only hang up and unhold`() {
        assertEquals(
            listOf(CallAction.HANG_UP, CallAction.UNHOLD),
            CallActions.availableFor(view(CallStatus.HOLDING, otherName = "Alex")),
        )
    }

    /**
     * a conference is the conference plus its members, all as calls of their own. the
     * conference belongs in front; a member there would show the same conversation twice.
     */
    @Test
    fun `a conference member does not go in front`() {
        assertEquals(
            0,
            CallForeground.pick(
                listOf(CallStatus.ACTIVE, CallStatus.ACTIVE, CallStatus.ACTIVE),
                listOf(false, true, true),
            ),
        )
    }

    @Test
    fun `a ringing call still beats the running conference`() {
        assertEquals(
            3,
            CallForeground.pick(
                listOf(CallStatus.ACTIVE, CallStatus.ACTIVE, CallStatus.ACTIVE, CallStatus.RINGING),
                listOf(false, true, true, false),
            ),
        )
    }

    @Test
    fun `a conference member is not the second call`() {
        assertNull(CallForeground.other(listOf(false, true, true), front = 0))
    }

    @Test
    fun `a real second call is found`() {
        assertEquals(1, CallForeground.other(listOf(false, false), front = 0))
    }

    @Test
    fun `with one call there is no other`() {
        assertNull(CallForeground.other(listOf(false), front = 0))
    }

    /** nothing is offered on a call that is over - a button there would act on nothing. */
    @Test
    fun `a finished call offers nothing`() {
        assertEquals(emptyList<CallAction>(), CallActions.availableFor(view(CallStatus.DISCONNECTED)))
        assertEquals(emptyList<CallAction>(), CallActions.availableFor(view(CallStatus.DISCONNECTING)))
    }
}
