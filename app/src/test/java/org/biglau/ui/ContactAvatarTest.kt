package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactAvatarTest {

    @Test
    fun `two name parts give two initials`() {
        assertEquals("AB", initialsOf("Anna Berger"))
    }

    @Test
    fun `with more than two parts the first and the last count`() {
        assertEquals("AZ", initialsOf("Anna Maria Zimmermann"))
    }

    @Test
    fun `a single name gives one initial`() {
        assertEquals("A", initialsOf("Anna"))
    }

    @Test
    fun `an empty name gives a question mark instead of an empty box`() {
        assertEquals("?", initialsOf(""))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `superfluous spaces do not disturb`() {
        assertEquals("AB", initialsOf("  Anna   Berger  "))
    }

    @Test
    fun `initials are always uppercase`() {
        assertEquals("AB", initialsOf("anna berger"))
    }

    @Test
    fun `punctuation does not count as an initial`() {
        // shapes from a real address book; this one gave "?(" before.
        assertEquals("VS", initialsOf("? (Vienna) (Sun)"))
        assertEquals("A", initialsOf("(Anna)"))
        assertEquals("MK", initialsOf("Miller - Klein"))
    }

    @Test
    fun `a name without any letter gives a question mark`() {
        assertEquals("?", initialsOf("??? ---"))
    }

    @Test
    fun `digits may be an initial`() {
        assertEquals("1F", initialsOf("1. Firestation"))
    }

    @Test
    fun `the colour hangs on the name only`() {
        // otherwise it jumps on every redraw and the contact is not recognisable again.
        assertEquals(colorIndexFor("Anna Berger"), colorIndexFor("Anna Berger"))
        assertEquals(colorIndexFor("Anna Berger"), colorIndexFor("  anna berger "))
    }

    @Test
    fun `different names mostly get different colours`() {
        val names = listOf("Anna", "Bertha", "Carl", "Dora", "Emil", "Frieda")
        val buckets = names.map { colorIndexFor(it).mod(6) }.toSet()
        assertTrue("only ${buckets.size} different colours for 6 names", buckets.size >= 4)
    }
}
