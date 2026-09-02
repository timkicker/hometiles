package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auf der PIN-Eingabe zählen die Tasten mehr als die Worte.
 *
 * Bei 200 % Textgröße wuchsen Überschrift und Bestätigungsknopf so weit, dass für die
 * Tastatur nur ein Streifen blieb: die Zifferntasten waren am Emulator noch **21 dp** hoch.
 * Auf einem Bildschirm, auf dem man genau treffen muss — und für jemanden, der 200 % nicht
 * zum Spaß eingestellt hat. Mit dem Deckel sind es 58 dp.
 */
class PinGateScaleTest {

    @Test
    fun `grosse Einstellungen werden gedeckelt`() {
        assertEquals(PIN_MAX_TEXT_SCALE, pinTextScale(2.0f), 0.001f)
        assertEquals(PIN_MAX_TEXT_SCALE, pinTextScale(1.5f), 0.001f)
    }

    @Test
    fun `bis zum Deckel gilt die Einstellung`() {
        assertEquals(1.0f, pinTextScale(1.0f), 0.001f)
        assertEquals(1.25f, pinTextScale(1.25f), 0.001f)
    }

    @Test
    fun `kleiner als eingestellt wird nie`() {
        // Wer 75 % gewaehlt hat, bekommt 75 % - der Deckel ist eine Obergrenze, keine Vorgabe.
        assertEquals(0.75f, pinTextScale(0.75f), 0.001f)
    }
}
