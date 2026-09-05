package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * one time of day, one pattern.
 *
 * found on 3.9.2026: `"HH:mm"` stood in three files. header and info tile asked first whether
 * the phone is set to 12 or 24 hours, the **message list** did not. set to 12 hours, the
 * header said 2:30 PM and the list said 14:30 for the same minute.
 *
 * for someone who reads with difficulty those are two different times.
 */
class TimePatternTest {

    @Test
    fun `twenty-four hours without AM and PM`() {
        assertEquals("HH:mm", ClockFormat.timePattern(twentyFourHour = true))
    }

    @Test
    fun `twelve hours with AM and PM`() {
        val pattern = ClockFormat.timePattern(twentyFourHour = false)
        assertTrue("without 'a' AM/PM is missing and 13:00 would look like 1:00", pattern.contains("a"))
        assertTrue("the hour must not be forced to two digits", pattern.startsWith("h:"))
    }
}
