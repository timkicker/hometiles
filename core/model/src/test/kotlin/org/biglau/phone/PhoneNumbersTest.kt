package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `bekannte Notrufnummern werden erkannt`() {
        listOf("112", "911", "999", "110", "144", "122", "133").forEach {
            assertTrue("$it haette als Notruf gelten muessen", PhoneNumbers.looksLikeEmergency(it))
        }
    }

    @Test
    fun `Schreibweisen mit Trennzeichen zaehlen auch`() {
        assertTrue(PhoneNumbers.looksLikeEmergency("1-1-2"))
        assertTrue(PhoneNumbers.looksLikeEmergency(" 112 "))
        assertTrue(PhoneNumbers.looksLikeEmergency("+112"))
    }

    @Test
    fun `gewoehnliche Nummern sind kein Notruf`() {
        listOf("+436601234567", "0512999888", "1122", "11", "").forEach {
            assertTrue("$it haette kein Notruf sein duerfen", !PhoneNumbers.looksLikeEmergency(it))
        }
    }

    @Test
    fun `die Plattform hat immer recht`() {
        // Sagt Android "Notruf", glauben wir das - auch wenn die Nummer nicht auf der Liste steht.
        assertTrue(PhoneNumbers.looksLikeEmergency("0800112", platformSaysYes = true))
    }

    @Test
    fun `Waehlzeichen bleiben erhalten`() {
        assertEquals("*100#", PhoneNumbers.clean("*100#"))
        assertEquals("+436601234567", PhoneNumbers.clean("+43 660 123 45 67"))
    }

    @Test
    fun `Buchstaben fliegen raus`() {
        assertEquals("0512", PhoneNumbers.clean("0512 (Innsbruck)"))
    }

    @Test
    fun `waehlbar ist nur was eine Ziffer hat`() {
        assertTrue(PhoneNumbers.isDialable("112"))
        assertTrue(!PhoneNumbers.isDialable(""))
        assertTrue(!PhoneNumbers.isDialable("***"))
        assertTrue(!PhoneNumbers.isDialable("keine Nummer"))
    }

    @Test
    fun `lange Nummern werden zum Lesen in Dreierbloecke gruppiert`() {
        // Bewusst ohne Laendervorwahlen-Logik: die waere ohne Bibliothek geraten und
        // laege bei jeder zweiten auslaendischen Nummer daneben.
        assertEquals("+436 601 234 567", PhoneNumbers.forDisplay("+436601234567"))
        assertEquals("051 299 988 8", PhoneNumbers.forDisplay("0512999888"))
    }

    @Test
    fun `kurze Nummern bleiben am Stueck`() {
        assertEquals("112", PhoneNumbers.forDisplay("112"))
        assertEquals("123456", PhoneNumbers.forDisplay("123456"))
    }

    // --- Absender, die keine Nummer sind (02.09.2026) ---

    /**
     * Banken, Paketdienste und Anmeldecodes kommen als Buchstabenkennung. `clean` laesst
     * davon nichts uebrig - in der Nachrichtenliste stand deshalb eine leere Zeile, und
     * zwar bei genau den Nachrichten, die man am ehesten sucht.
     */
    @Test
    fun `eine Buchstabenkennung bleibt lesbar`() {
        assertEquals("ADAC", PhoneNumbers.forDisplay("ADAC"))
        assertEquals("Bank Austria", PhoneNumbers.forDisplay("Bank Austria"))
    }

    @Test
    fun `leerraum um eine Kennung faellt weg`() {
        assertEquals("ADAC", PhoneNumbers.forDisplay("  ADAC  "))
    }

    @Test
    fun `eine gemischte Kennung zeigt ihre Ziffern`() {
        assertEquals("22580", PhoneNumbers.forDisplay("Info-22580"))
    }

    @Test
    fun `ohne alles bleibt es leer`() {
        assertEquals("", PhoneNumbers.forDisplay(""))
        assertEquals("", PhoneNumbers.forDisplay("   "))
    }

    @Test
    fun `gewoehnliche Nummern bleiben wie sie waren`() {
        assertEquals("+436 641 110 01", PhoneNumbers.forDisplay("+43664111001"))
        assertEquals("112", PhoneNumbers.forDisplay("112"))
    }
}
