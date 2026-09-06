package dev.kicker.hometiles.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryInfoTest {

    @Test
    fun `the usual case with a scale of a hundred`() {
        assertEquals(63, BatteryInfo.percent(63, 100))
        assertEquals(0, BatteryInfo.percent(0, 100))
        assertEquals(100, BatteryInfo.percent(100, 100))
    }

    @Test
    fun `another scale is converted`() {
        // some devices report 255 instead of 100. whoever misses that shows 50 of 255 on a
        // half full battery, reading it as 50 percent - out by more than double.
        assertEquals(50, BatteryInfo.percent(128, 255))
        assertEquals(100, BatteryInfo.percent(255, 255))
    }

    @Test
    fun `unusable values give no level`() {
        assertNull(BatteryInfo.percent(-1, 100))
        assertNull(BatteryInfo.percent(50, 0))
        assertNull(BatteryInfo.percent(50, -100))
    }

    @Test
    fun `the level stays between zero and a hundred`() {
        assertEquals(100, BatteryInfo.percent(300, 100))
    }

    @Test
    fun `charging is recognised from the status`() {
        assertTrue(BatteryInfo.isCharging(BatteryInfo.STATUS_CHARGING, 0))
        assertTrue(BatteryInfo.isCharging(BatteryInfo.STATUS_FULL, 0))
    }

    @Test
    fun `charging is recognised from the plug too`() {
        // some devices report the status late but the plug at once.
        assertTrue(BatteryInfo.isCharging(1, plugged = 1))
        assertTrue(!BatteryInfo.isCharging(1, plugged = 0))
    }

    @Test
    fun `under sixteen percent it warns`() {
        assertTrue(BatteryInfo.isLow(15))
        assertTrue(BatteryInfo.isLow(3))
        assertTrue(!BatteryInfo.isLow(16))
        assertTrue(!BatteryInfo.isLow(null))
    }

    @Test
    fun `an unknown level gives no bar`() {
        // an empty bar would look like empty - and that would be a false statement.
        assertNull(BatteryInfo.fraction(null))
        assertEquals(0.63f, BatteryInfo.fraction(63)!!, 0.001f)
    }
}
