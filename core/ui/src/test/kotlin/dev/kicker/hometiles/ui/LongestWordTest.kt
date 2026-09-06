package dev.kicker.hometiles.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * at which word a heading breaks.
 *
 * at 200 percent a heading stood as "Alles zurücksetze / n" - compose splits a word right
 * through as soon as it no longer fits the line on its own. so whether a heading fits is
 * decided not by its length but by its longest word. `BigHeading` measures exactly that and
 * goes one step smaller before it comes to that.
 *
 * the test data stays german: german compounds are the long words this exists for.
 */
class LongestWordTest {

    @Test
    fun `the longest word decides`() {
        assertEquals("zurücksetzen", longestWord("Alles zurücksetzen"))
        assertEquals("Benachrichtigungen", longestWord("Zugriff auf die Benachrichtigungen"))
    }

    @Test
    fun `a single word is its own longest`() {
        assertEquals("Einstellungen", longestWord("Einstellungen"))
    }

    @Test
    fun `line breaks count as a separation`() {
        assertEquals("Startbildschirm", longestWord("Ihr\nStartbildschirm"))
    }

    @Test
    fun `empty text stays empty`() {
        // do not crash and do not guess: an empty text has no longest word.
        assertEquals("", longestWord(""))
    }
}
