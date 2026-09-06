package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the missed-calls badge needs no write permission.
 *
 * counting only the system's `NEW = 1` flag and clearing it on opening the list requires
 * `WRITE_CALL_LOG`, and on 03.09.2026 that permission was not granted on the user's device -
 * the number on the tile would never have gone out there.
 *
 * since 04.09.2026 HomeTiles holds the phone role and gets the permission with it. the way
 * without write permission stays the right one: it hangs on no role.
 */
class MissedCallsTest {

    private fun call(id: Long, time: Long) = CallEntry(
        id = id,
        number = "+43123456789",
        name = null,
        direction = CallDirection.MISSED,
        timestamp = time,
        durationSeconds = 0,
    )

    @Test
    fun `both conditions stand in the query`() {
        val condition = MissedCalls.selection()
        assertTrue("the system's flag is missing: $condition", "new = 1" in condition.lowercase())
        assertTrue("the timestamp is missing: $condition", "date >" in condition.lowercase())
    }

    @Test
    fun `the values stand in the same order as the question marks`() {
        val values = MissedCalls.arguments(1_700_000_000_000L)
        assertEquals(2, values.size)
        assertEquals("1700000000000", values[1])
    }

    /** a negative timestamp would come from a broken backup - it counts as never. */
    @Test
    fun `a negative timestamp becomes zero`() {
        assertEquals("0", MissedCalls.arguments(-5L)[1])
    }

    /**
     * time passes between reading the list and storing the mark. with "now" a call arriving
     * exactly in between would be ticked off as seen without the user ever seeing it. the
     * only mistake this badge may make is showing one too many.
     */
    @Test
    fun `the youngest call is remembered, not the current time`() {
        val list = listOf(call(1, 500L), call(2, 900L), call(3, 700L))
        assertEquals(900L, MissedCalls.seenUpTo(previous = 0L, entries = list))
    }

    @Test
    fun `an empty list leaves the timestamp standing`() {
        assertEquals(1234L, MissedCalls.seenUpTo(previous = 1234L, entries = emptyList()))
    }

    /** and the timestamp never goes back - otherwise an old number would return. */
    @Test
    fun `an older call does not set the timestamp back`() {
        assertEquals(1000L, MissedCalls.seenUpTo(previous = 1000L, entries = listOf(call(1, 300L))))
    }
}
