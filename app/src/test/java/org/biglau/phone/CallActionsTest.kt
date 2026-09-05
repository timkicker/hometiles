package org.biglau.phone

import org.biglau.data.AudioRoute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallActionsTest {

    private fun view(
        status: CallStatus,
        started: Long? = null,
        muted: Boolean = false,
        speaker: Boolean = false,
        bluetooth: Boolean = false,
        other: String? = null,
        otherOnHold: Boolean = false,
        name: String? = null,
        number: String = "+436601234567",
    ) = CallView(
        status = status,
        number = number,
        name = name,
        startedAtMillis = started,
        muted = muted,
        audioRoute = if (speaker) AudioRoute.SPEAKER else AudioRoute.EARPIECE,
        bluetoothAvailable = bluetooth,
        otherName = other,
        otherHeld = otherOnHold,
    )

    @Test
    fun `a ringing call offers answer and reject`() {
        val actions = CallActions.availableFor(view(CallStatus.RINGING))
        assertEquals(listOf(CallAction.ANSWER, CallAction.REJECT), actions)
    }

    @Test
    fun `a ringing call never shows hang up`() {
        // "hang up" on a ringing call reads like "reject" - and whoever mixes those up has
        // lost the call.
        assertTrue(CallAction.HANG_UP !in CallActions.availableFor(view(CallStatus.RINGING)))
    }

    @Test
    fun `a running call offers hang up but no answer`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.HANG_UP in actions)
        assertTrue(CallAction.ANSWER !in actions)
    }

    @Test
    fun `mute and unmute alternate`() {
        assertTrue(CallAction.MUTE in CallActions.availableFor(view(CallStatus.ACTIVE, muted = false)))
        assertTrue(CallAction.UNMUTE in CallActions.availableFor(view(CallStatus.ACTIVE, muted = true)))
    }

    @Test
    fun `a held call offers resume`() {
        val actions = CallActions.availableFor(view(CallStatus.HOLDING))
        assertTrue(CallAction.UNHOLD in actions)
        assertTrue(CallAction.HOLD !in actions)
    }

    @Test
    fun `an ended call offers nothing any more`() {
        listOf(CallStatus.DISCONNECTED, CallStatus.DISCONNECTING, CallStatus.OTHER).forEach {
            assertTrue(CallActions.availableFor(view(it)).isEmpty())
        }
    }

    @Test
    fun `the duration runs only from connecting on`() {
        assertNull(CallActions.durationSeconds(view(CallStatus.RINGING, started = 1000L), 5000L))
        assertNull(CallActions.durationSeconds(view(CallStatus.DIALING, started = 1000L), 5000L))
        assertEquals(4L, CallActions.durationSeconds(view(CallStatus.ACTIVE, started = 1000L), 5000L))
    }

    @Test
    fun `without a start time there is no duration`() {
        assertNull(CallActions.durationSeconds(view(CallStatus.ACTIVE, started = null), 5000L))
    }

    @Test
    fun `a clock jumping back gives no negative duration`() {
        assertEquals(0L, CallActions.durationSeconds(view(CallStatus.ACTIVE, started = 9000L), 1000L))
    }

    @Test
    fun `the duration is written as minutes and seconds`() {
        assertEquals("0:00", CallActions.formatDuration(0))
        assertEquals("0:07", CallActions.formatDuration(7))
        assertEquals("1:05", CallActions.formatDuration(65))
        assertEquals("59:59", CallActions.formatDuration(3599))
    }

    @Test
    fun `from an hour on hours are added`() {
        assertEquals("1:00:00", CallActions.formatDuration(3600))
        assertEquals("2:03:04", CallActions.formatDuration(7384))
    }

    @Test
    fun `the headline takes the name when there is one`() {
        assertEquals("Oma", CallActions.headline(view(CallStatus.RINGING, name = "Oma"), "Unknown"))
    }

    @Test
    fun `without a name the readably grouped number stands there`() {
        assertEquals("+436 601 234 567", CallActions.headline(view(CallStatus.RINGING, name = null), "Unknown"))
    }

    @Test
    fun `a withheld number gives a fallback instead of an empty line`() {
        assertEquals("Unknown", CallActions.headline(view(CallStatus.RINGING, name = null, number = ""), "Unknown"))
        assertEquals("Unknown", CallActions.headline(view(CallStatus.RINGING, name = "  ", number = ""), "Unknown"))
    }

    // --- the opened keypad (PLAN.md 4.6) ---

    /**
     * with all five rows the keypad was left a strip twelve pixels tall - the digits were not
     * even drawn any more.
     */
    @Test
    fun `beside the keypad only two rows are left`() {
        val actions = CallActions.whileKeypad(view(CallStatus.ACTIVE))
        assertEquals(listOf(CallAction.HANG_UP, CallAction.KEYPAD), actions)
    }

    @Test
    fun `beside the keypad the way back stays visible`() {
        // before, only the back key closed it - nobody guesses that.
        assertTrue(CallAction.KEYPAD in CallActions.whileKeypad(view(CallStatus.ACTIVE)))
    }

    @Test
    fun `beside the keypad hang up stays`() {
        assertTrue(CallAction.HANG_UP in CallActions.whileKeypad(view(CallStatus.ACTIVE)))
    }

    @Test
    fun `the selection is always a subset of the possible rows`() {
        CallStatus.entries.forEach { status ->
            val all = CallActions.availableFor(view(status))
            assertTrue(status.name, CallActions.whileKeypad(view(status)).all { it in all })
        }
    }

    // --- the loudspeaker (PLAN.md 4.6) ---

    /**
     * there was only "speaker", and it switched it *on*; a second press did the same again.
     * whoever turned it on by accident could not get rid of it until hanging up, with the
     * conversation running loudly through the room meanwhile. the intention even stood in the
     * source, as a dead line: `if (view.speakerOn) SPEAKER else SPEAKER`.
     */
    @Test
    fun `with the speaker running it says switch off`() {
        val on = CallActions.availableFor(view(CallStatus.ACTIVE, speaker = true))
        assertTrue(CallAction.SPEAKER_OFF in on)
        assertTrue(CallAction.SPEAKER !in on)
    }

    @Test
    fun `without the speaker it says switch on`() {
        val off = CallActions.availableFor(view(CallStatus.ACTIVE, speaker = false))
        assertTrue(CallAction.SPEAKER in off)
        assertTrue(CallAction.SPEAKER_OFF !in off)
    }

    @Test
    fun `while dialling the speaker can be switched off again too`() {
        val on = CallActions.availableFor(view(CallStatus.DIALING, speaker = true))
        assertTrue(CallAction.SPEAKER_OFF in on)
    }

    @Test
    fun `the number of rows does not change with the speaker`() {
        // otherwise the whole row of buttons would jump when switching, and the finger would
        // hit something else on the second press.
        CallStatus.entries.forEach { status ->
            assertEquals(
                status.name,
                CallActions.availableFor(view(status, speaker = false)).size,
                CallActions.availableFor(view(status, speaker = true)).size,
            )
        }
    }

    // --- the second call (PLAN.md 4.6) ---

    /**
     * `otherCallWaiting` was set and **read nowhere**. reproduced on the emulator: during a
     * call with "Anna", "Bernd" called - the screen showed only Bernd, and after answering
     * Anna was neither to be seen nor to be reached. no button led back to her.
     */
    @Test
    fun `with a held second call it says switch instead of hold`() {
        val actions = CallActions.availableFor(
            view(CallStatus.ACTIVE, other = "Anna Bauer", otherOnHold = true),
        )
        assertTrue(CallAction.SWITCH in actions)
        assertTrue(CallAction.HOLD !in actions)
    }

    @Test
    fun `without a second call it stays with hold`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.HOLD in actions)
        assertTrue(CallAction.SWITCH !in actions)
    }

    @Test
    fun `a ringing second call does not replace hold yet`() {
        // while it rings it is not held - there is nothing to switch to, only to answer or
        // reject.
        val actions = CallActions.availableFor(
            view(CallStatus.ACTIVE, other = "Bernd", otherOnHold = false),
        )
        assertTrue(CallAction.HOLD in actions)
    }

    // --- who stands in front ---

    @Test
    fun `something rings - then the ringing one stands in front`() {
        val index = CallForeground.pick(listOf(CallStatus.ACTIVE, CallStatus.RINGING))
        assertEquals(1, index)
    }

    @Test
    fun `otherwise the running one before the held one`() {
        assertEquals(1, CallForeground.pick(listOf(CallStatus.HOLDING, CallStatus.ACTIVE)))
    }

    @Test
    fun `if only a held one is left, that one stands in front`() {
        assertEquals(0, CallForeground.pick(listOf(CallStatus.HOLDING)))
    }

    @Test
    fun `without a call there is no foreground`() {
        assertNull(CallForeground.pick(emptyList()))
    }

    // --- bluetooth (PLAN.md P5) ---

    /**
     * `PLAN.md` P5 names the bluetooth switch; it was not built, and a sixth row of buttons
     * does not fit on three inches - five already fill the screen. so the switch becomes a
     * choice **as soon as** a device is there: the row says where the sound goes and leads to
     * the three ways.
     */
    @Test
    fun `with bluetooth the switch becomes a choice`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE, bluetooth = true))
        assertTrue(CallAction.AUDIO in actions)
        assertTrue(CallAction.SPEAKER !in actions)
        assertTrue(CallAction.SPEAKER_OFF !in actions)
    }

    @Test
    fun `without bluetooth the switch stays`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.SPEAKER in actions)
        assertTrue(CallAction.AUDIO !in actions)
    }

    @Test
    fun `the number of rows does not change with bluetooth`() {
        CallStatus.entries.forEach { status ->
            assertEquals(
                status.name,
                CallActions.availableFor(view(status)).size,
                CallActions.availableFor(view(status, bluetooth = true)).size,
            )
        }
    }
}
