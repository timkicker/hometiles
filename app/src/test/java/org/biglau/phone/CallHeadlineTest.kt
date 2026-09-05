package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * who is calling - the name instead of the number.
 *
 * seen at the emulator: a call from a number that stands in the address book still showed the
 * digits, because only what the network sends along (CNAP) was taken, and that comes
 * practically never.
 */
class CallHeadlineTest {

    private fun view(number: String, name: String?) = CallView(
        status = CallStatus.RINGING,
        number = number,
        name = name,
        startedAtMillis = null,
    )

    @Test
    fun `the name comes before the number`() {
        assertEquals("Alex", CallActions.headline(view("+4366012345", "Alex"), "Unknown"))
    }

    // without a name the number stays - in blocks, as everywhere else.
    @Test
    fun `without a name the number stands in blocks`() {
        assertEquals("+436 601 234 5", CallActions.headline(view("+4366012345", null), "Unknown"))
    }

    // an empty name is no name. otherwise nothing at all would stand on the screen, and one
    // would not even know that somebody is calling.
    @Test
    fun `an empty name falls back on the number`() {
        assertEquals("+436 601 234 5", CallActions.headline(view("+4366012345", "  "), "Unknown"))
    }

    // withheld number: neither name nor digits. the replacement word is more honest than an
    // empty line - it says unknown, not broken.
    @Test
    fun `without both the replacement word stays`() {
        assertEquals("Unknown", CallActions.headline(view("", null), "Unknown"))
    }
}
