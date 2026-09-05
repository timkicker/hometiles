package org.biglau.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {

    @Test
    fun `a single failed start changes nothing`() {
        // whoever lands in safe mode after every slip stops trusting the app.
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(0))
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(1))
    }

    @Test
    fun `two failed starts in a row switch over`() {
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(2))
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(7))
    }

    @Test
    fun `the counter rises on start`() {
        assertEquals(1, CrashGuard.onStart(0))
        assertEquals(2, CrashGuard.onStart(1))
    }

    @Test
    fun `the counter does not run into infinity`() {
        var count = 0
        repeat(100) { count = CrashGuard.onStart(count) }
        assertTrue("counter was $count", count <= CrashGuard.THRESHOLD * 5)
    }

    @Test
    fun `a successful draw resets`() {
        assertEquals(0, CrashGuard.onRendered())
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(CrashGuard.onRendered()))
    }

    @Test
    fun `a start after the reset is normal again`() {
        val after = CrashGuard.onStart(CrashGuard.onRendered())
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after))
    }

    @Test
    fun `a successful start leads out of safe mode again`() {
        var count = 0
        repeat(3) { count = CrashGuard.onStart(count) }
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(count))
        count = CrashGuard.onRendered()
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(CrashGuard.onStart(count)))
    }
}
