package org.biglau.ui

import kotlinx.serialization.json.Json
import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PLAN.md 4.2: clock on the home screen - off / time / time+date / time+date+weekday.
 *
 * the weekday is the step that helps most and costs most room. whoever is not sure of the day
 * - more common than one thinks when the days look alike - reads it here.
 */
class ClockFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * "no clock" on a clock tile would look broken, and nothing about the tile would say it
     * was a setting.
     */
    @Test
    fun `the clock tile always shows the time`() {
        assertEquals(true, ClockFormat.showsTime(ClockDisplay.OFF, onTile = true))
        assertEquals(false, ClockFormat.showsTime(ClockDisplay.OFF, onTile = false))
    }

    @Test
    fun `without a date there is no date line`() {
        assertNull(ClockFormat.dateSkeleton(ClockDisplay.OFF, onTile = false))
        assertNull(ClockFormat.dateSkeleton(ClockDisplay.TIME, onTile = false))
        assertNull(ClockFormat.dateSkeleton(ClockDisplay.TIME, onTile = true))
    }

    @Test
    fun `the weekday arrives only in the last step`() {
        val header = ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE, onTile = false)!!
        val headerWithDay = ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false)!!
        assertEquals(false, header.contains("E"))
        assertEquals(true, headerWithDay.contains("E"))
    }

    // the header has one line, the tile a whole surface: "Wednesday" fits there, in the
    // header "Wed" has to do.
    @Test
    fun `the tile may have the long names`() {
        assertEquals("EEEEdMMMM", ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE_WEEKDAY, onTile = true))
        assertEquals("EEEdMMM", ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false))
    }

    @Test
    fun `old configurations are carried over`() {
        assertEquals(
            ClockDisplay.TIME_DATE_WEEKDAY,
            json.decodeFromString<Appearance>("""{"clockShowsDate":true}""").clock,
        )
        assertEquals(
            ClockDisplay.TIME,
            json.decodeFromString<Appearance>("""{"clockShowsDate":false}""").clock,
        )
    }

    @Test
    fun `withClock keeps both fields equal`() {
        assertEquals(false, Appearance().withClock(ClockDisplay.TIME).clockShowsDate)
        assertEquals(false, Appearance().withClock(ClockDisplay.OFF).clockShowsDate)
        assertEquals(true, Appearance().withClock(ClockDisplay.TIME_DATE).clockShowsDate)
        assertEquals(
            ClockDisplay.TIME_DATE,
            Appearance().withClock(ClockDisplay.TIME_DATE).clock,
        )
    }

    // the default is what the app painted before.
    @Test
    fun `the default shows everything`() {
        assertEquals(ClockDisplay.TIME_DATE_WEEKDAY, Appearance().clock)
    }
}

/**
 * PLAN.md 4.2: the clock's size is free.
 *
 * the header is the only place the time stands once full screen takes the system bar away.
 * whoever cannot read it there has no second one.
 */
class ClockScaleTest {

    @Test
    fun `the default changes nothing`() {
        assertEquals(1.0f, org.biglau.data.Appearance().clockScale, 0.001f)
        assertEquals(1.0f, ClockFormat.scale(1.0f), 0.001f)
    }

    // an imported file can contain anything: a clock at size zero would be an empty header,
    // one at size ten would push everything else out.
    @Test
    fun `impossible values are clamped`() {
        assertEquals(ClockFormat.SCALE_MIN, ClockFormat.scale(0f), 0.001f)
        assertEquals(ClockFormat.SCALE_MAX, ClockFormat.scale(10f), 0.001f)
    }

    @Test
    fun `every offered step lies in the allowed range`() {
        for (value in ClockFormat.SCALES) {
            assertEquals(value, ClockFormat.scale(value), 0.001f)
        }
    }

    @Test
    fun `the default is on offer too`() {
        assertEquals(true, ClockFormat.SCALES.contains(org.biglau.data.Appearance().clockScale))
    }

