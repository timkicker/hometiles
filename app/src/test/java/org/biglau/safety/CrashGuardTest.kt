package org.biglau.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {

    @Test
    fun `ein einzelner Fehlstart aendert nichts`() {
        // Ein Ausrutscher darf nicht in den Notmodus fuehren - wer dort nach jedem
        // Ausrutscher landet, traut der App nicht mehr.
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(0))
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(1))
    }

    @Test
    fun `zwei Fehlstarts hintereinander schalten um`() {
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(2))
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(7))
    }

    @Test
    fun `der Zaehler steigt beim Start`() {
        assertEquals(1, CrashGuard.onStart(0))
        assertEquals(2, CrashGuard.onStart(1))
    }

    @Test
    fun `der Zaehler laeuft nicht ins Unendliche`() {
        var count = 0
        repeat(100) { count = CrashGuard.onStart(count) }
        assertTrue("Zaehler war $count", count <= CrashGuard.THRESHOLD * 5)
    }

    @Test
    fun `erfolgreiches Zeichnen setzt zurueck`() {
        assertEquals(0, CrashGuard.onRendered())
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(CrashGuard.onRendered()))
    }

    @Test
    fun `ein Start nach dem Zuruecksetzen ist wieder normal`() {
        val after = CrashGuard.onStart(CrashGuard.onRendered())
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(after))
    }

    @Test
    fun `erklaert wird erst ab der Schwelle`() {
        assertTrue(!CrashGuard.shouldExplain(0))
        assertTrue(!CrashGuard.shouldExplain(1))
        assertTrue(CrashGuard.shouldExplain(2))
    }

    @Test
    fun `aus dem Notmodus fuehrt ein erfolgreicher Start wieder heraus`() {
        var count = 0
        repeat(3) { count = CrashGuard.onStart(count) }
        assertEquals(StartMode.SAFE, CrashGuard.modeFor(count))
        count = CrashGuard.onRendered()
        assertEquals(StartMode.NORMAL, CrashGuard.modeFor(CrashGuard.onStart(count)))
    }
}
