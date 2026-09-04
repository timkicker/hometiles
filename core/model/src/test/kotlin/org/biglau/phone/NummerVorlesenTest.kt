package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Eine Rufnummer ist keine Zahl.
 *
 * Als gewöhnlicher Text gelesen macht ein Vorleseprogramm aus „123" ein
 * „einhundertdreiundzwanzig" — und wer die Nummer nachprüfen will, bevor er wählt, kann es
 * nicht. Am 04.09.2026 am Emulator gesehen: die Wähltastatur zeigt die getippte Nummer als
 * schlichten `Text` ohne eigene Beschreibung.
 *
 * Ziffernweise mit Leerzeichen ist die Fassung, die jedes Vorleseprogramm einzeln liest.
 */
class NummerVorlesenTest {

    @Test
    fun `jede ziffer steht fuer sich`() {
        assertEquals("1 2 3", PhoneNumbers.forSpeech("123"))
        assertEquals("0 6 6 4 1 1 1 0 0 1", PhoneNumbers.forSpeech("0664111001"))
    }

    @Test
    fun `die gruppierung fuers auge faellt weg`() {
        // Die Luecken sind eine Lesehilfe fuer Sehende; das Ohr bekommt ohnehin nach jeder
        // Ziffer eine Pause. Bliebe die Gruppierung stehen, laese ein Vorleseprogramm die
        // Bloecke wieder als Zahlen.
        assertEquals(
            PhoneNumbers.forSpeech("0664111001"),
            PhoneNumbers.forSpeech(PhoneNumbers.forDisplay("0664111001")),
        )
    }

    @Test
    fun `das plus bleibt stehen`() {
        assertEquals("+ 4 3 6 6 4", PhoneNumbers.forSpeech("+43664"))
    }

    @Test
    fun `eine buchstabenkennung bleibt ein wort`() {
        // "ADAC" ist keine Nummer, sondern ein Absender. Buchstabe fuer Buchstabe gelesen
        // waere es unverstaendlich - und `clean` laesst davon ohnehin nichts uebrig, also
        // steht der Text selbst da. Dieselbe Regel wie bei forDisplay.
        assertEquals("ADAC", PhoneNumbers.forSpeech("ADAC"))
    }

    @Test
    fun `nichts bleibt nichts`() {
        assertEquals("", PhoneNumbers.forSpeech(""))
    }
}
