package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "no match" only where there was something to match.
 *
 * an empty list and a search without a hit are two different sentences. the contact list said
 * the second one in both cases, and on a phone without contacts it then refers to nothing.
 */
class EmptyListTest {

    private val contacts = Quelltext.file("dev/kicker/hometiles/contacts/ContactsActivity.kt").readText()

    @Test
    fun `the contact list tells empty from no match`() {
        assertTrue(
            "contacts_none missing - then an empty contact list keeps saying no match, even " +
                "though nobody searched.",
            "R.string.contacts_none" in contacts,
        )
        val before = Quelltext.cut(contacts, "", "R.string.contacts_no_match").takeLast(300)
        val after = Quelltext.cut(contacts, "R.string.contacts_no_match").take(100)
        assertTrue(
            "the choice between the two sentences hangs on nothing - `hasContacts` missing.",
            "hasContacts" in before + after,
        )
    }

    @Test
    fun `the app list still gets it right`() {
        val apps = Quelltext.file("dev/kicker/hometiles/apps/AppDrawerActivity.kt").readText()
        assertTrue(
            "the guard `all.isNotEmpty() && shown.isEmpty()` is gone - then the app list says " +
                "no match before any app is loaded.",
            "all.isNotEmpty() && shown.isEmpty()" in apps,
        )
    }

    @Test
    fun `empty and no match are not the same sentence`() {
        listOf("values", "values-de").forEach { language ->
            fun value(name: String) = Quelltext.textValue(name, language)
            assertEquals(
                "$language: contacts_none and contacts_no_match say the same - then the " +
                    "distinction was for nothing.",
                false,
                value("contacts_none") == value("contacts_no_match"),
            )
        }
    }
}
