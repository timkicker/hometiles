package dev.kicker.hometiles.toggles

import dev.kicker.hometiles.Quelltext
import java.io.File
import dev.kicker.hometiles.data.SosConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * alarm sound and flashing light during the sos, `PLAN.md` 4.8.
 *
 * the point had stood in the plan from the first day and was built nowhere - it did not even
 * stand among the open items. found while comparing the plan against the source.
 */
class SosAlarmTest {

    private val sosSource = Quelltext.withoutComments("dev/kicker/hometiles/toggles/SosActivity.kt")

    @Test
    fun `both off means do nothing`() {
        assertFalse(SosAlarm.active(SosConfig()))
        assertTrue(SosAlarm.active(SosConfig(alarmSound = true)))
        assertTrue(SosAlarm.active(SosConfig(alarmFlash = true)))
    }

    /**
     * only after the countdown, not during it. whoever hits the button by accident in a shop
     * and presses it away should not have set off a siren - otherwise they switch the sos
     * off entirely afterwards. checked on the order in the source: the start stands after
     * the countdown and right beside the sending.
     */
    @Test
    fun `the alarm begins only with the sending`() {
        val start = sosSource.indexOf("SosAlarm.start")
        val countdown = sosSource.indexOf("if (remaining == 0) break")
        assertTrue("SosAlarm.start is missing in SosActivity", start > 0)
        assertTrue("the alarm must not begin before the countdown ends", start > countdown)
    }

    /** a sound one could only get rid of by restarting turns the sos into a nuisance. */
    @Test
    fun `the alarm stops with the screen`() {
        assertTrue("SosAlarm.stop is missing in SosActivity", "SosAlarm.stop" in sosSource)
        // asked about the content, not the spelling: the same cleanup now also unregisters
        // the location, and this rule should not break on that.
        val cleanup = Quelltext.cut(sosSource, "onDispose {", "}")
        assertTrue("the onDispose for it is missing: $cleanup", "SosAlarm.stop" in cleanup)
    }
}
