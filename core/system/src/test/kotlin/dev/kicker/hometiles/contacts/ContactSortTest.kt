package dev.kicker.hometiles.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// the names are german and dutch test data: the checks compare the sort keys literally.
class ContactSortTest {

    private fun contact(name: String, starred: Boolean = false, number: String = "+431") =
        PhoneContact(name.hashCode().toLong(), name, null, listOf(PhoneNumber(number, null)), starred)

    @Test
    fun `by first name the name stays as it is`() {
        assertEquals("anna berger", ContactSort.sortKey("Anna Berger", ContactOrder.FIRST_NAME))
    }

    @Test
    fun `by surname the last part moves to the front`() {
        assertEquals("berger anna", ContactSort.sortKey("Anna Berger", ContactOrder.SURNAME))
    }

    @Test
    fun `with three parts the middle stays with the first name`() {
        assertEquals("zimmermann anna maria", ContactSort.sortKey("Anna Maria Zimmermann", ContactOrder.SURNAME))
    }

    @Test
    fun `name particles do not count for the order`() {
        // the old version of this test claimed in its comment "belongs under D, not under V"
        // and then checked for "van dijk anna" - so for V. it held the fault fast instead of
        // finding it.
        assertEquals("dijk anna van", ContactSort.sortKey("Anna van Dijk", ContactOrder.SURNAME))
        assertEquals("trapp maria von", ContactSort.sortKey("Maria von Trapp", ContactOrder.SURNAME))
        assertEquals("cruz juan de la", ContactSort.sortKey("Juan de la Cruz", ContactOrder.SURNAME))
        assertEquals("ackeren bernd von", ContactSort.sortKey("Bernd von Ackeren", ContactOrder.SURNAME))
    }

    @Test
    fun `the particle alone before the surname does not count either`() {
        // a contact called only "de Vries", without a first name.
        assertEquals("vries de", ContactSort.sortKey("de Vries", ContactOrder.SURNAME))
    }

    @Test
    fun `with and without a particle keep a fixed order`() {
        // were both keys equal, the two rows would swap places on every reload - and the
        // list would look restless.
        val without = ContactSort.sortKey("Anna Dijk", ContactOrder.SURNAME)
        val with = ContactSort.sortKey("Anna van Dijk", ContactOrder.SURNAME)
        assertEquals("dijk anna", without)
        assertEquals("dijk anna van", with)
        assertTrue("without the particle first", without < with)
    }

    @Test
    fun `by first name the particle stays where it stands`() {
        // only the surname order rearranges; the first-name order reads the name as it is.
        assertEquals("anna van dijk", ContactSort.sortKey("Anna van Dijk", ContactOrder.FIRST_NAME))
    }

    @Test
    fun `a run of digits looks like a number`() {
        assertTrue(ContactSort.looksLikeNumber("111003"))
        assertTrue("spaces and plus belong to it", ContactSort.looksLikeNumber("+43 664 111"))
        assertTrue("hyphens too", ContactSort.looksLikeNumber("0664-111"))
    }

    @Test
    fun `a name does not look like a number`() {
        assertFalse(ContactSort.looksLikeNumber("Anna"))
        // a contact called "X3" must not trigger the hint - nor two digits, nobody types a
        // number for that.
        assertFalse(ContactSort.looksLikeNumber("X3"))
        assertFalse(ContactSort.looksLikeNumber("12"))
        assertFalse("letters rule it out", ContactSort.looksLikeNumber("Haus 111"))
    }

    @Test
    fun `an empty search looks like nothing`() {
        assertFalse(ContactSort.looksLikeNumber(""))
    }

    @Test
    fun `a single name stays unchanged`() {
        assertEquals("oma", ContactSort.sortKey("Oma", ContactOrder.SURNAME))
        assertEquals("oma", ContactSort.sortKey("Oma", ContactOrder.FIRST_NAME))
    }

    @Test
    fun `diacritics are normalised for the sorting`() {
        // otherwise "Mueller" written with the umlaut lands behind "Mzyk", because the
        // character has a higher code point.
        assertTrue(
            ContactSort.sortKey("Anna Müller", ContactOrder.SURNAME) <
                ContactSort.sortKey("Anna Mzyk", ContactOrder.SURNAME),
        )
    }

    @Test
    fun `superfluous spaces do not disturb`() {
        assertEquals("berger anna", ContactSort.sortKey("  Anna   Berger  ", ContactOrder.SURNAME))
    }

    @Test
    fun `an empty name gives an empty key`() {
        assertEquals("", ContactSort.sortKey("   ", ContactOrder.SURNAME))
    }

    @Test
    fun `the list is sorted by the key`() {
        val list = listOf(contact("Anna Zimmermann"), contact("Bertha Adler"))
        assertEquals(
            listOf("Bertha Adler", "Anna Zimmermann"),
            ContactSort.sorted(list, ContactOrder.SURNAME).map { it.name },
        )
        assertEquals(
            listOf("Anna Zimmermann", "Bertha Adler"),
            ContactSort.sorted(list, ContactOrder.FIRST_NAME).map { it.name },
        )
    }

    @Test
    fun `favourites stand on top and are sorted among themselves`() {
        val list = listOf(
            contact("Anna Adler"),
            contact("Zora Zimmer", starred = true),
            contact("Berta Berg", starred = true),
        )
        assertEquals(
            listOf("Berta Berg", "Zora Zimmer", "Anna Adler"),
            ContactSort.sorted(list, ContactOrder.FIRST_NAME).map { it.name },
        )
    }

    @Test
    fun `favourites can also be treated alike`() {
        val list = listOf(contact("Anna Adler"), contact("Zora Zimmer", starred = true))
        assertEquals(
            listOf("Anna Adler", "Zora Zimmer"),
            ContactSort.sorted(list, ContactOrder.FIRST_NAME, favouritesFirst = false).map { it.name },
        )
    }

    @Test
    fun `the search can include numbers`() {
        val anna = contact("Anna Berger", number = "+43 660 1234")
        assertTrue(!ContactSort.searchText(anna, includeNumbers = false).contains("660"))
        assertTrue(ContactSort.searchText(anna, includeNumbers = true).contains("660"))
    }
}

/**
 * the favourites list as a tile of its own.
 *
 * `PLAN.md` 4.3 promises it; built so far was only "favourites first" inside the full list.
 * with 338 contacts even a sorted list is a detour to the three people one calls daily.
 */
class FavouritesOnlyTest {

    private fun contact(id: Long, name: String, starred: Boolean) = PhoneContact(
        id = id,
        name = name,
        photoUri = null,
        numbers = listOf(PhoneNumber("+43660$id", null)),
        starred = starred,
    )

    private val all = listOf(
        contact(1, "Zita Zauner", true),
        contact(2, "Anna Auer", false),
        contact(3, "Berta Berger", true),
    )

    @Test
    fun `only the starred ones`() {
        val only = ContactSort.favouritesOnly(all, ContactOrder.FIRST_NAME)
        assertEquals(listOf("Berta Berger", "Zita Zauner"), only.map { it.name })
    }

    @Test
    fun `within the favourites it sorts normally`() {
        // not "favourites first" - here everyone is a favourite, so only the name counts.
        val only = ContactSort.favouritesOnly(all, ContactOrder.SURNAME)
        assertEquals(listOf("Berta Berger", "Zita Zauner"), only.map { it.name })
    }

    @Test
    fun `without favourites the list stays empty`() {
        val none = all.map { it.copy(starred = false) }
        assertTrue(ContactSort.favouritesOnly(none, ContactOrder.FIRST_NAME).isEmpty())
    }
}
