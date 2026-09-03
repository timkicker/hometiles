package org.biglau.toggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SosNumbersTest {

    @Test
    fun `eine Zeile mit Kommas wird zerlegt`() {
        assertEquals(
            listOf("+436601234567", "+4351299988"),
            SosNumbers.parse("+43 660 123 45 67, +43 512 999 88"),
        )
    }

    @Test
    fun `auch Semikolon und Zeilenumbruch trennen`() {
        // Der Nutzer soll nicht raten muessen, welches Zeichen gemeint ist.
        assertEquals(listOf("111", "222"), SosNumbers.parse("111; 222"))
        assertEquals(listOf("111", "222"), SosNumbers.parse("111\n222"))
    }

    @Test
    fun `unbrauchbare Eintraege fliegen raus`() {
        // Was hier durchrutscht, ist im Ernstfall eine Nachricht, die nie ankommt.
        assertEquals(listOf("111"), SosNumbers.parse("111, Oma, ---"))
    }

    @Test
    fun `die aussortierten Eintraege lassen sich benennen`() {
        assertEquals(listOf("Oma", "---"), SosNumbers.rejected("111, Oma, ---"))
        assertTrue(SosNumbers.rejected("111, 222").isEmpty())
    }

    @Test
    fun `dieselbe Nummer steht nur einmal drin`() {
        assertEquals(listOf("+43660"), SosNumbers.parse("+43 660, +43660"))
    }

    @Test
    fun `die Zahl der Empfaenger ist begrenzt`() {
        val many = (1..20).joinToString(",") { "+4366012345$it" }
        assertEquals(SosNumbers.MAX, SosNumbers.parse(many).size)
    }

    @Test
    fun `leere Eingabe ergibt keine Empfaenger`() {
        assertTrue(SosNumbers.parse("").isEmpty())
        assertTrue(SosNumbers.parse("  ,  ; ").isEmpty())
    }

    @Test
    fun `Einlesen und Ausgeben passen zusammen`() {
        val numbers = SosNumbers.parse("+43660, +43512")
        assertEquals(numbers, SosNumbers.parse(SosNumbers.format(numbers)))
    }

    @Test
    fun `Waehlzeichen bleiben erhalten`() {
        assertEquals(listOf("*100#"), SosNumbers.parse("*100#"))
    }
}
