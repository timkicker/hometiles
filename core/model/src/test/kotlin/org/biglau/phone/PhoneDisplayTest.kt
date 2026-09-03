package org.biglau.phone

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wie eine Rufnummer am Bildschirm steht.
 *
 * Der Anlass steht am Geraet: auf dem Telefon des Nutzers stand eine oesterreichische
 * Mobilnummer als „+436 641 234 567" da. „+436" ist kein Land - Oesterreich ist „+43", und
 * wer die Nummer so abliest oder vorliest, liest sie falsch. Die Gruppierung in Dreierbloecke
 * kannte keine Vorwahlen, und das war auch so aufgeschrieben („waere ohne Bibliothek
 * geraten"). Die Bibliothek liegt aber im Geraet.
 *
 * Geprueft wird hier **die Weiche**, nicht die Bibliothek: dass die Auskunft des Systems
 * vorgeht, dass eine leere Antwort zurueckfaellt, und dass der Rueckfall der alte bleibt.
 * Im Unit-Test gibt es kein Android; ohne diese Trennung waere die Regel eine, die auf dem
 * Geraet etwas anderes tut als im Test.
 */
class PhoneDisplayTest {

    @After
    fun zurueck() {
        PhoneNumbers.systemFormat = { _, _ -> null }
        PhoneNumbers.region = null
    }

    @Test
    fun `die Auskunft des Systems geht der Gruppierung vor`() {
        PhoneNumbers.systemFormat = { _, _ -> "+43 680 1234567" }
        assertEquals("+43 680 1234567", PhoneNumbers.forDisplay("+436801234567"))
    }

    @Test
    fun `das System bekommt die gesaeuberte Nummer und das Land`() {
        var gesehen: Pair<String, String?>? = null
        PhoneNumbers.region = "at"
        PhoneNumbers.systemFormat = { nummer, land -> gesehen = nummer to land; null }
        PhoneNumbers.forDisplay("+43 (680) 1234567")
        assertEquals("+436801234567" to "at", gesehen)
    }

    @Test
    fun `eine leere Antwort faellt auf die Gruppierung zurueck`() {
        PhoneNumbers.systemFormat = { _, _ -> "   " }
        assertEquals("+436 641 234 567", PhoneNumbers.forDisplay("+436801234567"))
    }

    @Test
    fun `ohne Antwort bleibt es beim alten Rueckfall`() {
        PhoneNumbers.systemFormat = { _, _ -> null }
        assertEquals("069 912 345 6", PhoneNumbers.forDisplay("0699 123456"))
    }

    /** Eine Buchstabenkennung bleibt stehen - sie war nie eine Nummer. */
    @Test
    fun `ein Absender ohne Ziffern bleibt was er ist`() {
        PhoneNumbers.systemFormat = { _, _ -> "unsinn" }
        assertEquals("ADAC", PhoneNumbers.forDisplay(" ADAC "))
    }

    /** Kurze Nummern werden nicht angefasst: 112 bleibt 112, auch wenn das System etwas anbietet. */
    @Test
    fun `kurze Nummern bleiben unveraendert`() {
        PhoneNumbers.systemFormat = { _, _ -> "1 12" }
        assertEquals("112", PhoneNumbers.forDisplay("112"))
    }
}
