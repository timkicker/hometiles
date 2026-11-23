package dev.kicker.hometiles.phone

import dev.kicker.hometiles.contacts.PhoneContact
import dev.kicker.hometiles.contacts.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Test

/** issue #23: contacts beside the keypad, only where both fit. */
class KeypadContactsTest {

    @Test
    fun `off means the keypad alone`() {
        assertEquals(KeypadSide.NONE, KeypadContacts.side(on = false, widthDp = 1333, heightDp = 728))
    }

    @Test
    fun `a lying tablet gets the list beside, an upright one above`() {
        assertEquals(KeypadSide.BESIDE, KeypadContacts.side(on = true, widthDp = 1333, heightDp = 728))
        assertEquals(KeypadSide.ABOVE, KeypadContacts.side(on = true, widthDp = 800, heightDp = 1261))
    }

    @Test
    fun `a phone keeps its keys whatever the switch says`() {
        // the jelly 2 both ways, and a large phone upright.
        assertEquals(KeypadSide.NONE, KeypadContacts.side(on = true, widthDp = 349, heightDp = 597))
        assertEquals(KeypadSide.NONE, KeypadContacts.side(on = true, widthDp = 597, heightDp = 349))
        assertEquals(KeypadSide.NONE, KeypadContacts.side(on = true, widthDp = 411, heightDp = 891))
    }

    private val anna = PhoneContact(
        id = 1,
        name = "Anna",
        photoUri = null,
        numbers = listOf(PhoneNumber("+447700900123", "Mobile"), PhoneNumber("+447700900124", "Home")),
    )
    private val ben = PhoneContact(2, "Ben", null, listOf(PhoneNumber("+15550100", null)))

    @Test
    fun `nothing typed shows every number`() {
        assertEquals(3, KeypadContacts.entries(listOf(anna, ben), "").size)
        // two digits match nearly anything and only make the list jump.
        assertEquals(3, KeypadContacts.entries(listOf(anna, ben), "55").size)
    }

    @Test
    fun `typed digits narrow it down`() {
        assertEquals(listOf("+447700900124"), KeypadContacts.entries(listOf(anna, ben), "900124").map { it.number })
        assertEquals(listOf("Ben"), KeypadContacts.entries(listOf(anna, ben), "555").map { it.name })
    }

    @Test
    fun `a leading zero finds the number saved with a country code`() {
        assertEquals(
            listOf("+447700900123"),
            KeypadContacts.entries(listOf(anna, ben), "07700900123").map { it.number },
        )
    }
}
