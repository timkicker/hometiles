package org.biglau.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSortTest {

    private fun contact(name: String, starred: Boolean = false, number: String = "+431") =
        PhoneContact(name.hashCode().toLong(), name, null, listOf(PhoneNumber(number, null)), starred)

    @Test
    fun `nach Vornamen bleibt der Name wie er ist`() {
        assertEquals("anna berger", ContactSort.sortKey("Anna Berger", ContactOrder.FIRST_NAME))
    }

    @Test
    fun `nach Nachnamen wandert der letzte Teil nach vorn`() {
        assertEquals("berger anna", ContactSort.sortKey("Anna Berger", ContactOrder.SURNAME))
    }

    @Test
    fun `bei drei Teilen bleibt die Mitte beim Vornamen`() {
        assertEquals("zimmermann anna maria", ContactSort.sortKey("Anna Maria Zimmermann", ContactOrder.SURNAME))
    }

    @Test
    fun `Namenszusaetze bleiben beim Nachnamen`() {
        // "Anna van Dijk" gehoert unter D, nicht unter V - und nicht unter A.
        assertEquals("van dijk anna", ContactSort.sortKey("Anna van Dijk", ContactOrder.SURNAME))
        assertEquals("von trapp maria", ContactSort.sortKey("Maria von Trapp", ContactOrder.SURNAME))
        assertEquals("de la cruz juan", ContactSort.sortKey("Juan de la Cruz", ContactOrder.SURNAME))
    }

    @Test
    fun `ein einzelner Name bleibt unveraendert`() {
        assertEquals("oma", ContactSort.sortKey("Oma", ContactOrder.SURNAME))
        assertEquals("oma", ContactSort.sortKey("Oma", ContactOrder.FIRST_NAME))
    }

    @Test
    fun `Umlaute werden fuer die Sortierung normalisiert`() {
        // Sonst landet "Müller" hinter "Mzyk", weil das Zeichen einen hoeheren Codepunkt hat.
        assertTrue(
            ContactSort.sortKey("Anna Müller", ContactOrder.SURNAME) <
                ContactSort.sortKey("Anna Mzyk", ContactOrder.SURNAME),
        )
    }

    @Test
    fun `ueberfluessige Leerzeichen stoeren nicht`() {
        assertEquals("berger anna", ContactSort.sortKey("  Anna   Berger  ", ContactOrder.SURNAME))
    }

    @Test
    fun `ein leerer Name ergibt einen leeren Schluessel`() {
        assertEquals("", ContactSort.sortKey("   ", ContactOrder.SURNAME))
    }

    @Test
    fun `die Liste wird nach dem Schluessel sortiert`() {
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
    fun `Favoriten stehen oben und sind untereinander sortiert`() {
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
    fun `Favoriten lassen sich auch gleich behandeln`() {
        val list = listOf(contact("Anna Adler"), contact("Zora Zimmer", starred = true))
        assertEquals(
            listOf("Anna Adler", "Zora Zimmer"),
            ContactSort.sorted(list, ContactOrder.FIRST_NAME, favouritesFirst = false).map { it.name },
        )
    }

    @Test
    fun `die Suche kann Nummern einschliessen`() {
        val anna = contact("Anna Berger", number = "+43 660 1234")
        assertTrue(!ContactSort.searchText(anna, includeNumbers = false).contains("660"))
        assertTrue(ContactSort.searchText(anna, includeNumbers = true).contains("660"))
    }
}
