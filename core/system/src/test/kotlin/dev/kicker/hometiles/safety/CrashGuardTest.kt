package dev.kicker.hometiles.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {

    /** the count this start reaches after [crashes] crashed starts in a row. */
    private fun after(crashes: Int): Int {
        var count = CrashGuard.onStart(0, previousCrashed = false)
        repeat(crashes) { count = CrashGuard.onStart(count, previousCrashed = true) }
        return count
    }

    @Test
    fun `a single crash changes nothing`() {
        // whoever lands in safe mode after every slip stops trusting the app.
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after(0)))
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after(1)))
    }

    @Test
    fun `two crashes in a row switch over`() {
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(after(2)))
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(after(7)))
    }

    @Test
    fun `the counter rises on start`() {
        assertEquals(1, CrashGuard.onStart(0, previousCrashed = false))
        assertEquals(2, CrashGuard.onStart(1, previousCrashed = true))
    }

    @Test
    fun `a start that was ended from outside is forgiven`() {
        // an update, the low memory killer or switching off ended the start before the first
        // frame. without a crash that is no failure, or the next start shows the rescue screen.
        val after = CrashGuard.onStart(1, previousCrashed = false)
        assertEquals(1, after)
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after))
    }

    @Test
    fun `a crashed start still counts`() {
        assertEquals(2, CrashGuard.onStart(1, previousCrashed = true))
    }

    @Test
    fun `starts ended from outside never add up`() {
        var count = after(1)
        repeat(5) { count = CrashGuard.onStart(count, previousCrashed = false) }
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(count))
    }

    @Test
    fun `the counter does not run into infinity`() {
        var count = 0
        repeat(100) { count = CrashGuard.onStart(count, previousCrashed = true) }
        assertTrue("counter was $count", count <= CrashGuard.THRESHOLD * 5)
    }

    @Test
    fun `a successful draw resets`() {
        assertEquals(0, CrashGuard.onRendered())
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(CrashGuard.onRendered()))
    }

    @Test
    fun `a start after the reset is normal again`() {
        val after = CrashGuard.onStart(CrashGuard.onRendered(), previousCrashed = false)
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after))
    }

    @Test
    fun `a successful start leads out of safe mode again`() {
        var count = 0
        repeat(3) { count = CrashGuard.onStart(count, previousCrashed = true) }
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(count))
        count = CrashGuard.onRendered()
        assertEquals(
            StartMode.NORMAL,
            CrashGuard.modeFor(CrashGuard.onStart(count, previousCrashed = false)),
        )
    }
}
