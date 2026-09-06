package dev.kicker.hometiles.sms

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * when a message came - the conversation carried no time at all.
 *
 * whoever reads a message wants to know whether it is from just now or from last week. on "on
 * my way" that is the whole difference.
 */
class MessageStampsTest {

    private fun time(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, day, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // the very first message always gets a day heading - otherwise the topmost message would
    // be the only one without a place in time.
    @Test
    fun `the first message gets a day`() {
        assertEquals(true, MessageStamps.startsNewDay(null, time(2, 10)))
    }

    @Test
    fun `the same day gets no second heading`() {
        assertEquals(false, MessageStamps.startsNewDay(time(2, 9), time(2, 23)))
    }

    @Test
    fun `a new day gets a heading`() {
        assertEquals(true, MessageStamps.startsNewDay(time(1, 23), time(2, 0)))
    }

    // across midnight only minutes lie between them and still two days - exactly the case a
    // difference in hours would get wrong.
    @Test
    fun `a few minutes across midnight are two days`() {
        val beforeMidnight = time(1, 23) + 59 * 60_000
        val afterMidnight = time(2, 0) + 60_000
        assertEquals(false, MessageStamps.sameDay(beforeMidnight, afterMidnight))
        assertEquals(true, MessageStamps.startsNewDay(beforeMidnight, afterMidnight))
    }

    // and the same day in another year is not the same day.
    @Test
    fun `the same day in another year does not count as equal`() {
        val now = time(2, 12)
        val aYearAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, -1)
        }.timeInMillis
        assertEquals(false, MessageStamps.sameDay(aYearAgo, now))
    }
}
