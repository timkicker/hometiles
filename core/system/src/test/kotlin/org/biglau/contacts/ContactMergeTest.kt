package org.biglau.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMergeTest {

    private fun row(
        id: Long,
        name: String,
        number: String,
        label: String? = null,
        photo: String? = null,
        starred: Boolean = false,
    ) = ContactRow(id, name, number, label, photo, starred)

    @Test
    fun `spellings of the same number are recognised as equal`() {
        assertEquals(
            ContactMerge.normalizeNumber("+43 660 123 45 67"),
            ContactMerge.normalizeNumber("+436601234567"),
        )
        assertEquals(
            ContactMerge.normalizeNumber("0660/123-4567"),
            ContactMerge.normalizeNumber("0660 123 4567"),
        )
    }

    @Test
    fun `a leading plus keeps two numbers apart`() {
        // without a country code one cannot say for sure whether 0660... is the same number
        // as +43660... - so nothing is guessed here.
        assertTrue(
            ContactMerge.normalizeNumber("+436601234567") !=
                ContactMerge.normalizeNumber("06601234567"),
        )
    }

    @Test
    fun `the same number from two accounts appears once`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna Berger", "+43 660 1234567"),
                row(1, "Anna Berger", "+436601234567"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(1, merged.first().numbers.size)
    }

    @Test
    fun `the first spelling stays`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+43 660 1234567"),
                row(1, "Anna", "+436601234567"),
            ),
        )
        assertEquals("+43 660 1234567", merged.first().numbers.first().number)
    }

    // the labels stay german: they are what the phone's contacts deliver.
    @Test
    fun `a label replaces one seen before without`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+436601234567", label = null),
                row(1, "Anna", "+436601234567", label = "Mobil"),
            ),
        )
        assertEquals("Mobil", merged.first().numbers.first().label)
    }

    @Test
    fun `different numbers are all kept`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+436601234567", label = "Mobil"),
                row(1, "Anna", "+43512999888", label = "Arbeit"),
            ),
        )
        assertEquals(2, merged.first().numbers.size)
        assertTrue(merged.first().hasChoice)
    }

    @Test
    fun `a contact with one number asks nothing`() {
        val merged = ContactMerge.merge(listOf(row(1, "Anna", "+436601234567")))
        assertTrue(!merged.first().hasChoice)
        assertEquals("+436601234567", merged.first().primaryNumber)
    }

    @Test
    fun `nameless entries and numbers without digits fly out`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "", "+436601234567"),
                row(2, "   ", "+436601234567"),
                row(3, "No number", "---"),
                row(4, "Anna", "+436601234567"),
            ),
        )
        assertEquals(listOf("Anna"), merged.map { it.name })
    }

    @Test
    fun `favourites stand on top`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431"),
                row(2, "Bertha", "+432", starred = true),
                row(3, "Carl", "+433"),
            ),
        )
        assertEquals(listOf("Bertha", "Anna", "Carl"), merged.map { it.name })
    }

    @Test
    fun `a favourite mark on one row holds for the contact`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431", starred = false),
                row(1, "Anna", "+432", starred = true),
            ),
        )
        assertTrue(merged.first().starred)
    }

    @Test
    fun `the first photo there is wins`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431", photo = null),
                row(1, "Anna", "+432", photo = "content://photo"),
            ),
        )
        assertEquals("content://photo", merged.first().photoUri)
    }

    @Test
    fun `without a photo it stays null`() {
        val merged = ContactMerge.merge(listOf(row(1, "Anna", "+431")))
        assertNull(merged.first().photoUri)
    }

    @Test
    fun `names are trimmed`() {
        assertEquals("Anna Berger", ContactMerge.merge(listOf(row(1, "  Anna Berger  ", "+431"))).first().name)
    }

    @Test
    fun `an empty list gives an empty list`() {
        assertTrue(ContactMerge.merge(emptyList()).isEmpty())
    }
}

/**
 * a contact without a phone number.
 *
 * today the query over Phone.CONTENT_URI returns no row for such a person, so the list does
 * not hold them at all. but `primaryNumber` was a `first()` on a list that can be empty - a
 * crash one rebuild away. and a crashed launcher is a black phone: that is how android took
 * the home screen role away once before.
 */
class ContactWithoutNumberTest {

    private val withoutNumber = PhoneContact(
        id = 1L,
        name = "Email only",
        photoUri = null,
        numbers = emptyList(),
    )

    private val withNumber = PhoneContact(
        id = 2L,
        name = "With number",
        photoUri = null,
        numbers = listOf(PhoneNumber("+436601234567", null)),
    )

    @Test
    fun `without a number nothing crashes`() {
        assertNull(withoutNumber.primaryNumber)
        assertFalse(withoutNumber.isCallable)
        assertFalse(withoutNumber.hasChoice)
    }

    @Test
    fun `with one number there is nothing to choose`() {
        assertEquals("+436601234567", withNumber.primaryNumber)
        assertTrue(withNumber.isCallable)
        assertFalse(withNumber.hasChoice)
    }

    @Test
    fun `with two numbers it asks`() {
        val two = withNumber.copy(
            numbers = withNumber.numbers + PhoneNumber("+436809876543", null),
        )
        assertTrue(two.hasChoice)
        assertTrue(two.isCallable)
    }
}
