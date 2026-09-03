package org.biglau.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryInfoTest {

    @Test
    fun `der uebliche Fall mit Skala hundert`() {
        assertEquals(63, BatteryInfo.percent(63, 100))
        assertEquals(0, BatteryInfo.percent(0, 100))
        assertEquals(100, BatteryInfo.percent(100, 100))
    }

    @Test
    fun `eine andere Skala wird umgerechnet`() {
        // Manche Geraete melden 255 statt 100. Wer das uebersieht, zeigt bei halbvollem
        // Akku "50" von 255, also 50 Prozent - und liegt um mehr als das Doppelte daneben.
        assertEquals(50, BatteryInfo.percent(128, 255))
        assertEquals(100, BatteryInfo.percent(255, 255))
    }

    @Test
    fun `unbrauchbare Werte ergeben keinen Stand`() {
        assertNull(BatteryInfo.percent(-1, 100))
        assertNull(BatteryInfo.percent(50, 0))
        assertNull(BatteryInfo.percent(50, -100))
    }

    @Test
    fun `der Stand bleibt zwischen null und hundert`() {
        assertEquals(100, BatteryInfo.percent(300, 100))
    }

    @Test
    fun `Laden wird am Status erkannt`() {
        assertTrue(BatteryInfo.isCharging(BatteryInfo.STATUS_CHARGING, 0))
        assertTrue(BatteryInfo.isCharging(BatteryInfo.STATUS_FULL, 0))
    }

    @Test
    fun `Laden wird auch am Stecker erkannt`() {
        // Manche Geraete melden den Status verzoegert, den Stecker aber sofort.
        assertTrue(BatteryInfo.isCharging(1, plugged = 1))
        assertTrue(!BatteryInfo.isCharging(1, plugged = 0))
    }

    @Test
    fun `unter sechzehn Prozent wird gewarnt`() {
        assertTrue(BatteryInfo.isLow(15))
        assertTrue(BatteryInfo.isLow(3))
        assertTrue(!BatteryInfo.isLow(16))
        assertTrue(!BatteryInfo.isLow(null))
    }

    @Test
    fun `ein unbekannter Stand ergibt keinen Balken`() {
        // Ein leerer Balken saehe aus wie "leer" - das waere eine falsche Aussage.
        assertNull(BatteryInfo.fraction(null))
        assertEquals(0.63f, BatteryInfo.fraction(63)!!, 0.001f)
    }
}
