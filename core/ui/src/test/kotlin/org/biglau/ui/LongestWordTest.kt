package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * An welchem Wort eine Überschrift bricht.
 *
 * Bei 200 % stand über der Rücksetzen-Seite **„Alles zurücksetze / n"** — Compose trennt ein
 * Wort mitten hindurch, sobald es allein nicht mehr in die Zeile passt. Ob eine Überschrift
 * passt, entscheidet also nicht ihre Länge, sondern ihr längstes Wort. `BigHeading` misst
 * genau das und geht eine Stufe kleiner, bevor es dazu kommt.
 */
class LongestWordTest {

    @Test
    fun `das laengste Wort entscheidet`() {
        assertEquals("zurücksetzen", longestWord("Alles zurücksetzen"))
        assertEquals("Benachrichtigungen", longestWord("Zugriff auf die Benachrichtigungen"))
    }

    @Test
    fun `ein einzelnes Wort ist sein eigenes laengstes`() {
        assertEquals("Einstellungen", longestWord("Einstellungen"))
    }

    @Test
    fun `Zeilenumbrueche zaehlen als Trennung`() {
        assertEquals("Startbildschirm", longestWord("Ihr\nStartbildschirm"))
    }

    @Test
    fun `leerer Text bleibt leer`() {
        // Nicht abstuerzen und nicht raten: ein leerer Text hat kein laengstes Wort.
        assertEquals("", longestWord(""))
    }
}
