package dev.kicker.hometiles.info

import dev.kicker.hometiles.data.SosConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BatteryWarningTest {

    @Test
    fun `it warns when the level is reached`() {
        assertEquals(BatteryStep.WARN, BatteryWarning.step(10, charging = false, level = 10, warned = false))
        assertEquals(BatteryStep.WARN, BatteryWarning.step(4, charging = false, level = 10, warned = false))
        assertEquals(BatteryStep.NOTHING, BatteryWarning.step(11, charging = false, level = 10, warned = false))
    }

    @Test
    fun `once per discharge`() {
        // every further percent below the mark arrives as a reading of its own.
        listOf(9, 8, 5, 1).forEach { percent ->
            assertEquals(BatteryStep.NOTHING, BatteryWarning.step(percent, charging = false, level = 10, warned = true))
        }
    }

    @Test
    fun `charging arms it again`() {
        assertEquals(BatteryStep.RESET, BatteryWarning.step(8, charging = true, level = 10, warned = true))
        assertEquals(BatteryStep.NOTHING, BatteryWarning.step(8, charging = true, level = 10, warned = false))
    }

    @Test
    fun `no warning while it charges or without a level`() {
        assertEquals(BatteryStep.NOTHING, BatteryWarning.step(3, charging = true, level = 10, warned = false))
        assertEquals(BatteryStep.NOTHING, BatteryWarning.step(null, charging = false, level = 10, warned = false))
    }

    @Test
    fun `a level from an older backup falls back`() {
        assertEquals(15, BatteryWarning.clamp(15))
        assertEquals(BatteryWarning.DEFAULT_LEVEL, BatteryWarning.clamp(7))
    }

    @Test
    fun `both are off until someone switches them on`() {
        // a text to relatives that nobody asked for would be worse than none.
        assertFalse(SosConfig().batteryNotice)
        assertFalse(SosConfig().batteryText)
        assertEquals(BatteryWarning.DEFAULT_LEVEL, SosConfig().batteryLevel)
    }
}
