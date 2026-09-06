package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * the tile "new message to a fixed number" from PLAN.md 4.3.
 *
 * it stood back a long time because every check would have produced a message. it does not:
 * the tile opens the writing screen with the recipient filled in, and nothing goes out until
 * someone taps send.
 */
class MessageTileTest {

    @Test
    fun `a number becomes a message tile`() {
        val action = MessageTile.actionFor("+43 664 111 001")
        assertEquals("+43664111001", action?.number)
        assertEquals(ContactMode.SMS, action?.mode)
    }

    /** on the tile the number stands in blocks - the same picture as in the call log. */
    @Test
    fun `the number stands readably on the tile`() {
        assertEquals("+436 641 110 01", MessageTile.actionFor("+43664111001")?.name)
    }

    @Test
    fun `spellings fall away, the digits stay`() {
        assertEquals("0664111001", MessageTile.actionFor("0664/111-001")?.number)
    }

    /** a tile that opens an empty writing screen never does anything. */
    @Test
    fun `without a digit no tile comes about`() {
        assertNull(MessageTile.actionFor(""))
        assertNull(MessageTile.actionFor("   "))
        assertNull(MessageTile.actionFor("Alex"))
        assertNull(MessageTile.actionFor("+"))
    }
}
