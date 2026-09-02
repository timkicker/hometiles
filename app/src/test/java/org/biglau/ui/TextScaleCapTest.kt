package org.biglau.ui

import org.biglau.phone.INCALL_MAX_TEXT_SCALE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bildschirme, die nicht scrollen, deckeln die Textgröße.
 *
 * Zweimal am Emulator gesehen, beide Male bei 200 %: die PIN-Tasten waren noch **21 dp**
 * hoch, und im Gespräch stand auf dem Knopf **„Lautsprec…"**. Auf solchen Seiten frisst
 * jede weitere Vergrößerung die Fläche, auf die man tippen muss, oder schneidet das Wort ab
 * — beides trifft genau den, der 200 % eingestellt hat.
 *
 * Der Deckel gilt nur nach oben. Und er ersetzt kein kurzes Wort: „Nicht mehr stumm" war
 * auch bei 125 % zu lang und heißt jetzt „Stumm aus" — parallel zu „Lautsprecher aus".
 */
class TextScaleCapTest {

    @Test
    fun `nach oben wird gedeckelt`() {
        assertEquals(1.25f, cappedTextScale(2.0f, 1.25f), 0.001f)
        assertEquals(1.5f, cappedTextScale(2.0f, 1.5f), 0.001f)
    }

    @Test
    fun `nach unten nie`() {
        assertEquals(0.75f, cappedTextScale(0.75f, 1.25f), 0.001f)
        assertEquals(1.0f, cappedTextScale(1.0f, 1.25f), 0.001f)
    }

    @Test
    fun `die Deckel der festen Bildschirme bleiben unter der groessten Stufe`() {
        // 2.0 ist die groesste waehlbare Stufe; ein Deckel darueber waere keiner.
        assertTrue(PIN_MAX_TEXT_SCALE < 2.0f)
        assertTrue(INCALL_MAX_TEXT_SCALE < 2.0f)
    }
}
