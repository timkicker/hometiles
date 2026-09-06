package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallLogEmptyTest {

    private var nextId = 0L

    private fun call(kind: CallDirection, number: String = "+4366411111") = CallEntry(
        id = ++nextId,
        number = number,
        name = null,
        direction = kind,
        timestamp = 1_000_000L - nextId,
        durationSeconds = 0,
    )

    private fun groups(vararg calls: CallEntry) = CallLogGrouping.group(calls.toList())

    private val allKinds = CallDirection.entries.toSet()

    @Test
    fun `with visible calls there is no reason`() {
        assertNull(
            CallLogEmpty.reason(groups(call(CallDirection.INCOMING)), false, allKinds),
        )
    }

    @Test
    fun `without a single call the list is really empty`() {
        assertEquals(
            EmptyCallLog.NO_CALLS,
            CallLogEmpty.reason(emptyList(), false, allKinds),
        )
    }

    @Test
    fun `with all kinds hidden the list says so`() {
        // seven calls in the log, every kind unticked, and the screen said there were no
        // calls yet.
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(
                groups(call(CallDirection.INCOMING), call(CallDirection.MISSED, "+4366422222")),
                false,
                emptySet(),
            ),
        )
    }

    @Test
    fun `even a single hidden kind can empty the list`() {
        // not all kinds have to be unticked - it is enough that the calls there all belong
        // to one that is.
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(
                groups(call(CallDirection.BLOCKED)),
                false,
                allKinds - CallDirection.BLOCKED,
            ),
        )
    }

    @Test
    fun `without missed calls it is the filter above`() {
        assertEquals(
            EmptyCallLog.NO_MISSED,
            CallLogEmpty.reason(groups(call(CallDirection.OUTGOING)), true, allKinds),
        )
    }

    @Test
    fun `the hidden kind weighs more than the filter`() {
        // both apply. named is the reason one cannot see: the missed-only filter stands as a
        // button above the list, the choice of kinds sits in the settings.
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(groups(call(CallDirection.OUTGOING)), true, emptySet()),
        )
    }

    @Test
    fun `a missed call stays visible with the filter on`() {
        assertNull(
            CallLogEmpty.reason(groups(call(CallDirection.MISSED)), true, allKinds),
        )
    }
}
