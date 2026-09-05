package org.biglau.phone

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the call log does not dial straight away.
 *
 * `PLAN.md` 3.1, principle 5 names three things needing a confirmation: deleting, calling
 * from the log, sos. deleting and sos asked, calling did not - a tap on a row dialled. in a
 * list somebody goes through with a shaky hand, a tap beside the mark is then a call to a
 * person.
 *
 * checked where it went wrong: the call log's row must not dial directly.
 */
class CallConfirmTest {

    private val source = Quelltext.file("org/biglau/phone/DialerActivity.kt").readText()

    @Test
    fun `the row in the log asks first`() {
        val row = Quelltext.cut(source, "items(groups, key =", "onLongClick")
        assertTrue("the row calls onAskCall: $row", "onAskCall(" in row)
        assertTrue("the row must not dial directly: $row", "onCall(" !in row)
    }

    /** both ways out of the question exist - otherwise it would be a dead end. */
    @Test
    fun `the confirmation has a yes and a no`() {
        assertTrue("calllog_call_yes is missing", "R.string.calllog_call_yes" in source)
        assertTrue("calllog_call_no is missing", "R.string.calllog_call_no" in source)
        assertTrue("onCancelCall is missing", "onCancelCall" in source)
        assertTrue("onConfirmCall is missing", "onConfirmCall" in source)
    }

    /**
     * the emergency path stays untouched: it goes to the system dialer anyway, not through
     * this row. see [PhoneNumbers.looksLikeEmergency] and `dial`.
     */
    @Test
    fun `the emergency path stays before the block`() {
        val dial = Quelltext.cut(source, "private fun dial(", "\n    }")
        assertTrue(
            "the emergency number has to be checked before the number block",
            dial.indexOf("looksLikeEmergency") < dial.indexOf("CallBlocking.isBlocked"),
        )
    }
}