    @Test
    fun `the header grows with the text size`() {
        // computed from the clock size alone, the header stayed as tall at 150 % text as at
        // 100 % and cut the date line through the middle.
        val at100 = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.0f)
        val at150 = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.5f, clockScale = 1.0f)
        assertEquals(78f, at100, 0.01f)
        assertEquals(117f, at150, 0.01f)
        assertTrue("larger type needs more height", at150 > at100)
    }

    @Test
    fun `the header grows with the clock size too`() {
        val small = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.0f)
        val large = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.0f, clockScale = 1.5f)
        assertTrue("a larger clock needs more height", large > small)
    }

    @Test
    fun `both factors work together`() {
        // both sliders turned up is what someone with bad eyes really sets.
        val both = ClockFormat.headerHeightDp(hasDate = true, textScale = 1.5f, clockScale = 1.5f)
        assertEquals(78f * 1.5f * 1.5f, both, 0.01f)
    }

    @Test
    fun `without a date line the header is lower`() {
        assertTrue(
            ClockFormat.headerHeightDp(hasDate = false, textScale = 1f, clockScale = 1f) <
                ClockFormat.headerHeightDp(hasDate = true, textScale = 1f, clockScale = 1f),
        )
    }

    @Test
    fun `a nonsensical clock size is bounded`() {
        // headerHeightDp goes through the same clamp as the type, or the header would run out
        // of the picture while the type stayed inside it.
        assertEquals(
            ClockFormat.headerHeightDp(hasDate = true, textScale = 1f, clockScale = 99f),
            78f * ClockFormat.scale(99f),
            0.01f,
        )
    }

    @Test
    fun `the clock fits beside the battery level`() {
        // at 200 % clock and battery ran into each other. on the jelly 2's 349 dp both have to
        // fit side by side.
        listOf(1.0f, 1.5f, 2.0f).forEach { scale ->
            val widthLeft = 349f - 16f - 24f - ClockFormat.batteryWidthDp(scale)
            val size = ClockFormat.clockSizeSp("10:38 AM", widthLeft, scale, 1.0f)
            val needed = "10:38 AM".length * 0.62f * size
            assertTrue(
                "at $scale the clock needs $needed dp, $widthLeft are free",
                needed <= widthLeft + 0.01f,
            )
        }
    }

    @Test
    fun `at small type the clock stays as large as asked`() {
        // the bound may only bite when it gets tight, or the clock size setting would be a
        // dummy.
        assertEquals(26f, ClockFormat.clockSizeSp("9:37", 300f, 1.0f, 1.0f), 0.01f)
        assertEquals(39f, ClockFormat.clockSizeSp("9:37", 300f, 1.0f, 1.5f), 0.01f)
    }

    @Test
    fun `a long time is set smaller than a short one`() {
        val short = ClockFormat.clockSizeSp("9:37", 120f, 2.0f, 1.0f)
        val long = ClockFormat.clockSizeSp("12:38 AM", 120f, 2.0f, 1.0f)
        assertTrue("the longer time needs the smaller type", long < short)
    }

    @Test
    fun `the clock never gets unreadably small`() {
        assertEquals(14f, ClockFormat.clockSizeSp("12:38 AM", 10f, 1.0f, 1.0f), 0.01f)
    }

    @Test
    fun `the battery level grows with the text size`() {
        assertTrue(ClockFormat.batteryWidthDp(2.0f) > ClockFormat.batteryWidthDp(1.0f))
    }

    /**
     * on the english jelly 2 the clock tile read "Wednesday, 2. September" - english name,
     * german full stop, german order. the pattern stood fixed in the source and was only
     * *filled* with the language. now the logic says only **which parts** the date has; how
     * they are arranged the language knows.
     */
    @Test
    fun `the skeleton holds no punctuation and no order`() {
        listOf(
            ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE, onTile = true),
            ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE, onTile = false),
            ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE_WEEKDAY, onTile = true),
            ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false),
        ).forEach { skeleton ->
            assertNotNull(skeleton)
            assertEquals("no full stop in the skeleton: $skeleton", false, skeleton!!.contains("."))
            assertEquals("no comma in the skeleton: $skeleton", false, skeleton.contains(","))
            assertEquals("no space in the skeleton: $skeleton", false, skeleton.contains(" "))
        }
    }

    @Test
    fun `day and month stand in every skeleton`() {
        listOf(true, false).forEach { onTile ->
            val skeleton = ClockFormat.dateSkeleton(ClockDisplay.TIME_DATE, onTile = onTile)!!
            assertEquals(true, skeleton.contains("d"))
            assertEquals(true, skeleton.contains("M"))
        }
    }
}
