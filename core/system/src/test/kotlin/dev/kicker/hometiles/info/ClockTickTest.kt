package dev.kicker.hometiles.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTickTest {

    @Test
    fun `mid-minute the remainder stays`() {
        // 20 seconds after the full minute leave 40 seconds open.
        assertEquals(40_000L, ClockTick.millisUntilNextMinute(20_000L))
    }

    @Test
    fun `on the full minute a whole one is waited`() {
        assertEquals(60_000L, ClockTick.millisUntilNextMinute(0L))
        assertEquals(60_000L, ClockTick.millisUntilNextMinute(120_000L))
    }

    @Test
    fun `just before the minute only the remainder is waited`() {
        assertEquals(1L, ClockTick.millisUntilNextMinute(59_999L))
    }

    @Test
    fun `the wait is never zero or negative`() {
        // otherwise the loop would spin free and roast the battery.
        (0L..120_000L step 997L).forEach { now ->
            val wait = ClockTick.millisUntilNextMinute(now)
            assertTrue("at $now it was $wait", wait in 1L..60_000L)
        }
    }

    @Test
    fun `the display does not drift out of step`() {
        // simply waiting 60 seconds would push the change further into the minute with every
        // round. so it counts to the next full minute instead.
        var now = 20_000L
        repeat(5) {
            now += ClockTick.millisUntilNextMinute(now)
            assertEquals("the change does not fall on the full minute", 0L, now % 60_000L)
        }
    }
}
