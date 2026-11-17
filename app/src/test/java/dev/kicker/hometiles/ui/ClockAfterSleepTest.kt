package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a clock on screen shows the time right after the phone wakes.
 *
 * both clocks waited for the next minute with `delay`, which counts on the monotonic clock
 * and stands still in sleep. after waking they kept the old minute until that wait ran out
 * (user mail 14.11.2026). [rememberNow] also listens to the system and to the screen coming
 * back, and every clock has to read it.
 */
class ClockAfterSleepTest {

    @Test
    fun `the shared time follows the system and the screen`() {
        val now = Quelltext.withoutComments("dev/kicker/hometiles/ui/ClockNow.kt")
        listOf("Intent.ACTION_TIME_TICK", "Intent.ACTION_TIME_CHANGED", "Intent.ACTION_TIMEZONE_CHANGED", "Lifecycle.Event.ON_START")
            .forEach { assertTrue("rememberNow does not react to $it.", it in now) }
    }

    @Test
    fun `no clock keeps a minute loop of its own`() {
        val own = Quelltext.files()
            .filter { it.name != "ClockNow.kt" }
            .filter { "ClockTick.millisUntilNextMinute" in it.readText() }
            .map { it.name }
        assertEquals("a clock waits for the minute by itself and sleeps through the wake up.", emptyList<String>(), own)
        val users = Quelltext.files().filter { "rememberNow()" in it.readText() && it.name != "ClockNow.kt" }
        Quelltext.atLeast(users, 2, "clocks reading rememberNow")
    }
}
