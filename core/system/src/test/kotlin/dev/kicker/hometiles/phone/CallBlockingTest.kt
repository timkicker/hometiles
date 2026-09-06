package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the number block (`PLAN.md` 4.6).
 *
 * the delicate part is the comparison. the same person appears as "+43 664 1234567",
 * "0043 664 1234567" and "0664 1234567" - whoever compares for equality fails to block in
 * two cases out of three. a block that sometimes does not hold is worse than none, because
 * one relies on it.
 */
class CallBlockingTest {

    private val blocked = listOf("+436641234567")

    @Test
    fun `the same number in three spellings is the same block`() {
        assertTrue(CallBlocking.isBlocked("+43 664 1234567", blocked))
        assertTrue(CallBlocking.isBlocked("0043 664 1234567", blocked))
        assertTrue(CallBlocking.isBlocked("0664 1234567", blocked))
        assertTrue(CallBlocking.isBlocked("06641234567", blocked))
    }

    @Test
    fun `another number stays free`() {
        assertFalse(CallBlocking.isBlocked("+436641234568", blocked))
        assertFalse(CallBlocking.isBlocked("+436649999999", blocked))
    }

    @Test
    fun `emergency numbers cannot be blocked`() {
        // PLAN.md 4.6: they always go through. an accidentally blocked 112 would be the
        // most expensive fault this app can make.
        listOf("112", "144", "133").forEach { emergency ->
            assertFalse(emergency, CallBlocking.isBlocked(emergency, listOf(emergency)))
        }
    }

    @Test
    fun `a withheld number cannot be blocked`() {
        // otherwise a single block would hit every anonymous caller at once.
        assertFalse(CallBlocking.isBlocked("", blocked))
        assertFalse(CallBlocking.isBlocked("Unbekannt", blocked))
    }

    @Test
    fun `entries that are too short are not taken`() {
        // otherwise "123" would block every number that happens to end that way.
        assertEquals(emptyList<String>(), CallBlocking.parse("123, 45"))
        assertEquals(listOf("123", "45"), CallBlocking.rejected("123, 45"))
    }

    @Test
    fun `the line is split and duplicates fall away`() {
        val list = CallBlocking.parse("+436641234567, 0664 1234567; 0680 7654321")
        assertEquals(2, list.size)
    }

    @Test
    fun `saving and showing are reversible`() {
        val text = "+436641234567, +436807654321"
        assertEquals(text, CallBlocking.format(CallBlocking.parse(text)))
    }

    @Test
    fun `an empty list blocks nothing`() {
        assertFalse(CallBlocking.isBlocked("+436641234567", emptyList()))
    }

    /**
     * the block ran in `onCallAdded` alone until 02.09.2026 - after android had rung and
     * bound the call view, and the call log showed it as **rejected**, as if the user had
     * pressed it away. with `CallScreening` android asks beforehand: checked on the
     * emulator, `mCallBlockReason = 1`, entry with `type=6` (blocked) instead of `type=5`
     * (rejected), no ringing.
     */
    @Test
    fun `a blocked number is turned away on the way in`() {
        assertTrue(
            CallBlocking.blocksIncoming("+436641234567", incoming = true, blocked = blocked),
        )
    }

    @Test
    fun `outgoing is never turned away`() {
        // otherwise the user dials a number they blocked themselves and nothing happens,
        // without anybody telling them why.
        assertFalse(
            CallBlocking.blocksIncoming("+436641234567", incoming = false, blocked = blocked),
        )
    }

    @Test
    fun `a free number comes through`() {
        assertFalse(
            CallBlocking.blocksIncoming("+436649998888", incoming = true, blocked = blocked),
        )
    }

    @Test
    fun `the emergency number comes through incoming too`() {
        assertFalse(CallBlocking.blocksIncoming("112", incoming = true, blocked = listOf("112")))
    }
}
