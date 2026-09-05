package org.biglau.ui

import java.text.SimpleDateFormat
import java.util.Locale
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * shorten first, then wrap - the date on the clock tile too.
 *
 * seen on the user's phone (03.09.2026): the 1x1 tile showed "Wednesday, September" and
 * below it the **2 alone**. the source said there was room on the tile for the long names -
 * a claim about a device nobody had measured.
 *
 * this rule checks the ladder and what is measured with. whether a step fits is decided by
 * the measurement in `ClockContent`; what is held here is that a shorter step *exists* at
 * all and that it is shorter.
 */
class ClockDateLadderTest {

    @Test
    fun `the tile has a shorter step than the long form`() {
        val steps = ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE_WEEKDAY, onTile = true)
        assertTrue("no step to shorten to: $steps", steps.size >= 2)
        assertEquals("EEEEdMMMM", steps.first())
        assertTrue("the ladder does not get shorter: $steps", steps.last().length < steps.first().length)
    }

    @Test
    fun `without a weekday it goes from the long to the short month just the same`() {
        assertEquals(
            listOf("dMMMM", "dMMM"),
            ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE, onTile = true),
        )
    }

    /** the header is narrow and starts at the short form - one step, no ladder. */
    @Test
    fun `the header stays at the short form`() {
        assertEquals(listOf("EEEdMMM"), ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false))
        assertEquals(listOf("dMMM"), ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE, onTile = false))
    }

    @Test
    fun `without a date there are no steps`() {
        assertTrue(ClockFormat.dateSkeletons(ClockDisplay.OFF, onTile = true).isEmpty())
        assertTrue(ClockFormat.dateSkeletons(ClockDisplay.TIME, onTile = true).isEmpty())
    }

    /** the old question about *one* step stays answered - it is now the first. */
    @Test
    fun `dateSkeleton is the first step`() {
        ClockDisplay.entries.forEach { display ->
            listOf(true, false).forEach { onTile ->
                assertEquals(
                    ClockFormat.dateSkeletons(display, onTile).firstOrNull(),
                    ClockFormat.dateSkeleton(display, onTile),
                )
            }
        }
    }

    /**
     * measured with the year's longest date, not with today's. otherwise the tile would look
     * different depending on the weekday.
     */
    @Test
    fun `the longest date takes the longest weekday and month`() {
        val format = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
        val longest = ClockFormat.longestDate(format)
        assertTrue("Wednesday is missing: $longest", "Wednesday" in longest)
        assertTrue("September is missing: $longest", "September" in longest)
    }

    @Test
    fun `the short form really is shorter than the long one`() {
        val long = ClockFormat.longestDate(SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH))
        val short = ClockFormat.longestDate(SimpleDateFormat("EEE, d MMM", Locale.ENGLISH))
        assertTrue("$short is not shorter than $long", short.length < long.length)
    }
}
