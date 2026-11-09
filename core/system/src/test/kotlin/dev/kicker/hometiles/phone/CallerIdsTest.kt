package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CallerIdsTest {

    @Test
    fun `the presentation column decides before the number`() {
        assertEquals(CallerShown.WITHHELD, CallerIds.shownOf(2, ""))
        assertEquals(CallerShown.UNKNOWN, CallerIds.shownOf(3, ""))
        assertEquals(CallerShown.PAYPHONE, CallerIds.shownOf(4, ""))
        assertEquals(CallerShown.UNKNOWN, CallerIds.shownOf(5, ""))
        assertEquals(CallerShown.NUMBER, CallerIds.shownOf(1, "+43660"))
    }

    @Test
    fun `old radio markers in the number column are no number`() {
        // "-2" cleans to the digit 2: a withheld caller stood there as "2" and could be called.
        assertEquals(CallerShown.WITHHELD, CallerIds.shownOf(1, "-2"))
        assertEquals(CallerShown.UNKNOWN, CallerIds.shownOf(null, "-1"))
        assertEquals(CallerShown.PAYPHONE, CallerIds.shownOf(null, "-3"))
        val shown = CallerIds.shownOf(1, "-2")
        assertFalse(PhoneNumbers.isDialable(CallerIds.numberOf(shown, "-2")))
    }

    @Test
    fun `a withheld call keeps no number even when the column has one`() {
        assertEquals("", CallerIds.numberOf(CallerShown.WITHHELD, "+43660"))
        assertEquals("+43660", CallerIds.numberOf(CallerShown.NUMBER, "+43660"))
    }

    @Test
    fun `a blank cached name is no name`() {
        // it stood in the log as an empty row, the report in issue 2.
        assertEquals(null, CallerIds.nameOf(""))
        assertEquals(null, CallerIds.nameOf("  "))
        assertEquals("Anna", CallerIds.nameOf("Anna"))
    }

    @Test
    fun `withheld calls are not folded into one caller`() {
        val groups = CallLogGrouping.group(
            listOf(
                CallEntry(1, "", null, CallDirection.MISSED, 300, 0, CallerShown.WITHHELD),
                CallEntry(2, "", null, CallDirection.MISSED, 200, 0, CallerShown.WITHHELD),
            ),
        )
        assertEquals(2, groups.size)
    }
}
