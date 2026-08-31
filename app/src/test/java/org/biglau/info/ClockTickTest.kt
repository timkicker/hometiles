package org.biglau.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTickTest {

    @Test
    fun `mitten in der Minute bleibt der Rest`() {
        // 20 Sekunden nach der vollen Minute sind noch 40 Sekunden offen.
        assertEquals(40_000L, ClockTick.millisUntilNextMinute(20_000L))
    }

    @Test
    fun `auf der vollen Minute wird eine ganze gewartet`() {
        assertEquals(60_000L, ClockTick.millisUntilNextMinute(0L))
        assertEquals(60_000L, ClockTick.millisUntilNextMinute(120_000L))
    }

    @Test
    fun `kurz vor der Minute wird nur der Rest gewartet`() {
        assertEquals(1L, ClockTick.millisUntilNextMinute(59_999L))
    }

    @Test
    fun `die Wartezeit ist nie null oder negativ`() {
        // Sonst drehte die Schleife frei und braete die Batterie.
        (0L..120_000L step 997L).forEach { now ->
            val wait = ClockTick.millisUntilNextMinute(now)
            assertTrue("bei $now war es $wait", wait in 1L..60_000L)
        }
    }

    @Test
    fun `die Anzeige laeuft nicht aus dem Takt`() {
        // Schlicht 60 Sekunden zu warten verschoebe den Wechsel mit jeder Runde weiter
        // in die Minute hinein. Gerechnet wird deshalb bis zur naechsten vollen Minute.
        var now = 20_000L
        repeat(5) {
            now += ClockTick.millisUntilNextMinute(now)
            assertEquals("Wechsel liegt nicht auf der vollen Minute", 0L, now % 60_000L)
        }
    }
}
