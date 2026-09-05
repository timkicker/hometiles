package org.biglau.ui

import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2 asks for the app's own large battery and signal display for full screen. the
 * reason behind it is a question: can the user still see what time it is and how full the
 * battery is?
 *
 * in full screen the system bar is gone and the app's header carries both. switching both
 * off at once is allowed - it is a home screen, not a cockpit - but it is the kind of
 * setting one hits by accident and then cannot place: the phone simply shows no time any
 * more, and one looks for the fault in the phone.
 */
class StatusVisibilityTest {

    @Test
    fun `without full screen the system bar carries everything`() {
        val without = Appearance(fullScreen = false, showHeader = false)
        assertEquals(true, StatusVisibility.showsTime(without))
        assertEquals(true, StatusVisibility.showsBattery(without))
        assertEquals(false, StatusVisibility.warns(without))
    }

    @Test
    fun `in full screen the header carries everything`() {
        val withHeader = Appearance(fullScreen = true, showHeader = true)
        assertEquals(true, StatusVisibility.showsTime(withHeader))
        assertEquals(true, StatusVisibility.showsBattery(withHeader))
        assertEquals(false, StatusVisibility.warns(withHeader))
    }

    // the case this is about.
    @Test
    fun `full screen without a header shows nothing any more`() {
        val blind = Appearance(fullScreen = true, showHeader = false)
        assertEquals(false, StatusVisibility.showsTime(blind))
        assertEquals(false, StatusVisibility.showsBattery(blind))
        assertEquals(true, StatusVisibility.warns(blind))
    }

    /**
     * the half case: header on, clock off. then the battery level stands there and the time
     * does not - the warning has to tell them apart, otherwise it names something visible
     * and becomes unbelievable.
     */
    @Test
    fun `with a header but without a clock only the time is missing`() {
        val withoutClock = Appearance(
            fullScreen = true,
            showHeader = true,
            clockDisplay = ClockDisplay.OFF,
        )
        assertEquals(false, StatusVisibility.showsTime(withoutClock))
        assertEquals(true, StatusVisibility.showsBattery(withoutClock))
        assertEquals(true, StatusVisibility.warns(withoutClock))
    }

    // the default never warns - otherwise a freshly set up app would carry a red row, and
    // red rows that are always there go unread.
    @Test
    fun `the default does not warn`() {
        assertEquals(false, StatusVisibility.warns(Appearance()))
    }
}
