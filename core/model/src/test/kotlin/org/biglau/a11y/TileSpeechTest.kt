package org.biglau.a11y

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Ansage einer Kachel, `PLAN.md` 3.6.
 *
 * Der Anlass steht in [TileSpeech]: der Benachrichtigungszähler und die Empfangsbalken
 * waren gezeichnet und stumm.
 */
class TileSpeechTest {

    @Test
    fun `Label allein bleibt Label`() {
        assertEquals("Telefon", TileSpeech.describe("Telefon"))
    }

    @Test
    fun `Zustand und Zaehler kommen hinter das Label`() {
        assertEquals(
            "Empfang. 3 von 4 Balken. 2 neue Meldungen",
            TileSpeech.describe("Empfang", "3 von 4 Balken", "2 neue Meldungen"),
        )
    }

    /** Ohne das stünde „Telefon. . 2 neue Meldungen" da - der Screenreader liest die Lücke. */
    @Test
    fun `leere Teile fallen weg`() {
        assertEquals("Telefon. 2 neue Meldungen", TileSpeech.describe("Telefon", "  ", "2 neue Meldungen"))
        assertEquals("Telefon", TileSpeech.describe("Telefon", null, null))
        assertEquals("Telefon", TileSpeech.describe("Telefon", "", ""))
    }

    /** Ein Text, der schon auf einen Punkt endet, bekommt keinen zweiten. */
    @Test
    fun `kein doppelter Punkt`() {
        assertEquals("Batterie. 84 %", TileSpeech.describe("Batterie", "84 %."))
    }
}
